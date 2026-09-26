package com.whoami;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpServer;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
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
 * Spec 04 契约集成测试：GitHub 同步链路（mock GitHub REST API）+ 公开/管理 API + 缓存兜底 + 置顶上限。
 * mock 服务器用 JDK HttpServer（零额外依赖），通过 app.github.api-base 注入。
 */
@Testcontainers(disabledWithoutDocker = true)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class ProjectApiIT {

    private static final String TEST_SECRET = "it-test-jwt-secret-0123456789abcdef-0123";
    private static final String ADMIN_PASSWORD = "Admin@whoami2026";
    private static final ObjectMapper JSON = new ObjectMapper();

    @Container
    static final MySQLContainer<?> MYSQL = new MySQLContainer<>("mysql:8.0")
            .withDatabaseName("whoami")
            .withUsername("whoami")
            .withPassword("whoami-test");

    /** GitHub API mock：body/status 可切换，模拟同步数据变化与服务不可用 */
    static final MockGitHub GITHUB = new MockGitHub();

    static final class MockGitHub {
        final HttpServer server;
        final AtomicInteger status = new AtomicInteger(200);
        final AtomicReference<String> body = new AtomicReference<>("[]");

        MockGitHub() {
            try {
                server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
                server.createContext("/", exchange -> {
                    byte[] payload = body.get().getBytes(StandardCharsets.UTF_8);
                    exchange.getResponseHeaders().set("Content-Type", "application/json");
                    exchange.sendResponseHeaders(status.get(), payload.length);
                    try (OutputStream out = exchange.getResponseBody()) {
                        out.write(payload);
                    }
                });
                server.start();
            } catch (Exception e) {
                throw new IllegalStateException("mock GitHub 启动失败", e);
            }
        }

        String baseUrl() {
            return "http://127.0.0.1:" + server.getAddress().getPort();
        }

        void stop() {
            server.stop(0);
        }
    }

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
        registry.add("app.github.token", () -> "test-pat");
        registry.add("app.github.owner", () -> "idonhve");
        registry.add("app.github.api-base", GITHUB::baseUrl);
    }

    @AfterAll
    static void cleanup(@Autowired JdbcTemplate jdbc) {
        GITHUB.stop();
        jdbc.update("DELETE FROM sync_task_log");
        jdbc.update("DELETE FROM project");
    }

    private static String repos(String... entries) {
        return "[" + String.join(",", entries) + "]";
    }

    private static String repo(long id, String name, String description, String language, int stars, int forks) {
        return "{\"id\":" + id + ",\"name\":\"" + name + "\",\"full_name\":\"idonhve/" + name
                + "\",\"description\":" + (description == null ? "null" : "\"" + description + "\"")
                + ",\"language\":\"" + language + "\",\"stargazers_count\":" + stars
                + ",\"forks_count\":" + forks
                + ",\"html_url\":\"https://github.com/idonhve/" + name
                + "\",\"pushed_at\":\"2026-08-25T09:00:00\"}";
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

    private ResponseEntity<String> sync(String token) {
        return rest.exchange("/admin/api/projects/sync", HttpMethod.POST, withToken(token), String.class);
    }

    @Test
    @Order(1)
    void manualSyncInsertsReposAndPublicListServesThem() {
        GITHUB.status.set(200);
        GITHUB.body.set(repos(
                repo(101, "whoami-site", "personal homepage", "Java", 12, 3),
                repo(102, "cli-tool", null, "TypeScript", 5, 1)));

        ResponseEntity<String> sync = sync(login());

        assertThat(sync.getStatusCode().value()).isEqualTo(200);
        JsonNode result = json(sync).path("data");
        assertThat(result.path("status").asText()).isEqualTo("success");
        assertThat(result.path("repoCount").asInt()).isEqualTo(2);
        assertThat(result.path("hiddenGone").asInt()).isZero();

        ResponseEntity<String> publicList = rest.getForEntity("/api/projects", String.class);
        assertThat(publicList.getStatusCode().value()).isEqualTo(200);
        JsonNode data = json(publicList).path("data");
        assertThat(data).hasSize(2);
        // cnTitle 回退 description；语言/star/fork/url 透传
        assertThat(data.get(0).path("cnTitle").asText()).isEqualTo("personal homepage");
        assertThat(data.get(0).path("language").asText()).isEqualTo("Java");
        assertThat(data.get(0).path("stargazersCount").asInt()).isEqualTo(12);
        assertThat(data.get(0).path("forksCount").asInt()).isEqualTo(3);
        assertThat(data.get(0).path("htmlUrl").asText()).isEqualTo("https://github.com/idonhve/whoami-site");
        assertThat(data.get(0).path("pushedAt").asText()).isNotEmpty();
        // 无 cnTitle 且 GitHub description 为空 → 空标题（前端回退仓库名展示）
        assertThat(data.get(1).path("cnTitle").isNull()).isTrue();
    }

    @Test
    @Order(2)
    void adminUpdatePinCnTitleReflectInPublicAndFeaturedScope() {
        String token = login();
        Long id = jdbc.queryForObject("SELECT id FROM project WHERE repo_id = 101", Long.class);

        ResponseEntity<String> put = rest.exchange(
                "/admin/api/projects/" + id, HttpMethod.PUT,
                jsonBody("{\"cnTitle\":\"我的个人主页\",\"isPinned\":true}", token), String.class);
        assertThat(put.getStatusCode().value()).isEqualTo(200);

        // scope=featured 只含置顶项，中文描述生效（无需发版）
        ResponseEntity<String> featured = rest.getForEntity("/api/projects?scope=featured", String.class);
        JsonNode data = json(featured).path("data");
        assertThat(data).hasSize(1);
        assertThat(data.get(0).path("cnTitle").asText()).isEqualTo("我的个人主页");
        assertThat(data.get(0).path("isPinned").asBoolean()).isTrue();
    }

    @Test
    @Order(3)
    void hidingProjectRemovesItFromPublicList() {
        String token = login();
        Long id = jdbc.queryForObject("SELECT id FROM project WHERE repo_id = 102", Long.class);

        rest.exchange("/admin/api/projects/" + id, HttpMethod.PUT,
                jsonBody("{\"isHidden\":true}", token), String.class);

        ResponseEntity<String> publicList = rest.getForEntity("/api/projects", String.class);
        assertThat(json(publicList).path("data")).hasSize(1);
    }

    @Test
    @Order(4)
    void resyncUpdatesMetadataKeepsOperationalFieldsAndHidesGone() {
        // repo 101 star 数变化；repo 102 已消失；repo 103 新增
        GITHUB.body.set(repos(
                repo(101, "whoami-site", "updated description", "Java", 99, 3),
                repo(103, "new-repo", "fresh", "Go", 1, 0)));

        ResponseEntity<String> sync = sync(login());
        JsonNode result = json(sync).path("data");
        assertThat(result.path("status").asText()).isEqualTo("success");
        assertThat(result.path("repoCount").asInt()).isEqualTo(2);
        assertThat(result.path("hiddenGone").asInt()).isEqualTo(1);

        // 元数据更新 + 运营字段保留（cn_title 不被覆盖）
        Map<String, Object> row = jdbc.queryForMap(
                "SELECT stargazers_count, cn_title, is_pinned FROM project WHERE repo_id = 101");
        assertThat(((Number) row.get("stargazers_count")).intValue()).isEqualTo(99);
        assertThat(row.get("cn_title")).isEqualTo("我的个人主页");
        assertThat(((Number) row.get("is_pinned")).intValue()).isEqualTo(1);
        // 消失仓库不删除、自动隐藏
        Map<String, Object> gone = jdbc.queryForMap(
                "SELECT is_hidden FROM project WHERE repo_id = 102");
        assertThat(((Number) gone.get("is_hidden")).intValue()).isEqualTo(1);
        // 新仓库入库存默认值
        assertThat(jdbc.queryForObject(
                "SELECT COUNT(*) FROM project WHERE repo_id = 103 AND is_pinned = 0 AND is_hidden = 0",
                Integer.class)).isEqualTo(1);
    }

    @Test
    @Order(5)
    void githubUnavailableFailsSyncButPublicListServesCachedData() {
        GITHUB.status.set(500);

        ResponseEntity<String> sync = sync(login());

        // 同步失败：200 包络内 status=failed + 原因（不 500）
        assertThat(sync.getStatusCode().value()).isEqualTo(200);
        JsonNode result = json(sync).path("data");
        assertThat(result.path("status").asText()).isEqualTo("failed");
        assertThat(result.path("message").asText()).contains("500");

        // 前台照常展示最近一次同步的缓存数据
        ResponseEntity<String> publicList = rest.getForEntity("/api/projects", String.class);
        assertThat(publicList.getStatusCode().value()).isEqualTo(200);
        assertThat(json(publicList).path("data").size()).isGreaterThanOrEqualTo(2);
    }

    @Test
    @Order(6)
    void syncLogsVisibleWithTriggerTypeAndMessage() {
        String token = login();

        ResponseEntity<String> logs = rest.exchange(
                "/admin/api/projects/sync/logs", HttpMethod.GET, withToken(token), String.class);
        assertThat(logs.getStatusCode().value()).isEqualTo(200);
        JsonNode data = json(logs).path("data");
        assertThat(data.size()).isGreaterThanOrEqualTo(4);
        boolean hasFailedWithReason = false;
        boolean hasManual = false;
        for (JsonNode entry : data) {
            if ("failed".equals(entry.path("status").asText()) && entry.path("message").asText().contains("500")) {
                hasFailedWithReason = true;
            }
            if ("manual".equals(entry.path("triggerType").asText())) {
                hasManual = true;
            }
        }
        assertThat(hasFailedWithReason).as("失败日志含原因").isTrue();
        assertThat(hasManual).as("手动同步记 manual").isTrue();
    }

    @Test
    @Order(7)
    void pinningFourthProjectReturns409() {
        String token = login();
        // 同步出第 4 个仓库（102 在远端回归：运营字段 is_hidden 保留，需手动恢复展示）
        GITHUB.status.set(200);
        GITHUB.body.set(repos(
                repo(101, "whoami-site", "updated description", "Java", 99, 3),
                repo(102, "cli-tool", null, "TypeScript", 5, 1),
                repo(103, "new-repo", "fresh", "Go", 1, 0),
                repo(104, "fourth-repo", "extra", "Rust", 2, 0)));
        sync(token);
        jdbc.update("UPDATE project SET is_hidden = 0");

        Long id102 = jdbc.queryForObject("SELECT id FROM project WHERE repo_id = 102", Long.class);
        Long id103 = jdbc.queryForObject("SELECT id FROM project WHERE repo_id = 103", Long.class);
        Long id104 = jdbc.queryForObject("SELECT id FROM project WHERE repo_id = 104", Long.class);

        // 101 已置顶（Order 2），补到 3 个
        assertThat(rest.exchange("/admin/api/projects/" + id102, HttpMethod.PUT,
                jsonBody("{\"isPinned\":true}", token), String.class).getStatusCode().value()).isEqualTo(200);
        assertThat(rest.exchange("/admin/api/projects/" + id103, HttpMethod.PUT,
                jsonBody("{\"isPinned\":true}", token), String.class).getStatusCode().value()).isEqualTo(200);

        // 第 4 个置顶 → 409
        ResponseEntity<String> pinFourth = rest.exchange(
                "/admin/api/projects/" + id104, HttpMethod.PUT,
                jsonBody("{\"isPinned\":true}", token), String.class);
        assertThat(pinFourth.getStatusCode().value()).isEqualTo(409);
        assertThat(json(pinFourth).path("message").asText()).contains("置顶数量已达上限 3");
    }

    @Test
    @Order(8)
    void adminListFilterByLanguagePinnedHidden() {
        String token = login();

        ResponseEntity<String> all = rest.exchange(
                "/admin/api/projects?language=Go", HttpMethod.GET, withToken(token), String.class);
        JsonNode data = json(all).path("data");
        assertThat(data).hasSize(1);
        assertThat(data.get(0).path("repoName").asText()).isEqualTo("new-repo");
        assertThat(data.get(0).path("isHidden").asBoolean()).isFalse();
        assertThat(data.get(0).path("lastSyncedAt").asText()).isNotEmpty();

        ResponseEntity<String> pinnedOnly = rest.exchange(
                "/admin/api/projects?pinned=true", HttpMethod.GET, withToken(token), String.class);
        assertThat(json(pinnedOnly).path("data").size()).isEqualTo(3);

        ResponseEntity<String> hiddenOnly = rest.exchange(
                "/admin/api/projects?hidden=false", HttpMethod.GET, withToken(token), String.class);
        assertThat(json(hiddenOnly).path("data").size()).isEqualTo(4);
    }

    @Test
    @Order(9)
    void adminAndSyncWithoutTokenReturn401() {
        assertThat(rest.getForEntity("/admin/api/projects", String.class).getStatusCode().value()).isEqualTo(401);
        assertThat(rest.exchange("/admin/api/projects/sync", HttpMethod.POST, HttpEntity.EMPTY, String.class)
                .getStatusCode().value()).isEqualTo(401);
        assertThat(rest.getForEntity("/admin/api/projects/sync/logs", String.class)
                .getStatusCode().value()).isEqualTo(401);
    }
}
