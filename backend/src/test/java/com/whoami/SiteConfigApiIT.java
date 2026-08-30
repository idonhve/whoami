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
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Spec 06 站点配置契约测试：公开接口只含白名单键、PUT 更新后公开值变化、未知键 404。
 */
@Testcontainers(disabledWithoutDocker = true)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class SiteConfigApiIT {

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

    @AfterAll
    static void cleanup(@Autowired JdbcTemplate jdbc) {
        jdbc.update("DELETE FROM site_config WHERE config_key = 'internal_secret'");
        jdbc.update("UPDATE site_config SET config_value = 'localhost' WHERE config_key = 'domain'");
    }

    @Test
    @Order(1)
    void publicConfigContainsOnlyWhitelistKeys() {
        jdbc.update(
                "INSERT INTO site_config (config_key, config_value, description, updated_at) "
                        + "VALUES ('internal_secret', 's3cr3t', '敏感键-永不下发', NOW())");

        ResponseEntity<String> response = rest.getForEntity("/api/site-config", String.class);

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        JsonNode data = json(response).path("data");
        assertThat(data.path("domain").asText()).isEqualTo("localhost");
        assertThat(data.path("ownerName").asText()).isEqualTo("站主");
        assertThat(data.path("githubUrl").asText()).isEmpty();
        assertThat(data.path("degradeForceFull").asBoolean()).isFalse();
        // 敏感键永不下发：响应体任何位置都不应出现
        assertThat(response.getBody()).doesNotContain("internal_secret").doesNotContain("s3cr3t");
    }

    @Test
    @Order(2)
    void adminListContainsAllKeysIncludingSensitive() {
        String token = login();

        ResponseEntity<String> response = rest.exchange(
                "/admin/api/site-config", HttpMethod.GET, withToken(token), String.class);

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        JsonNode keys = json(response).path("data");
        assertThat(keys.toString()).contains("internal_secret").contains("degrade_force_full");
    }

    @Test
    @Order(3)
    void putUpdatesValueAndPublicConfigReflectsIt() {
        String token = login();

        ResponseEntity<String> put = rest.exchange(
                "/admin/api/site-config/domain",
                HttpMethod.PUT,
                jsonBody("{\"value\":\"whoami.dev\"}", token),
                String.class);
        assertThat(put.getStatusCode().value()).isEqualTo(200);

        ResponseEntity<String> publicResponse = rest.getForEntity("/api/site-config", String.class);
        assertThat(json(publicResponse).path("data").path("domain").asText()).isEqualTo("whoami.dev");
    }

    @Test
    @Order(4)
    void putUnknownKeyReturns404() {
        String token = login();

        ResponseEntity<String> response = rest.exchange(
                "/admin/api/site-config/no_such_key",
                HttpMethod.PUT,
                jsonBody("{\"value\":\"v\"}", token),
                String.class);

        assertThat(response.getStatusCode().value()).isEqualTo(404);
        assertThat(json(response).path("code").asInt()).isEqualTo(404);
    }

    @Test
    @Order(5)
    void adminSiteConfigWithoutTokenReturns401() {
        ResponseEntity<String> response = rest.getForEntity("/admin/api/site-config", String.class);

        assertThat(response.getStatusCode().value()).isEqualTo(401);
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
