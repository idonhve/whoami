package com.whoami;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Spec 08 契约集成测试：真实图片字节 multipart 上传 → 缩略图/压缩原图产物 + 公开列表 + 排序 +
 * 校验 400 + 删除清理物理文件 + /uploads/** 静态访问 + 未登录 401。
 */
@Testcontainers(disabledWithoutDocker = true)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class CertificateApiIT {

    private static final String TEST_SECRET = "it-test-jwt-secret-0123456789abcdef-0123";
    private static final String ADMIN_PASSWORD = "Admin@whoami2026";
    private static final ObjectMapper JSON = new ObjectMapper();
    private static final Path UPLOAD_DIR = createTempUploadDir();

    @Container
    static final MySQLContainer<?> MYSQL = new MySQLContainer<>("mysql:8.0")
            .withDatabaseName("whoami")
            .withUsername("whoami")
            .withPassword("whoami-test");

    @Autowired
    private TestRestTemplate rest;

    @Autowired
    private JdbcTemplate jdbc;

    @org.springframework.test.context.DynamicPropertySource
    static void datasourceProps(org.springframework.test.context.DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", MYSQL::getJdbcUrl);
        registry.add("spring.datasource.username", MYSQL::getUsername);
        registry.add("spring.datasource.password", MYSQL::getPassword);
        registry.add("app.jwt.secret", () -> TEST_SECRET);
        registry.add("app.upload-dir", () -> UPLOAD_DIR.toString());
    }

    @AfterAll
    static void cleanup(@Autowired JdbcTemplate jdbc) throws IOException {
        jdbc.update("DELETE FROM certificate");
        try (var paths = Files.walk(UPLOAD_DIR)) {
            paths.sorted(java.util.Comparator.reverseOrder()).forEach(path -> {
                try {
                    Files.deleteIfExists(path);
                } catch (IOException ignored) {
                    // 临时目录清理失败不影响测试结论
                }
            });
        }
    }

    private static Path createTempUploadDir() {
        try {
            return Files.createTempDirectory("whoami-cert-it");
        } catch (IOException e) {
            throw new IllegalStateException("临时上传目录创建失败", e);
        }
    }

    @Test
    @Order(1)
    void uploadJpegProducesWebpThumbnailAndCompressedOriginalServedOverUploads() throws IOException {
        String token = login();
        byte[] source = jpegBytes(2400, 1600);

        ResponseEntity<String> created = upload(token, source, "cert.jpg", "软考中级", "2025-06-01");

        assertThat(created.getStatusCode().value()).isEqualTo(200);
        long id = json(created).path("data").path("id").asLong();
        assertThat(id).isPositive();

        // 落库相对路径 + 物理文件存在
        String originalFile = jdbc.queryForObject(
                "SELECT original_file FROM certificate WHERE id = ?", String.class, id);
        String thumbnailFile = jdbc.queryForObject(
                "SELECT thumbnail_file FROM certificate WHERE id = ?", String.class, id);
        Path original = UPLOAD_DIR.resolve(originalFile);
        Path thumbnail = UPLOAD_DIR.resolve(thumbnailFile);
        assertThat(originalFile).startsWith("certificate/").endsWith(".webp");
        assertThat(thumbnailFile).startsWith("certificate/").endsWith(".webp");
        assertThat(original).exists();
        assertThat(thumbnail).exists();

        // 缩略图约 400px 宽；压缩原图长边 ≤ 2000px；缩略图体积显著更小
        assertThat(ImageIO.read(thumbnail.toFile()).getWidth()).isEqualTo(400);
        BufferedImage compressed = ImageIO.read(original.toFile());
        assertThat(Math.max(compressed.getWidth(), compressed.getHeight())).isLessThanOrEqualTo(2000);
        assertThat(Files.size(thumbnail) * 3).isLessThan(Files.size(original));

        // 公开列表给出 /uploads/** 地址，且该地址可只读访问到真实图片字节
        ResponseEntity<String> list = rest.getForEntity("/api/certificates", String.class);
        assertThat(list.getStatusCode().value()).isEqualTo(200);
        JsonNode item = json(list).path("data").get(0);
        assertThat(item.path("name").asText()).isEqualTo("软考中级");
        assertThat(item.path("obtainedAt").asText()).isEqualTo("2025-06-01");
        assertThat(item.path("thumbUrl").asText()).isEqualTo("/uploads/" + thumbnailFile);
        assertThat(item.path("imageUrl").asText()).isEqualTo("/uploads/" + originalFile);

        ResponseEntity<byte[]> served = rest.getForEntity(item.path("thumbUrl").asText(), byte[].class);
        assertThat(served.getStatusCode().value()).isEqualTo(200);
        assertThat(served.getBody()).isEqualTo(Files.readAllBytes(thumbnail));
    }

    @Test
    @Order(2)
    void invalidUploadsRejectedWith400() throws IOException {
        String token = login();

        // 魔数伪装：文本改扩展名冒充 jpg
        assertThat(upload(token, "not an image".getBytes(), "fake.jpg", "伪装", "2025-06-01")
                .getStatusCode().value()).isEqualTo(400);
        // 超过 5MB（multipart 上限 20MB，5MB 由业务校验拦截）
        assertThat(upload(token, new byte[6 * 1024 * 1024], "huge.jpg", "超大", "2025-06-01")
                .getStatusCode().value()).isEqualTo(400);
        // name 必填 / 超长
        assertThat(upload(token, jpegBytes(200, 150), "cert.jpg", null, "2025-06-01")
                .getStatusCode().value()).isEqualTo(400);
        assertThat(upload(token, jpegBytes(200, 150), "cert.jpg", "证".repeat(101), "2025-06-01")
                .getStatusCode().value()).isEqualTo(400);
        // obtainedAt 必填 / 格式非法
        assertThat(upload(token, jpegBytes(200, 150), "cert.jpg", "无时间", null)
                .getStatusCode().value()).isEqualTo(400);
        assertThat(upload(token, jpegBytes(200, 150), "cert.jpg", "时间非法", "2025/06/01")
                .getStatusCode().value()).isEqualTo(400);

        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM certificate", Integer.class)).isEqualTo(1);
    }

    @Test
    @Order(3)
    void listOrdersBySortOrderAscThenObtainedAtDesc() throws IOException {
        String token = login();
        long early = createCertificate(token, "早证书", "2020-01-01");
        long late = createCertificate(token, "晚证书", "2026-01-01");
        long middle = createCertificate(token, "中证书", "2024-01-01");

        // 未设 sortOrder（均为 0）：按 obtainedAt 倒序
        assertThat(names()).containsExactly("晚证书", "软考中级", "中证书", "早证书");

        // 排序权重生效：sortOrder 升序优先
        update(token, early, "{\"sortOrder\":1}");
        update(token, late, "{\"sortOrder\":2}");
        assertThat(names()).containsExactly("软考中级", "中证书", "早证书", "晚证书");

        // 同时改名称与获取时间
        assertThat(update(token, middle, "{\"name\":\"中证书改\",\"obtainedAt\":\"2026-06-06\",\"sortOrder\":1}")
                .getStatusCode().value()).isEqualTo(200);
        assertThat(names()).containsExactly("中证书改", "早证书", "晚证书", "软考中级");
        assertThat(jdbc.queryForObject("SELECT obtained_at FROM certificate WHERE id = ?", String.class, middle))
                .isEqualTo("2026-06-06");
    }

    @Test
    @Order(4)
    void updateUnknownIdReturns404() {
        String token = login();

        assertThat(update(token, 999999L, "{\"name\":\"不存在\"}").getStatusCode().value()).isEqualTo(404);
        assertThat(rest.exchange("/admin/api/certificates/999999", HttpMethod.DELETE, withToken(token), String.class)
                .getStatusCode().value()).isEqualTo(404);
    }

    @Test
    @Order(5)
    void deleteRemovesRecordAndPhysicalFiles() throws IOException {
        String token = login();
        long id = createCertificate(token, "待删除", "2023-03-03");
        String originalFile = jdbc.queryForObject(
                "SELECT original_file FROM certificate WHERE id = ?", String.class, id);
        String thumbnailFile = jdbc.queryForObject(
                "SELECT thumbnail_file FROM certificate WHERE id = ?", String.class, id);
        Path original = UPLOAD_DIR.resolve(originalFile);
        Path thumbnail = UPLOAD_DIR.resolve(thumbnailFile);
        assertThat(original).exists();
        assertThat(thumbnail).exists();

        ResponseEntity<String> deleted = rest.exchange(
                "/admin/api/certificates/" + id, HttpMethod.DELETE, withToken(token), String.class);

        assertThat(deleted.getStatusCode().value()).isEqualTo(200);
        assertThat(jdbc.queryForObject(
                "SELECT COUNT(*) FROM certificate WHERE id = ?", Integer.class, id)).isZero();
        assertThat(original).doesNotExist();
        assertThat(thumbnail).doesNotExist();
    }

    @Test
    @Order(6)
    void adminEndpointsRequireToken() throws IOException {
        assertThat(rest.getForEntity("/api/certificates", String.class).getStatusCode().value()).isEqualTo(200);
        assertThat(rest.exchange("/admin/api/certificates/" + 1, HttpMethod.PUT,
                jsonBody("{\"sortOrder\":1}", null), String.class).getStatusCode().value()).isEqualTo(401);
        assertThat(rest.exchange("/admin/api/certificates/1", HttpMethod.DELETE,
                HttpEntity.EMPTY, String.class).getStatusCode().value()).isEqualTo(401);

        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("file", new ByteArrayResource(jpegBytes(200, 150)) {
            @Override
            public String getFilename() {
                return "cert.jpg";
            }
        });
        body.add("name", "未登录");
        body.add("obtainedAt", "2025-06-01");
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);
        assertThat(rest.exchange("/admin/api/certificates", HttpMethod.POST,
                new HttpEntity<>(body, headers), String.class).getStatusCode().value()).isEqualTo(401);
    }

    private long createCertificate(String token, String name, String obtainedAt) throws IOException {
        ResponseEntity<String> created = upload(token, jpegBytes(800, 600), "cert.jpg", name, obtainedAt);
        assertThat(created.getStatusCode().value()).isEqualTo(200);
        return json(created).path("data").path("id").asLong();
    }

    private List<String> names() {
        ResponseEntity<String> list = rest.getForEntity("/api/certificates", String.class);
        List<String> names = new ArrayList<>();
        json(list).path("data").forEach(node -> names.add(node.path("name").asText()));
        return names;
    }

    private ResponseEntity<String> update(String token, long id, String json) {
        return rest.exchange("/admin/api/certificates/" + id, HttpMethod.PUT, jsonBody(json, token), String.class);
    }

    private String login() {
        ResponseEntity<String> login = rest.postForEntity(
                "/admin/api/auth/login",
                jsonBody("{\"username\":\"admin\",\"password\":\"" + ADMIN_PASSWORD + "\"}", null),
                String.class);
        assertThat(login.getStatusCode().value()).isEqualTo(200);
        return json(login).path("data").path("token").asText();
    }

    private ResponseEntity<String> upload(String token, byte[] bytes, String filename, String name, String obtainedAt) {
        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("file", new ByteArrayResource(bytes) {
            @Override
            public String getFilename() {
                return filename;
            }
        });
        if (name != null) {
            body.add("name", name);
        }
        if (obtainedAt != null) {
            body.add("obtainedAt", obtainedAt);
        }
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);
        headers.setBearerAuth(token);
        return rest.exchange("/admin/api/certificates", HttpMethod.POST, new HttpEntity<>(body, headers), String.class);
    }

    private HttpEntity<String> jsonBody(String json, String token) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        if (token != null) {
            headers.setBearerAuth(token);
        }
        return new HttpEntity<>(json, headers);
    }

    private HttpEntity<Void> withToken(String token) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        return new HttpEntity<>(headers);
    }

    private JsonNode json(ResponseEntity<String> response) {
        try {
            return JSON.readTree(response.getBody());
        } catch (Exception e) {
            throw new IllegalStateException("响应不是合法 JSON: " + response.getBody(), e);
        }
    }

    /** 生成带噪点的真实 JPEG 字节 */
    private static byte[] jpegBytes(int width, int height) throws IOException {
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        int[] pixels = new int[width * height];
        Random random = new Random(20260926L);
        for (int i = 0; i < pixels.length; i++) {
            pixels[i] = random.nextInt(0xFFFFFF) | 0xFF000000;
        }
        image.setRGB(0, 0, width, height, pixels, 0, width);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ImageIO.write(image, "jpeg", out);
        return out.toByteArray();
    }
}