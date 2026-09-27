package com.whoami;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
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
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Spec 05 留言板契约集成测试：必填/长度/邮箱 400；同 IP 每分钟第 4 条 429（含窗口滚动）；
 * 默认 approved；公开接口响应绝无 email（契约断言）；后台回复/上下架/删除全流程；未登录 401。
 */
@Testcontainers(disabledWithoutDocker = true)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class GuestMessageApiIT {

    private static final String TEST_SECRET = "it-test-jwt-secret-0123456789abcdef-0123";
    private static final String ADMIN_PASSWORD = "Admin@whoami2026";
    private static final ObjectMapper JSON = new ObjectMapper();

    @Container
    static final MySQLContainer<?> MYSQL = new MySQLContainer<>("mysql:8.0")
            .withDatabaseName("whoami")
            .withUsername("whoami")
            .withPassword("whoami-test")
            .withUrlParam("serverTimezone", "Asia/Shanghai");

    @DynamicPropertySource
    static void datasourceProps(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", MYSQL::getJdbcUrl);
        registry.add("spring.datasource.username", MYSQL::getUsername);
        registry.add("spring.datasource.password", MYSQL::getPassword);
        registry.add("app.jwt.secret", () -> TEST_SECRET);
    }

    @Autowired
    private TestRestTemplate rest;

    @Autowired
    private JdbcTemplate jdbc;

    @AfterAll
    static void cleanup(@Autowired JdbcTemplate jdbc) {
        jdbc.update("DELETE FROM guest_message");
    }

    private ResponseEntity<String> post(String body, String forwardedFor) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        if (forwardedFor != null) {
            headers.set("X-Forwarded-For", forwardedFor);
        }
        return rest.postForEntity("/api/messages", new HttpEntity<>(body, headers), String.class);
    }

    private ResponseEntity<String> exchange(String uri, HttpMethod method, String body, String token) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        if (token != null) {
            headers.setBearerAuth(token);
        }
        return rest.exchange(uri, method, new HttpEntity<>(body, headers), String.class);
    }

    private JsonNode json(ResponseEntity<String> response) {
        try {
            return JSON.readTree(response.getBody());
        } catch (Exception e) {
            throw new IllegalStateException("响应不是合法 JSON: " + response.getBody(), e);
        }
    }

    private String login() {
        ResponseEntity<String> login = exchange("/admin/api/auth/login", HttpMethod.POST,
                "{\"username\":\"admin\",\"password\":\"" + ADMIN_PASSWORD + "\"}", null);
        assertThat(login.getStatusCode().value()).isEqualTo(200);
        return json(login).path("data").path("token").asText();
    }

    @Test
    @Order(1)
    void createValidatesRequiredLengthAndEmailWith400() {
        // nickname 缺失
        assertThat(post("{\"content\":\"hi\"}", "198.51.100.1").getStatusCode().value()).isEqualTo(400);
        // nickname 超长（21 字）
        assertThat(post("{\"nickname\":\"一二三四五六七八九十一二三四五六七八九十一\",\"content\":\"hi\"}",
                "198.51.100.1").getStatusCode().value()).isEqualTo(400);
        // content 缺失
        assertThat(post("{\"nickname\":\"张三\"}", "198.51.100.1").getStatusCode().value()).isEqualTo(400);
        // content 超长（501 字）
        assertThat(post("{\"nickname\":\"张三\",\"content\":\"" + "字".repeat(501) + "\"}",
                "198.51.100.1").getStatusCode().value()).isEqualTo(400);
        // email 非法格式
        ResponseEntity<String> badEmail = post(
                "{\"nickname\":\"张三\",\"content\":\"hi\",\"email\":\"not-an-email\"}", "198.51.100.1");
        assertThat(badEmail.getStatusCode().value()).isEqualTo(400);
        assertThat(json(badEmail).path("message").asText()).contains("email");
    }

    @Test
    @Order(2)
    void fourthMessageWithinOneMinuteRejectedWith429() {
        String ip = "198.51.100.7";
        for (int i = 1; i <= 3; i++) {
            ResponseEntity<String> response =
                    post("{\"nickname\":\"张三\",\"content\":\"第" + i + "条\"}", ip);
            assertThat(response.getStatusCode().value()).as("第 %d 条应通过", i).isEqualTo(200);
        }
        ResponseEntity<String> fourth = post("{\"nickname\":\"张三\",\"content\":\"第4条\"}", ip);
        assertThat(fourth.getStatusCode().value()).isEqualTo(429);
        assertThat(json(fourth).path("code").asInt()).isEqualTo(429);

        // 限流按 IP 维度：换 IP 立即可发
        assertThat(post("{\"nickname\":\"李四\",\"content\":\"另一访客\"}", "198.51.100.8")
                .getStatusCode().value()).isEqualTo(200);

        // 窗口滚动：旧数据移出一分钟窗口后同 IP 恢复可发
        jdbc.update("UPDATE guest_message SET created_at = DATE_SUB(created_at, INTERVAL 61 SECOND)"
                + " WHERE ip = ?", ip);
        assertThat(post("{\"nickname\":\"张三\",\"content\":\"窗口外\"}", ip)
                .getStatusCode().value()).isEqualTo(200);
    }

    @Test
    @Order(3)
    void createDefaultsApprovedAndPublicListHidesEmail() {
        assertThat(post("{\"nickname\":\"王五\",\"content\":\"站长加油\",\"email\":\"wangwu@example.com\"}",
                "203.0.113.20").getStatusCode().value()).isEqualTo(200);
        assertThat(jdbc.queryForObject(
                "SELECT status FROM guest_message WHERE nickname = '王五'", String.class))
                .isEqualTo("approved");

        ResponseEntity<String> list = rest.getForEntity("/api/messages", String.class);
        assertThat(list.getStatusCode().value()).isEqualTo(200);
        JsonNode data = json(list).path("data");
        assertThat(data.isArray()).isTrue();
        JsonNode mine = null;
        for (JsonNode item : data) {
            if ("王五".equals(item.path("nickname").asText())) {
                mine = item;
            }
        }
        assertThat(mine).as("公开列表应包含默认 approved 的留言").isNotNull();
        // 契约断言：公开 DTO 绝不含 email，也不含任何 IP
        assertThat(mine.has("email")).isFalse();
        assertThat(mine.has("ip")).isFalse();
        assertThat(mine.path("content").asText()).isEqualTo("站长加油");
        assertThat(mine.path("reply").isNull()).isTrue();
        assertThat(mine.path("createdAt").asText()).isNotEmpty();
        assertThat(mine.toString()).doesNotContain("example.com");
    }

    @Test
    @Order(4)
    void publicListLimitAndOnlyApprovedVisible() {
        // hidden 的留言不下发前台
        jdbc.update("INSERT INTO guest_message (nickname, email, content, status, ip, created_at)"
                + " VALUES ('隐藏者', NULL, '不该出现', 'hidden', '203.0.113.99', NOW())");
        ResponseEntity<String> list = rest.getForEntity("/api/messages", String.class);
        assertThat(json(list).path("data").toString()).doesNotContain("不该出现");

        // limit 生效（当前 approved 已 ≥ 3 条：张三×4、李四、王五、隐藏者之外）
        ResponseEntity<String> limited = rest.getForEntity("/api/messages?limit=2", String.class);
        JsonNode data = json(limited).path("data");
        assertThat(data).hasSize(2);
    }

    @Test
    @Order(5)
    void adminFlowReplyStatusDeleteAndMaskedIp() {
        String token = login();
        long id = jdbc.queryForObject("SELECT id FROM guest_message WHERE nickname = '王五'", Long.class);

        // 后台列表：含 email（仅站主可见）与脱敏 IP（1.2.*.* 形态），绝不输出原文
        ResponseEntity<String> adminList = exchange("/admin/api/messages", HttpMethod.GET, null, token);
        assertThat(adminList.getStatusCode().value()).isEqualTo(200);
        JsonNode rows = json(adminList).path("data");
        JsonNode target = null;
        for (JsonNode row : rows) {
            if (row.path("id").asLong() == id) {
                target = row;
            }
        }
        assertThat(target).isNotNull();
        assertThat(target.path("email").asText()).isEqualTo("wangwu@example.com");
        assertThat(target.path("status").asText()).isEqualTo("approved");
        assertThat(target.path("ip").asText()).isEqualTo("203.0.113.*");
        assertThat(target.path("ip").asText()).doesNotContain("203.0.113.20");

        // 回复：前台可见 reply 与 repliedAt
        assertThat(exchange("/admin/api/messages/" + id + "/reply", HttpMethod.PUT,
                "{\"reply\":\"多谢关注\"}", token).getStatusCode().value()).isEqualTo(200);
        ResponseEntity<String> publicList = rest.getForEntity("/api/messages", String.class);
        JsonNode replied = null;
        for (JsonNode item : json(publicList).path("data")) {
            if (item.path("id").asLong() == id) {
                replied = item;
            }
        }
        assertThat(replied.path("reply").asText()).isEqualTo("多谢关注");
        assertThat(replied.path("repliedAt").asText()).isNotEmpty();
        // 公开响应仍无 email
        assertThat(replied.has("email")).isFalse();

        // 下架：前台不可见，后台按状态可过滤
        assertThat(exchange("/admin/api/messages/" + id + "/status", HttpMethod.PUT,
                "{\"status\":\"hidden\"}", token).getStatusCode().value()).isEqualTo(200);
        assertThat(rest.getForEntity("/api/messages", String.class).getBody()).doesNotContain("站长加油");
        ResponseEntity<String> hiddenList =
                exchange("/admin/api/messages?status=hidden", HttpMethod.GET, null, token);
        JsonNode hiddenRows = json(hiddenList).path("data");
        boolean found = false;
        for (JsonNode row : hiddenRows) {
            if (row.path("id").asLong() == id) {
                found = true;
            }
        }
        assertThat(found).isTrue();

        // 非法 status → 400
        assertThat(exchange("/admin/api/messages/" + id + "/status", HttpMethod.PUT,
                "{\"status\":\"bogus\"}", token).getStatusCode().value()).isEqualTo(400);

        // 重新上架后删除：前台与后台均消失
        assertThat(exchange("/admin/api/messages/" + id + "/status", HttpMethod.PUT,
                "{\"status\":\"approved\"}", token).getStatusCode().value()).isEqualTo(200);
        assertThat(exchange("/admin/api/messages/" + id, HttpMethod.DELETE, null, token)
                .getStatusCode().value()).isEqualTo(200);
        assertThat(jdbc.queryForObject(
                "SELECT COUNT(*) FROM guest_message WHERE id = ?", Integer.class, id)).isZero();
        assertThat(rest.getForEntity("/api/messages", String.class).getBody()).doesNotContain("站长加油");
    }

    @Test
    @Order(6)
    void adminOperationsOnMissingMessage404AndRequireToken() {
        assertThat(exchange("/admin/api/messages/999999/reply", HttpMethod.PUT,
                "{\"reply\":\"x\"}", null).getStatusCode().value()).isEqualTo(401);
        String token = login();
        assertThat(exchange("/admin/api/messages/999999/reply", HttpMethod.PUT,
                "{\"reply\":\"x\"}", token).getStatusCode().value()).isEqualTo(404);
        assertThat(exchange("/admin/api/messages/999999/status", HttpMethod.PUT,
                "{\"status\":\"hidden\"}", token).getStatusCode().value()).isEqualTo(404);
        assertThat(exchange("/admin/api/messages/999999", HttpMethod.DELETE, null, token)
                .getStatusCode().value()).isEqualTo(404);
        assertThat(exchange("/admin/api/messages", HttpMethod.GET, null, null)
                .getStatusCode().value()).isEqualTo(401);
        // 非法 status 过滤值 → 400
        assertThat(exchange("/admin/api/messages?status=bogus", HttpMethod.GET, null, token)
                .getStatusCode().value()).isEqualTo(400);
    }
}
