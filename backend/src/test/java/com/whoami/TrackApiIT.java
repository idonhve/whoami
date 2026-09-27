package com.whoami;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;
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
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Spec 05 埋点契约集成测试：session start 写 visit_log（IP/省市/UA/entry_page/visit_date 后端补齐，
 * ip2region 经 X-Forwarded-For 用已知 IP 断言省市）、重复 sessionId 幂等、
 * end 计算 duration 且幂等 + 离开页 PV 兜底补记、eventType 非枚举 400。
 */
@Testcontainers(disabledWithoutDocker = true)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class TrackApiIT {

    private static final String TEST_SECRET = "it-test-jwt-secret-0123456789abcdef-0123";
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
        jdbc.update("DELETE FROM track_event");
        jdbc.update("DELETE FROM visit_log");
    }

    private ResponseEntity<String> post(String uri, String body, Map<String, String> headers) {
        HttpHeaders httpHeaders = new HttpHeaders();
        httpHeaders.setContentType(MediaType.APPLICATION_JSON);
        headers.forEach(httpHeaders::set);
        return rest.postForEntity(uri, new HttpEntity<>(body, httpHeaders), String.class);
    }

    private ResponseEntity<String> post(String uri, String body) {
        return post(uri, body, new HashMap<>());
    }

    private JsonNode json(ResponseEntity<String> response) {
        try {
            return JSON.readTree(response.getBody());
        } catch (Exception e) {
            throw new IllegalStateException("响应不是合法 JSON: " + response.getBody(), e);
        }
    }

    @Test
    @Order(1)
    void sessionStartWritesVisitLogWithResolvedGeo() {
        ResponseEntity<String> response = post("/api/track/session",
                "{\"sessionId\":\"it-track-s1\",\"referrer\":\"https://github.com/\",\"entryPage\":\"/about\"}",
                Map.of("X-Forwarded-For", "114.114.114.114", "User-Agent", "IT-Agent/1.0"));
        assertThat(response.getStatusCode().value()).isEqualTo(200);
        JsonNode body = json(response);
        assertThat(body.path("code").asInt()).isZero();
        assertThat(body.path("data").isNull()).isTrue();

        Map<String, Object> row = jdbc.queryForMap(
                "SELECT ip, province, city, user_agent, referrer, entry_page, entry_time, visit_date,"
                        + " leave_time, duration_seconds"
                        + " FROM visit_log WHERE session_id = 'it-track-s1'");
        assertThat(row.get("ip")).isEqualTo("114.114.114.114");
        // ip2region 已知 IP 断言：114DNS → 江苏省南京市
        assertThat((String) row.get("province")).contains("江苏");
        assertThat((String) row.get("city")).contains("南京");
        assertThat(row.get("user_agent")).isEqualTo("IT-Agent/1.0");
        assertThat(row.get("referrer")).isEqualTo("https://github.com/");
        assertThat(row.get("entry_page")).isEqualTo("/about");
        assertThat(row.get("entry_time")).isNotNull();
        assertThat(((java.sql.Date) row.get("visit_date")).toLocalDate()).isEqualTo(LocalDate.now());
        assertThat(row.get("leave_time")).isNull();
        assertThat(row.get("duration_seconds")).isNull();
    }

    @Test
    @Order(2)
    void sessionStartIgnoresDuplicateSessionId() {
        int before = jdbc.queryForObject(
                "SELECT COUNT(*) FROM visit_log WHERE session_id = 'it-track-s1'", Integer.class);
        ResponseEntity<String> response = post("/api/track/session",
                "{\"sessionId\":\"it-track-s1\"}",
                Map.of("X-Forwarded-For", "8.8.8.8"));
        assertThat(response.getStatusCode().value()).isEqualTo(200);
        int after = jdbc.queryForObject(
                "SELECT COUNT(*) FROM visit_log WHERE session_id = 'it-track-s1'", Integer.class);
        assertThat(after).isEqualTo(before);
    }

    @Test
    @Order(3)
    void sessionEndComputesDurationBackfillsPvAndIsIdempotent() {
        post("/api/track/session", "{\"sessionId\":\"it-track-s2\",\"entryPage\":\"/\"}");
        // 回拨进入时间 130 秒，模拟停留 130s 后离开
        jdbc.update("UPDATE visit_log SET entry_time = DATE_SUB(entry_time, INTERVAL 130 SECOND)"
                + " WHERE session_id = 'it-track-s2'");

        ResponseEntity<String> end = post("/api/track/session/it-track-s2/end",
                "{\"lastPagePath\":\"/about\"}");
        assertThat(end.getStatusCode().value()).isEqualTo(200);

        Integer duration = jdbc.queryForObject(
                "SELECT duration_seconds FROM visit_log WHERE session_id = 'it-track-s2'", Integer.class);
        Timestamp leaveTime = jdbc.queryForObject(
                "SELECT leave_time FROM visit_log WHERE session_id = 'it-track-s2'", Timestamp.class);
        assertThat(duration).isBetween(125, 140);
        assertThat(leaveTime).isNotNull();
        // 离开页兜底：该会话在 /about 尚无 page_view → 补记一条
        assertThat(pageViewCount("it-track-s2", "/about")).isEqualTo(1);

        // 再次 end：幂等，duration / leave_time 不改写，也不重复补记 PV
        ResponseEntity<String> endAgain = post("/api/track/session/it-track-s2/end",
                "{\"lastPagePath\":\"/about\"}");
        assertThat(endAgain.getStatusCode().value()).isEqualTo(200);
        assertThat(jdbc.queryForObject(
                "SELECT duration_seconds FROM visit_log WHERE session_id = 'it-track-s2'", Integer.class))
                .isEqualTo(duration);
        assertThat(pageViewCount("it-track-s2", "/about")).isEqualTo(1);
    }

    @Test
    @Order(4)
    void endIgnoresUnknownSession() {
        ResponseEntity<String> response = post("/api/track/session/never-started/end",
                "{\"lastPagePath\":\"/\"}");
        assertThat(response.getStatusCode().value()).isEqualTo(200);
        Integer count = jdbc.queryForObject(
                "SELECT COUNT(*) FROM visit_log WHERE session_id = 'never-started'", Integer.class);
        assertThat(count).isZero();
    }

    @Test
    @Order(5)
    void eventRejectsUnknownTypeWith400() {
        ResponseEntity<String> response = post("/api/track/event",
                "{\"sessionId\":\"it-track-s1\",\"eventType\":\"hacker_event\"}");
        assertThat(response.getStatusCode().value()).isEqualTo(400);
        assertThat(json(response).path("message").asText()).contains("eventType");
    }

    @Test
    @Order(6)
    void eventRecordsPageViewWithDetailJson() {
        ResponseEntity<String> response = post("/api/track/event",
                "{\"sessionId\":\"it-track-s1\",\"eventType\":\"page_view\","
                        + "\"pagePath\":\"/about\",\"detail\":{\"from\":\"it\"}}",
                Map.of("X-Forwarded-For", "220.181.38.148"));
        assertThat(response.getStatusCode().value()).isEqualTo(200);

        Map<String, Object> row = jdbc.queryForMap(
                "SELECT event_type, page_path, detail, ip FROM track_event"
                        + " WHERE session_id = 'it-track-s1' ORDER BY id DESC LIMIT 1");
        assertThat(row.get("event_type")).isEqualTo("page_view");
        assertThat(row.get("page_path")).isEqualTo("/about");
        assertThat(row.get("ip")).isEqualTo("220.181.38.148");
        try {
            assertThat(JSON.readTree((String) row.get("detail")).path("from").asText()).isEqualTo("it");
        } catch (Exception e) {
            throw new IllegalStateException("detail 非合法 JSON: " + row.get("detail"), e);
        }
    }

    @Test
    @Order(7)
    void eventAcceptsAllSixEnumValues() {
        for (String type : new String[]{"page_view", "resume_download", "cmd_palette_use",
                "easter_egg", "github_outbound", "message_submit"}) {
            ResponseEntity<String> response = post("/api/track/event",
                    "{\"sessionId\":\"it-track-enum\",\"eventType\":\"" + type + "\"}");
            assertThat(response.getStatusCode().value()).as("eventType=%s", type).isEqualTo(200);
        }
        Integer count = jdbc.queryForObject(
                "SELECT COUNT(DISTINCT event_type) FROM track_event WHERE session_id = 'it-track-enum'",
                Integer.class);
        assertThat(count).isEqualTo(6);
    }

    @Test
    @Order(8)
    void invalidSessionBodyRejectedWith400() {
        assertThat(post("/api/track/session", "{\"referrer\":\"https://x.com/\"}")
                .getStatusCode().value()).isEqualTo(400);
        assertThat(post("/api/track/event", "{\"sessionId\":\"s\",\"eventType\":\"\"}")
                .getStatusCode().value()).isEqualTo(400);
    }

    private int pageViewCount(String sessionId, String pagePath) {
        Integer count = jdbc.queryForObject(
                "SELECT COUNT(*) FROM track_event WHERE session_id = ? AND event_type = 'page_view'"
                        + " AND page_path = ?",
                Integer.class, sessionId, pagePath);
        return count == null ? 0 : count;
    }
}
