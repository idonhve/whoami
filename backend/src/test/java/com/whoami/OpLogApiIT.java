package com.whoami;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Map;
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
 * Spec 06 操作日志契约测试：登录成功/失败留痕、写接口自动留痕且敏感字段脱敏、分页正确。
 */
@Testcontainers(disabledWithoutDocker = true)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class OpLogApiIT {

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

    @Test
    @Order(1)
    void failedLoginIsRecordedWithMaskedPassword() {
        long before = maxLogId();

        ResponseEntity<String> response = rest.postForEntity(
                "/admin/api/auth/login",
                jsonBody("{\"username\":\"admin\",\"password\":\"wrong-password\"}", null),
                String.class);
        assertThat(response.getStatusCode().value()).isEqualTo(401);
        resetLoginState();

        Map<String, Object> row = latestLogAfter(before);
        assertThat(row.get("action")).isEqualTo("LOGIN");
        assertThat(row.get("resource")).isEqualTo("/admin/api/auth/login");
        String detail = (String) row.get("detail");
        assertThat(detail).contains("\"result\":\"fail\"").contains("***");
        assertThat(detail).doesNotContain("wrong-password");
    }

    @Test
    @Order(2)
    void successfulLoginIsRecordedWithAdminId() {
        long before = maxLogId();

        String token = login();

        assertThat(token).isNotBlank();
        Map<String, Object> row = latestLogAfter(before);
        assertThat(row.get("action")).isEqualTo("LOGIN");
        assertThat(((Number) row.get("admin_user_id")).longValue()).isEqualTo(1L);
        assertThat((String) row.get("detail")).contains("\"result\":\"success\"");
        assertThat(row.get("ip")).isNotNull();
    }

    @Test
    @Order(3)
    void writeOperationIsRecordedWithResourceId() {
        String token = login();
        long before = maxLogId();

        ResponseEntity<String> put = rest.exchange(
                "/admin/api/site-config/owner_name",
                HttpMethod.PUT,
                jsonBody("{\"value\":\"日志测试站主\"}", token),
                String.class);
        assertThat(put.getStatusCode().value()).isEqualTo(200);

        Map<String, Object> row = latestLogAfter(before);
        assertThat(row.get("action")).isEqualTo("PUT");
        assertThat(row.get("resource")).isEqualTo("/admin/api/site-config/owner_name");
        assertThat(row.get("resource_id")).isEqualTo("owner_name");
        assertThat(((Number) row.get("admin_user_id")).longValue()).isEqualTo(1L);
        assertThat((String) row.get("detail")).contains("日志测试站主").contains("\"result\":\"success\"");

        jdbc.update("UPDATE site_config SET config_value = '站主' WHERE config_key = 'owner_name'");
    }

    @Test
    @Order(4)
    void readOperationsAreNotRecorded() {
        String token = login();
        long before = maxLogId();

        ResponseEntity<String> get = rest.exchange(
                "/admin/api/site-config", HttpMethod.GET, withToken(token), String.class);
        assertThat(get.getStatusCode().value()).isEqualTo(200);

        assertThat(maxLogId()).isEqualTo(before);
    }

    @Test
    @Order(5)
    void opLogsPaginationReturnsNewestFirst() {
        String token = login();

        ResponseEntity<String> page1 = rest.exchange(
                "/admin/api/op-logs?page=1&size=2", HttpMethod.GET, withToken(token), String.class);

        assertThat(page1.getStatusCode().value()).isEqualTo(200);
        JsonNode data = json(page1).path("data");
        assertThat(data.path("total").asLong()).isGreaterThanOrEqualTo(2);
        JsonNode list = data.path("list");
        assertThat(list).hasSize(2);
        // 倒序：第一页第一条的 id 必须大于第二条
        assertThat(list.get(0).path("id").asLong()).isGreaterThan(list.get(1).path("id").asLong());

        ResponseEntity<String> page2 = rest.exchange(
                "/admin/api/op-logs?page=2&size=2", HttpMethod.GET, withToken(token), String.class);
        JsonNode list2 = json(page2).path("data").path("list");
        assertThat(list2.get(0).path("id").asLong()).isLessThan(list.get(1).path("id").asLong());
    }

    @Test
    @Order(6)
    void opLogsWithoutTokenReturns401() {
        ResponseEntity<String> response = rest.getForEntity("/admin/api/op-logs", String.class);

        assertThat(response.getStatusCode().value()).isEqualTo(401);
    }

    private long maxLogId() {
        Long max = jdbc.queryForObject("SELECT COALESCE(MAX(id), 0) FROM admin_op_log", Long.class);
        return max == null ? 0 : max;
    }

    private Map<String, Object> latestLogAfter(long id) {
        List<Map<String, Object>> rows = jdbc.queryForList(
                "SELECT * FROM admin_op_log WHERE id > ? ORDER BY id DESC LIMIT 1", id);
        assertThat(rows).as("admin_op_log 应新增一条记录").isNotEmpty();
        return rows.get(0);
    }

    private void resetLoginState() {
        jdbc.update("UPDATE admin_user SET failed_attempts = 0, locked_until = NULL WHERE username = 'admin'");
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
