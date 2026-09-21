package com.whoami;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Spec 09 工作经历契约测试：公开排序 / 未登录 401 / JSON 字段校验（radar 维度与分数、日期倒置）/ CRUD 即时反映。
 */
@Testcontainers(disabledWithoutDocker = true)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class ExperienceApiIT {

    private static final String TEST_SECRET = "it-test-jwt-secret-0123456789abcdef-0123";
    private static final String ADMIN_PASSWORD = "Admin@whoami2026";
    private static final ObjectMapper JSON = new ObjectMapper();

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
    }

    @BeforeEach
    void clearTable() {
        jdbc.update("DELETE FROM experience");
    }

    @Test
    @Order(1)
    void publicListOrdersBySortThenStartDateDesc() {
        String token = login();
        // A: sort=1, start 更早；B: sort=1, start 更新；C: sort=2
        long a = create(experienceJson("A社", 1, "2020-01-01", null), token);
        long b = create(experienceJson("B社", 1, "2022-06-01", null), token);
        long c = create(experienceJson("C社", 2, "2023-03-01", "2024-01-01"), token);

        ResponseEntity<String> response = rest.getForEntity("/api/experiences", String.class);
        assertThat(response.getStatusCode().value()).isEqualTo(200);
        JsonNode data = json(response).path("data");
        // sort 升序：1 在 2 前；sort 相同按 startDate 倒序：B(2022) 在 A(2020) 前 → B,A,C
        assertThat(companies(data)).containsExactly("B社", "A社", "C社");
        assertThat(data.get(0).path("id").asLong()).isEqualTo(b);
        assertThat(data.get(2).path("endDate").asText()).isEqualTo("2024-01-01");
    }

    @Test
    @Order(2)
    void adminRequiresToken() {
        String createBody = experienceJson("X社", 0, "2021-01-01", null);

        ResponseEntity<String> get = rest.getForEntity("/admin/api/experiences", String.class);
        ResponseEntity<String> post = rest.postForEntity("/admin/api/experiences", jsonBody(createBody, null), String.class);

        assertThat(get.getStatusCode().value()).isEqualTo(401);
        assertThat(post.getStatusCode().value()).isEqualTo(401);
    }

    @Test
    @Order(3)
    void rejectsRadarOutOfRange() {
        String token = login();
        // 2 维
        assertThat(postStatus(experienceJsonRadar(2, 60), token)).isEqualTo(400);
        // 9 维
        assertThat(postStatus(experienceJsonRadar(9, 60), token)).isEqualTo(400);
        // 分数越界（101 / -1），维度固定 3 以隔离分数校验
        assertThat(postStatus(experienceJsonWithScore(3, 101), token)).isEqualTo(400);
        assertThat(postStatus(experienceJsonWithScore(3, -1), token)).isEqualTo(400);
        // 合法 3~8 维通过
        assertThat(postStatus(experienceJsonRadar(5, 60), token)).isEqualTo(200);
    }

    @Test
    @Order(4)
    void rejectsInvertedDates() {
        String token = login();
        String body = experienceJson("倒置社", 0, "2024-01-01", "2023-01-01");
        ResponseEntity<String> response = rest.exchange(
                "/admin/api/experiences", HttpMethod.POST, jsonBody(body, token), String.class);
        assertThat(response.getStatusCode().value()).isEqualTo(400);
        assertThat(json(response).path("code").asInt()).isEqualTo(400);
    }

    @Test
    @Order(5)
    void rejectsDuplicateRadarDimension() {
        String token = login();
        String body = experienceJsonWithDimension("后端", "后端");
        ResponseEntity<String> response = rest.exchange(
                "/admin/api/experiences", HttpMethod.POST, jsonBody(body, token), String.class);
        assertThat(response.getStatusCode().value()).isEqualTo(400);
    }

    @Test
    @Order(6)
    void crudReflectsImmediately() {
        String token = login();
        long id = create(experienceJson("联调社", 0, "2021-07-01", null), token);

        // 创建后公开列表即时含该条
        assertThat(publicCompanies()).contains("联调社");

        // PUT 更新 company 与 endDate
        ResponseEntity<String> put = rest.exchange(
                "/admin/api/experiences/" + id,
                HttpMethod.PUT,
                jsonBody(experienceJson("联调社改", 0, "2021-07-01", "2023-12-31"), token),
                String.class);
        assertThat(put.getStatusCode().value()).isEqualTo(200);
        JsonNode afterUpdate = findPublicById(id);
        assertThat(afterUpdate.path("company").asText()).isEqualTo("联调社改");
        assertThat(afterUpdate.path("endDate").asText()).isEqualTo("2023-12-31");

        // DELETE 后公开列表删除该条
        ResponseEntity<String> del = rest.exchange(
                "/admin/api/experiences/" + id, HttpMethod.DELETE, withToken(token), String.class);
        assertThat(del.getStatusCode().value()).isEqualTo(200);
        assertThat(findPublicById(id)).isNull();

        // 更新/删除不存在 id → 404
        ResponseEntity<String> put404 = rest.exchange(
                "/admin/api/experiences/999999",
                HttpMethod.PUT,
                jsonBody(experienceJson("不存在", 0, "2021-01-01", null), token),
                String.class);
        assertThat(put404.getStatusCode().value()).isEqualTo(404);
    }

    // ---- helpers ----

    private String login() {
        ResponseEntity<String> login = rest.postForEntity(
                "/admin/api/auth/login",
                jsonBody("{\"username\":\"admin\",\"password\":\"" + ADMIN_PASSWORD + "\"}", null),
                String.class);
        assertThat(login.getStatusCode().value()).isEqualTo(200);
        return json(login).path("data").path("token").asText();
    }

    private long create(String body, String token) {
        ResponseEntity<String> response = rest.exchange(
                "/admin/api/experiences", HttpMethod.POST, jsonBody(body, token), String.class);
        assertThat(response.getStatusCode().value()).isEqualTo(200);
        return json(response).path("data").path("id").asLong();
    }

    private int postStatus(String body, String token) {
        ResponseEntity<String> response = rest.exchange(
                "/admin/api/experiences", HttpMethod.POST, jsonBody(body, token), String.class);
        return response.getStatusCode().value();
    }

    private String experienceJson(String company, int sort, String start, String end) {
        return "{\"company\":\"" + company + "\",\"title\":\"后端工程师\",\"startDate\":\"" + start + "\","
                + (end == null ? "" : "\"endDate\":\"" + end + "\",")
                + "\"sortOrder\":" + sort + ",\"radar\":" + radarOf(3, 60)
                + ",\"techTags\":[\"Java\"],\"highlights\":[\"要点一\"]}";
    }

    private String experienceJsonRadar(int dims, int score) {
        return "{\"company\":\"雷达社\",\"title\":\"工程师\",\"startDate\":\"2021-01-01\","
                + "\"radar\":" + radarOf(dims, score) + "}";
    }

    private String experienceJsonWithScore(int dims, int score) {
        return "{\"company\":\"分数社\",\"title\":\"工程师\",\"startDate\":\"2021-01-01\","
                + "\"radar\":" + radarOf(dims, score) + "}";
    }

    private String experienceJsonWithDimension(String d1, String d2) {
        return "{\"company\":\"重复社\",\"title\":\"工程师\",\"startDate\":\"2021-01-01\","
                + "\"radar\":[{\"dimension\":\"" + d1 + "\",\"score\":60},"
                + "{\"dimension\":\"" + d2 + "\",\"score\":70},"
                + "{\"dimension\":\"性能\",\"score\":80}]}";
    }

    private String radarOf(int dims, int score) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < dims; i++) {
            if (i > 0) {
                sb.append(",");
            }
            sb.append("{\"dimension\":\"维度").append(i).append("\",\"score\":").append(score).append("}");
        }
        return sb.append("]").toString();
    }

    private java.util.List<String> companies(JsonNode data) {
        java.util.List<String> names = new java.util.ArrayList<>();
        data.forEach(n -> names.add(n.path("company").asText()));
        return names;
    }

    private java.util.List<String> publicCompanies() {
        ResponseEntity<String> response = rest.getForEntity("/api/experiences", String.class);
        return companies(json(response).path("data"));
    }

    private JsonNode findPublicById(long id) {
        ResponseEntity<String> response = rest.getForEntity("/api/experiences", String.class);
        for (JsonNode n : json(response).path("data")) {
            if (n.path("id").asLong() == id) {
                return n;
            }
        }
        return null;
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
}