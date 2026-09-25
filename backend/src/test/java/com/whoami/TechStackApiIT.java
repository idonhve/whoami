package com.whoami;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.AfterAll;
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
 * Spec 02 技术栈契约测试：公开列表按 sortOrder 排序、未登录访问管理接口 401、
 * CRUD 后公开接口即时反映变更、枚举/长度校验返回 400。
 */
@Testcontainers(disabledWithoutDocker = true)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class TechStackApiIT {

    private static final String TEST_SECRET = "it-test-jwt-secret-0123456789abcdef-0123";
    private static final String ADMIN_PASSWORD = "Admin@whoami2026";
    private static final ObjectMapper JSON = new ObjectMapper();
    /** 记录本次测试新增的条目 id，收尾统一清理 */
    private static final List<Long> CREATED_IDS = new ArrayList<>();

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

    @AfterAll
    static void cleanup(@Autowired JdbcTemplate jdbc) {
        for (Long id : CREATED_IDS) {
            jdbc.update("DELETE FROM tech_stack WHERE id = ?", id);
        }
    }

    @Test
    @Order(1)
    void publicListIsEmptyAndSorted() {
        jdbc.update("DELETE FROM tech_stack");
        // 插入两条乱序数据，验证公开接口按 sortOrder 升序
        jdbc.update("INSERT INTO tech_stack (name, icon, category, proficiency, weight, sort_order, created_at, updated_at) "
                + "VALUES ('后端-B', 'java', '后端', 'proficient', 5, 2, NOW(), NOW())");
        jdbc.update("INSERT INTO tech_stack (name, icon, category, proficiency, weight, sort_order, created_at, updated_at) "
                + "VALUES ('前端-A', 'vuejs', '前端', 'master', 8, 1, NOW(), NOW())");

        ResponseEntity<String> response = rest.getForEntity("/api/tech-stack", String.class);
        assertThat(response.getStatusCode().value()).isEqualTo(200);

        JsonNode data = json(response).path("data");
        assertThat(data.size()).isEqualTo(2);
        assertThat(data.get(0).path("name").asText()).isEqualTo("前端-A");
        assertThat(data.get(1).path("name").asText()).isEqualTo("后端-B");
        assertThat(data.get(0).path("icon").asText()).isEqualTo("vuejs");
    }

    @Test
    @Order(2)
    void adminWithoutTokenReturns401() {
        ResponseEntity<String> response = rest.getForEntity("/admin/api/tech-stack", String.class);
        assertThat(response.getStatusCode().value()).isEqualTo(401);
    }

    @Test
    @Order(3)
    void crudFlowReflectsInPublicImmediately() {
        String token = login();

        // POST 新建 → 返回 id，公开接口能查到
        ResponseEntity<String> create = rest.exchange(
                "/admin/api/tech-stack",
                HttpMethod.POST,
                jsonBody("{\"name\":\"MySQL\",\"category\":\"数据库\","
                        + "\"proficiency\":\"familiar\",\"weight\":3,\"sortOrder\":3}", token),
                String.class);
        assertThat(create.getStatusCode().value()).isEqualTo(200);
        long id = json(create).path("data").path("id").asLong();
        assertThat(id).isGreaterThan(0);
        CREATED_IDS.add(id);

        assertThat(publicNames()).contains("MySQL");

        // PUT 更新 → 公开接口反映新值
        ResponseEntity<String> update = rest.exchange(
                "/admin/api/tech-stack/" + id,
                HttpMethod.PUT,
                jsonBody("{\"name\":\"MySQL 8\",\"category\":\"数据库\","
                        + "\"proficiency\":\"proficient\",\"weight\":6,\"sortOrder\":3}", token),
                String.class);
        assertThat(update.getStatusCode().value()).isEqualTo(200);

        ResponseEntity<String> afterUpdate = rest.getForEntity("/api/tech-stack", String.class);
        JsonNode updatedItem = findByJsonName(json(afterUpdate).path("data"), "MySQL 8");
        assertThat(updatedItem).isNotNull();
        assertThat(updatedItem.path("proficiency").asText()).isEqualTo("proficient");
        assertThat(updatedItem.path("weight").asInt()).isEqualTo(6);

        // DELETE → 公开接口不再返回
        ResponseEntity<String> delete = rest.exchange(
                "/admin/api/tech-stack/" + id, HttpMethod.DELETE, withToken(token), String.class);
        assertThat(delete.getStatusCode().value()).isEqualTo(200);
        assertThat(publicNames()).doesNotContain("MySQL 8");
    }

    @Test
    @Order(4)
    void invalidEnumOrMissingNameReturns400() {
        String token = login();

        ResponseEntity<String> badEnum = rest.exchange(
                "/admin/api/tech-stack",
                HttpMethod.POST,
                jsonBody("{\"name\":\"X\",\"category\":\"前端\",\"proficiency\":\"expert\",\"weight\":1}", token),
                String.class);
        assertThat(badEnum.getStatusCode().value()).isEqualTo(400);
        assertThat(json(badEnum).path("code").asInt()).isEqualTo(400);

        ResponseEntity<String> missingName = rest.exchange(
                "/admin/api/tech-stack",
                HttpMethod.POST,
                jsonBody("{\"category\":\"前端\",\"proficiency\":\"master\",\"weight\":1}", token),
                String.class);
        assertThat(missingName.getStatusCode().value()).isEqualTo(400);

        String longCategory = "c".repeat(21);
        ResponseEntity<String> longCategoryResp = rest.exchange(
                "/admin/api/tech-stack",
                HttpMethod.POST,
                jsonBody("{\"name\":\"X\",\"category\":\"" + longCategory + "\","
                        + "\"proficiency\":\"master\",\"weight\":1}", token),
                String.class);
        assertThat(longCategoryResp.getStatusCode().value()).isEqualTo(400);
    }

    @Test
    @Order(5)
    void updateOrDeleteUnknownIdReturns404() {
        String token = login();

        ResponseEntity<String> update = rest.exchange(
                "/admin/api/tech-stack/999999",
                HttpMethod.PUT,
                jsonBody("{\"name\":\"X\",\"category\":\"前端\",\"proficiency\":\"master\",\"weight\":1}", token),
                String.class);
        assertThat(update.getStatusCode().value()).isEqualTo(404);

        ResponseEntity<String> delete = rest.exchange(
                "/admin/api/tech-stack/999999", HttpMethod.DELETE, withToken(token), String.class);
        assertThat(delete.getStatusCode().value()).isEqualTo(404);
    }

    private List<String> publicNames() {
        ResponseEntity<String> response = rest.getForEntity("/api/tech-stack", String.class);
        JsonNode data = json(response).path("data");
        List<String> names = new ArrayList<>();
        for (JsonNode node : data) {
            names.add(node.path("name").asText());
        }
        return names;
    }

    private JsonNode findByJsonName(JsonNode array, String name) {
        for (JsonNode node : array) {
            if (name.equals(node.path("name").asText())) {
                return node;
            }
        }
        return null;
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
}