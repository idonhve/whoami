package com.whoami;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
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
 * Spec 05 统计聚合契约集成测试：daily PV/UV（按日去重 sessionId、缺失日补零）、
 * top-pages / referrers 聚合、geo 省级聚合 + 每省城市 TOP5（公开接口与后台同源且无任何 IP 信息）。
 */
@Testcontainers(disabledWithoutDocker = true)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class StatsApiIT {

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
        jdbc.update("DELETE FROM track_event");
        jdbc.update("DELETE FROM visit_log");
    }

    private ResponseEntity<String> postJson(String uri, String body) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        return rest.postForEntity(uri, new HttpEntity<>(body, headers), String.class);
    }

    private ResponseEntity<String> get(String uri, String token) {
        HttpHeaders headers = new HttpHeaders();
        if (token != null) {
            headers.setBearerAuth(token);
        }
        return rest.exchange(uri, HttpMethod.GET, new HttpEntity<>(headers), String.class);
    }

    private JsonNode json(ResponseEntity<String> response) {
        try {
            return JSON.readTree(response.getBody());
        } catch (Exception e) {
            throw new IllegalStateException("响应不是合法 JSON: " + response.getBody(), e);
        }
    }

    private String login() {
        ResponseEntity<String> login = rest.postForEntity("/admin/api/auth/login",
                new HttpEntity<>("{\"username\":\"admin\",\"password\":\"" + ADMIN_PASSWORD + "\"}",
                        jsonHeaders()), String.class);
        assertThat(login.getStatusCode().value()).isEqualTo(200);
        return json(login).path("data").path("token").asText();
    }

    private HttpHeaders jsonHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        return headers;
    }

    private void insertPageView(String sessionId, String pagePath, LocalDateTime at) {
        jdbc.update("INSERT INTO track_event (event_type, session_id, page_path, ip, created_at)"
                        + " VALUES ('page_view', ?, ?, '9.9.9.9', ?)",
                sessionId, pagePath, Timestamp.valueOf(at));
    }

    private void insertVisit(String sessionId, String province, String city, String referrer) {
        LocalDateTime at = LocalDateTime.now().withNano(0).minusMinutes(5);
        jdbc.update("INSERT INTO visit_log (session_id, ip, province, city, user_agent, referrer,"
                        + " entry_page, entry_time, visit_date) VALUES (?, '10.0.0.9', ?, ?,"
                        + " 'it-agent', ?, '/stats-it', ?, ?)",
                sessionId, province, city, referrer, Timestamp.valueOf(at), at.toLocalDate());
    }

    /** 造同一省多城数据（各城市访问数互不相同，避免 GROUP BY 无序导致的断言不稳定） */
    private void insertVisits(int count, String sessionIdPrefix, String province, String city) {
        for (int i = 1; i <= count; i++) {
            insertVisit(sessionIdPrefix + i, province, city, null);
        }
    }

    @Test
    @Order(1)
    void dailyAggregatesPvAndDeduplicatedUvPerDay() {
        String token = login();
        // 今天：会话 A 浏览 3 页、会话 B 浏览 1 页（A 的 3 次只计 1 UV）
        assertThat(postJson("/api/track/event",
                "{\"sessionId\":\"st-uv-1\",\"eventType\":\"page_view\",\"pagePath\":\"/\"}")
                .getStatusCode().value()).isEqualTo(200);
        assertThat(postJson("/api/track/event",
                "{\"sessionId\":\"st-uv-1\",\"eventType\":\"page_view\",\"pagePath\":\"/\"}")
                .getStatusCode().value()).isEqualTo(200);
        assertThat(postJson("/api/track/event",
                "{\"sessionId\":\"st-uv-1\",\"eventType\":\"page_view\",\"pagePath\":\"/about\"}")
                .getStatusCode().value()).isEqualTo(200);
        assertThat(postJson("/api/track/event",
                "{\"sessionId\":\"st-uv-2\",\"eventType\":\"page_view\",\"pagePath\":\"/\"}")
                .getStatusCode().value()).isEqualTo(200);
        // 昨天：会话 A 又活跃 2 次（跨日会话在两个活跃日各计 1 UV）
        LocalDateTime yesterday = LocalDate.now().minusDays(1).atTime(12, 0);
        insertPageView("st-uv-1", "/", yesterday);
        insertPageView("st-uv-1", "/", yesterday.plusMinutes(1));

        // days=1：仅今天一条
        JsonNode daily1 = json(get("/admin/api/stats/daily?days=1", token)).path("data");
        assertThat(daily1).hasSize(1);
        assertThat(daily1.get(0).path("date").asText()).isEqualTo(LocalDate.now().toString());
        assertThat(daily1.get(0).path("pv").asLong()).isEqualTo(4);
        assertThat(daily1.get(0).path("uv").asLong()).isEqualTo(2);

        // days=3：连续三日补零，昨天 PV=2 / UV=1（同会话去重），前天 0/0
        JsonNode daily3 = json(get("/admin/api/stats/daily?days=3", token)).path("data");
        assertThat(daily3).hasSize(3);
        assertThat(daily3.get(0).path("date").asText()).isEqualTo(LocalDate.now().minusDays(2).toString());
        assertThat(daily3.get(0).path("pv").asLong()).isZero();
        assertThat(daily3.get(0).path("uv").asLong()).isZero();
        assertThat(daily3.get(1).path("date").asText()).isEqualTo(LocalDate.now().minusDays(1).toString());
        assertThat(daily3.get(1).path("pv").asLong()).isEqualTo(2);
        assertThat(daily3.get(1).path("uv").asLong()).isEqualTo(1);
        assertThat(daily3.get(2).path("pv").asLong()).isEqualTo(4);
        assertThat(daily3.get(2).path("uv").asLong()).isEqualTo(2);

        // days 默认 30 也可用
        assertThat(json(get("/admin/api/stats/daily", token)).path("data")).hasSize(30);
    }

    @Test
    @Order(2)
    void topPagesAndReferrersAggregateInWindow() {
        String token = login();
        // top-pages（今天窗口）：/ 共 3 次（A×2 + B×1），/about 1 次，倒序
        JsonNode top = json(get("/admin/api/stats/top-pages?days=1", token)).path("data");
        assertThat(top).hasSize(2);
        assertThat(top.get(0).path("pagePath").asText()).isEqualTo("/");
        assertThat(top.get(0).path("pv").asLong()).isEqualTo(3);
        assertThat(top.get(1).path("pagePath").asText()).isEqualTo("/about");
        assertThat(top.get(1).path("pv").asLong()).isEqualTo(1);

        // referrers：接口上报 2 个 github 会话 + 1 条直连（不计）；jdbc 直插 1 条 google
        assertThat(postJson("/api/track/session",
                "{\"sessionId\":\"st-r1\",\"referrer\":\"https://github.com/\"}")
                .getStatusCode().value()).isEqualTo(200);
        assertThat(postJson("/api/track/session",
                "{\"sessionId\":\"st-r2\",\"referrer\":\"https://github.com/\"}")
                .getStatusCode().value()).isEqualTo(200);
        assertThat(postJson("/api/track/session", "{\"sessionId\":\"st-r3\"}")
                .getStatusCode().value()).isEqualTo(200);
        insertVisit("st-r4", null, null, "https://www.google.com/");

        JsonNode referrers = json(get("/admin/api/stats/referrers?days=1", token)).path("data");
        assertThat(referrers).hasSize(2);
        assertThat(referrers.get(0).path("referrer").asText()).isEqualTo("https://github.com/");
        assertThat(referrers.get(0).path("count").asLong()).isEqualTo(2);
        assertThat(referrers.get(1).path("referrer").asText()).isEqualTo("https://www.google.com/");
        assertThat(referrers.get(1).path("count").asLong()).isEqualTo(1);
    }

    @Test
    @Order(3)
    void geoAggregatesProvincesWithCityTop5AndNoIpLeak() {
        // 广东省 21 次：广州 6、深圳 5、东莞 4、佛山 3、中山 2、珠海 1（互不相同；珠海应被 TOP5 截断）
        insertVisits(6, "geo-gz-", "广东省", "广州市");
        insertVisits(5, "geo-sz-", "广东省", "深圳市");
        insertVisits(4, "geo-dg-", "广东省", "东莞市");
        insertVisits(3, "geo-fs-", "广东省", "佛山市");
        insertVisits(2, "geo-zs-", "广东省", "中山市");
        insertVisits(1, "geo-zh-", "广东省", "珠海市");
        // 江苏省 3：南京 2、苏州 1；北京市 2；浙江省 1；境外（province null）不计
        insertVisits(2, "geo-js-nj-", "江苏省", "南京市");
        insertVisits(1, "geo-js-sz-", "江苏省", "苏州市");
        insertVisits(2, "geo-bj-", "北京市", "北京市");
        insertVisits(1, "geo-zj-", "浙江省", "杭州市");
        insertVisit("geo-oversea-1", null, null, null);

        // 公开接口（前台"关于本站"地图数据源）
        ResponseEntity<String> publicGeo = rest.getForEntity("/api/visit-stats/geo", String.class);
        assertThat(publicGeo.getStatusCode().value()).isEqualTo(200);
        JsonNode data = json(publicGeo).path("data");
        assertThat(data).hasSize(4);
        assertThat(data.get(0).path("province").asText()).isEqualTo("广东省");
        assertThat(data.get(0).path("count").asLong()).isEqualTo(21);
        JsonNode cities = data.get(0).path("cities");
        assertThat(cities).hasSize(5);
        assertThat(cities.get(0).path("city").asText()).isEqualTo("广州市");
        assertThat(cities.get(0).path("count").asLong()).isEqualTo(6);
        assertThat(cities.get(1).path("city").asText()).isEqualTo("深圳市");
        assertThat(cities.get(1).path("count").asLong()).isEqualTo(5);
        assertThat(cities.get(2).path("city").asText()).isEqualTo("东莞市");
        assertThat(cities.get(3).path("city").asText()).isEqualTo("佛山市");
        assertThat(cities.get(4).path("city").asText()).isEqualTo("中山市");
        // 第 6 名珠海被 TOP5 截断
        assertThat(cities.toString()).doesNotContain("珠海市");
        assertThat(data.get(1).path("province").asText()).isEqualTo("江苏省");
        assertThat(data.get(1).path("count").asLong()).isEqualTo(3);
        assertThat(data.get(1).path("cities")).hasSize(2);
        assertThat(data.get(2).path("province").asText()).isEqualTo("北京市");
        assertThat(data.get(2).path("count").asLong()).isEqualTo(2);
        assertThat(data.get(3).path("province").asText()).isEqualTo("浙江省");
        assertThat(data.get(3).path("count").asLong()).isEqualTo(1);
        // 红线：整棵响应树不含任何 IP 字段 / 原文 IP
        assertThat(publicGeo.getBody()).doesNotContain("\"ip\"");
        assertThat(publicGeo.getBody()).doesNotContain("10.0.0.9");

        // 后台 geo 与公开同源同数据
        String token = login();
        JsonNode adminGeo = json(get("/admin/api/stats/geo", token)).path("data");
        assertThat(adminGeo).hasSize(4);
        assertThat(adminGeo.get(0).path("province").asText()).isEqualTo("广东省");
        assertThat(adminGeo.get(0).path("count").asLong()).isEqualTo(21);
    }

    @Test
    @Order(4)
    void adminStatsRequireToken() {
        assertThat(get("/admin/api/stats/daily", null).getStatusCode().value()).isEqualTo(401);
        assertThat(get("/admin/api/stats/top-pages", null).getStatusCode().value()).isEqualTo(401);
        assertThat(get("/admin/api/stats/referrers", null).getStatusCode().value()).isEqualTo(401);
        assertThat(get("/admin/api/stats/geo", null).getStatusCode().value()).isEqualTo(401);
        assertThat(rest.getForEntity("/api/visit-stats/geo", String.class).getStatusCode().value())
                .isEqualTo(200);
    }
}
