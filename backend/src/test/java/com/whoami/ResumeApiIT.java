package com.whoami;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;
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
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Spec 07 契约集成测试：真实 multipart 上传、流下载、版本保留/回滚、下载埋点直查库、
 * 无版本分支、owner_name 联动文件名、未登录 401。
 */
@Testcontainers(disabledWithoutDocker = true)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class ResumeApiIT {

    private static final String TEST_SECRET = "it-test-jwt-secret-0123456789abcdef-0123";
    private static final String ADMIN_PASSWORD = "Admin@whoami2026";
    private static final long MAX_FILE_BYTES = 20L * 1024 * 1024;
    private static final ObjectMapper JSON = new ObjectMapper();
    private static final DateTimeFormatter MONTH = DateTimeFormatter.ofPattern("yyyy-MM");

    @Container
    static final MySQLContainer<?> MYSQL = new MySQLContainer<>("mysql:8.0")
            .withDatabaseName("whoami")
            .withUsername("whoami")
            .withPassword("whoami-test");

    private static Path uploadDir;

    @DynamicPropertySource
    static void datasourceProps(DynamicPropertyRegistry registry) {
        try {
            uploadDir = Files.createTempDirectory("resume-it");
        } catch (IOException e) {
            throw new IllegalStateException("创建测试上传目录失败", e);
        }
        registry.add("spring.datasource.url", MYSQL::getJdbcUrl);
        registry.add("spring.datasource.username", MYSQL::getUsername);
        registry.add("spring.datasource.password", MYSQL::getPassword);
        registry.add("app.jwt.secret", () -> TEST_SECRET);
        registry.add("app.upload-dir", () -> uploadDir.toString());
    }

    @Autowired
    private TestRestTemplate rest;

    @Autowired
    private JdbcTemplate jdbc;

    @AfterAll
    static void cleanup(@Autowired JdbcTemplate jdbc) throws IOException {
        jdbc.update("DELETE FROM track_event");
        jdbc.update("DELETE FROM resume_file");
        if (uploadDir != null && Files.exists(uploadDir)) {
            try (Stream<Path> paths = Files.walk(uploadDir)) {
                for (Path p : paths.sorted(java.util.Comparator.reverseOrder()).toList()) {
                    Files.deleteIfExists(p);
                }
            }
        }
    }

    /** 仿真实 PDF：魔数 %PDF- 开头 */
    private static byte[] pdf(String marker) {
        return ("%PDF-1.4\nmarker:" + marker + "\n%%EOF").getBytes(StandardCharsets.UTF_8);
    }

    private String login() {
        ResponseEntity<String> login = rest.postForEntity(
                "/admin/api/auth/login",
                jsonBody("{\"username\":\"admin\",\"password\":\"" + ADMIN_PASSWORD + "\"}", null),
                String.class);
        assertThat(login.getStatusCode().value()).isEqualTo(200);
        return json(login).path("data").path("token").asText();
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

    private ResponseEntity<String> upload(String token, byte[] content, String filename) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);
        if (token != null) {
            headers.setBearerAuth(token);
        }
        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("file", new ByteArrayResource(content) {
            @Override
            public String getFilename() {
                return filename;
            }
        });
        return rest.postForEntity("/admin/api/resumes", new HttpEntity<>(body, headers), String.class);
    }

    private ResponseEntity<byte[]> download() {
        return rest.exchange("/api/resume/download", HttpMethod.GET, HttpEntity.EMPTY, byte[].class);
    }

    private int downloadEventCount() {
        return jdbc.queryForObject(
                "SELECT COUNT(*) FROM track_event WHERE event_type = 'resume_download'", Integer.class);
    }

    private long pdfFileCount() throws IOException {
        try (Stream<Path> files = Files.list(uploadDir.resolve("resume"))) {
            return files.filter(p -> p.toString().endsWith(".pdf")).count();
        }
    }

    @Test
    @Order(1)
    void noVersionLatestFalseAndDownload404() {
        ResponseEntity<String> latest = rest.getForEntity("/api/resume/latest", String.class);
        assertThat(latest.getStatusCode().value()).isEqualTo(200);
        JsonNode data = json(latest).path("data");
        assertThat(data.path("exists").asBoolean()).isFalse();
        assertThat(data.path("displayName").isNull()).isTrue();
        assertThat(data.path("updatedAt").isNull()).isTrue();

        ResponseEntity<byte[]> download = download();
        assertThat(download.getStatusCode().value()).isEqualTo(404);
        assertThat(downloadEventCount()).isZero();
    }

    @Test
    @Order(2)
    void uploadPdfServesLatestAndTracksDownload() throws IOException {
        String token = login();
        byte[] v1 = pdf("v1");

        ResponseEntity<String> upload = upload(token, v1, "resume-v1.pdf");
        assertThat(upload.getStatusCode().value()).isEqualTo(200);
        JsonNode up = json(upload);
        assertThat(up.path("code").asInt()).isZero();
        assertThat(up.path("message").asText()).isEqualTo("ok");
        assertThat(up.path("data").path("versionNo").asInt()).isEqualTo(1);
        assertThat(up.path("data").path("id").asLong()).isPositive();

        JsonNode latest = json(rest.getForEntity("/api/resume/latest", String.class)).path("data");
        assertThat(latest.path("exists").asBoolean()).isTrue();
        String displayName = latest.path("displayName").asText();
        assertThat(displayName).startsWith("站主_简历_").endsWith(".pdf");
        assertThat(displayName).contains(LocalDate.now().format(MONTH));

        int before = downloadEventCount();
        ResponseEntity<byte[]> dl = download();
        assertThat(dl.getStatusCode().value()).isEqualTo(200);
        assertThat(dl.getBody()).isEqualTo(v1);
        String disposition = dl.getHeaders().getFirst(HttpHeaders.CONTENT_DISPOSITION);
        assertThat(disposition).contains("attachment").contains("filename*=UTF-8''");
        String encoded = disposition.substring(
                disposition.indexOf("filename*=UTF-8''") + "filename*=UTF-8''".length());
        assertThat(URLDecoder.decode(encoded, StandardCharsets.UTF_8)).isEqualTo(displayName);
        assertThat(downloadEventCount()).isEqualTo(before + 1);
        assertThat(jdbc.queryForObject(
                "SELECT event_type FROM track_event ORDER BY id DESC LIMIT 1", String.class))
                .isEqualTo("resume_download");
        // detail 携带版本信息（MySQL JSON 列会规范化格式，按 JSON 解析断言）
        String detail = jdbc.queryForObject(
                "SELECT detail FROM track_event ORDER BY id DESC LIMIT 1", String.class);
        JsonNode detailJson = JSON.readTree(detail);
        assertThat(detailJson.path("versionNo").asInt()).isEqualTo(1);
        assertThat(detailJson.path("displayName").asText()).isEqualTo(displayName);
        assertThat(pdfFileCount()).isEqualTo(1);
    }

    @Test
    @Order(3)
    void rejectsTxtAndOversizeWith400() {
        String token = login();

        ResponseEntity<String> txt = upload(token, "hello this is not a pdf".getBytes(StandardCharsets.UTF_8), "resume.txt");
        assertThat(txt.getStatusCode().value()).isEqualTo(400);
        assertThat(json(txt).path("message").asText()).contains("仅支持 PDF");

        byte[] big = new byte[(int) (MAX_FILE_BYTES + 1024 * 1024)];
        Arrays.fill(big, (byte) 'x');
        ResponseEntity<String> oversize = upload(token, big, "big.pdf");
        assertThat(oversize.getStatusCode().value()).isEqualTo(400);
        assertThat(json(oversize).path("message").asText()).contains("20MB");
    }

    @Test
    @Order(4)
    void fourUploadsKeepThreeAndEvictOldest() throws IOException {
        String token = login();
        byte[] v4 = pdf("v4");

        upload(token, pdf("v2"), "resume.pdf");
        upload(token, pdf("v3"), "resume.pdf");
        ResponseEntity<String> up4 = upload(token, v4, "resume.pdf");

        // 第 4 次上传提示淘汰版本 1
        assertThat(json(up4).path("message").asText()).contains("淘汰").contains("1");
        assertThat(json(up4).path("data").path("versionNo").asInt()).isEqualTo(4);

        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM resume_file", Integer.class)).isEqualTo(3);
        assertThat(jdbc.queryForObject(
                "SELECT COUNT(*) FROM resume_file WHERE version_no = 1", Integer.class)).isZero();
        assertThat(jdbc.queryForObject(
                "SELECT COUNT(*) FROM resume_file WHERE is_current = 1", Integer.class)).isEqualTo(1);
        assertThat(pdfFileCount()).isEqualTo(3);

        ResponseEntity<String> list = rest.exchange(
                "/admin/api/resumes", HttpMethod.GET, withToken(token), String.class);
        JsonNode versions = json(list).path("data");
        assertThat(versions).hasSize(3);
        assertThat(versions.get(0).path("versionNo").asInt()).isEqualTo(4);
        assertThat(versions.get(0).path("isCurrent").asBoolean()).isTrue();
        assertThat(versions.get(1).path("versionNo").asInt()).isEqualTo(3);
        assertThat(versions.get(2).path("versionNo").asInt()).isEqualTo(2);
        assertThat(versions.get(0).path("sizeBytes").asLong()).isEqualTo(v4.length);
        assertThat(versions.get(0).path("displayName").asText()).isNotEmpty();
        assertThat(versions.get(0).path("uploadedAt").asText()).isNotEmpty();
    }

    @Test
    @Order(5)
    void restoreOldVersionBecomesCurrentAndDownloadServesIt() {
        String token = login();
        Long id2 = jdbc.queryForObject("SELECT id FROM resume_file WHERE version_no = 2", Long.class);

        ResponseEntity<String> restore = rest.exchange(
                "/admin/api/resumes/" + id2 + "/restore", HttpMethod.PUT, withToken(token), String.class);
        assertThat(restore.getStatusCode().value()).isEqualTo(200);
        assertThat(jdbc.queryForObject(
                "SELECT COUNT(*) FROM resume_file WHERE is_current = 1 AND version_no = 2", Integer.class))
                .isEqualTo(1);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM resume_file", Integer.class)).isEqualTo(3);

        // 下载返回回滚版内容
        ResponseEntity<byte[]> dl = download();
        assertThat(dl.getStatusCode().value()).isEqualTo(200);
        assertThat(dl.getBody()).isEqualTo(pdf("v2"));

        // 回滚不存在的版本 → 404
        ResponseEntity<String> missing = rest.exchange(
                "/admin/api/resumes/999999/restore", HttpMethod.PUT, withToken(token), String.class);
        assertThat(missing.getStatusCode().value()).isEqualTo(404);
    }

    @Test
    @Order(6)
    void ownerNameChangeReflectedInNewUpload() throws IOException {
        String token = login();
        ResponseEntity<String> put = rest.exchange(
                "/admin/api/site-config/owner_name", HttpMethod.PUT,
                jsonBody("{\"value\":\"新主\"}", token), String.class);
        assertThat(put.getStatusCode().value()).isEqualTo(200);

        ResponseEntity<String> upload = upload(token, pdf("v5"), "resume.pdf");
        assertThat(upload.getStatusCode().value()).isEqualTo(200);
        assertThat(json(upload).path("data").path("versionNo").asInt()).isEqualTo(5);

        JsonNode latest = json(rest.getForEntity("/api/resume/latest", String.class)).path("data");
        assertThat(latest.path("displayName").asText()).startsWith("新主_简历_");
        assertThat(download().getBody()).isEqualTo(pdf("v5"));
        // 回滚后的旧当前版 v2 不会被淘汰，latest 仍然有效
        assertThat(jdbc.queryForObject(
                "SELECT COUNT(*) FROM resume_file WHERE is_current = 1", Integer.class)).isEqualTo(1);
        assertThat(pdfFileCount()).isEqualTo(3);
    }

    @Test
    @Order(7)
    void adminEndpointsRequireToken() {
        assertThat(rest.getForEntity("/admin/api/resumes", String.class).getStatusCode().value()).isEqualTo(401);
        assertThat(upload(null, pdf("anon"), "resume.pdf").getStatusCode().value()).isEqualTo(401);
        assertThat(rest.exchange("/admin/api/resumes/1/restore", HttpMethod.PUT, HttpEntity.EMPTY, String.class)
                .getStatusCode().value()).isEqualTo(401);
    }
}
