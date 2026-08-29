package com.whoami;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
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

@Testcontainers(disabledWithoutDocker = true)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class AuthApiIT {

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
    void flywayCreatedAllTwelveTables() {
        List<String> tables = jdbc.queryForList(
                "SELECT table_name FROM information_schema.tables WHERE table_schema = DATABASE()",
                String.class);

        assertThat(tables).containsExactlyInAnyOrder(
                "admin_user", "tech_stack", "project", "experience", "certificate", "resume_file",
                "guest_message", "visit_log", "track_event", "admin_op_log", "site_config", "sync_task_log");
    }

    @Test
    @Order(2)
    void seedInsertedAdminAndSiteConfig() {
        Integer admins = jdbc.queryForObject("SELECT COUNT(*) FROM admin_user", Integer.class);
        assertThat(admins).isEqualTo(1);

        List<String> keys = jdbc.queryForList(
                "SELECT config_key FROM site_config ORDER BY config_key", String.class);
        assertThat(keys).containsExactlyInAnyOrder(
                "degrade_force_full", "domain", "github_url", "owner_name");
    }

    @Test
    @Order(3)
    void healthReturnsUp() {
        ResponseEntity<String> response = rest.getForEntity("/admin/api/health", String.class);

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(json(response).path("data").path("status").asText()).isEqualTo("up");
    }

    @Test
    @Order(4)
    void loginSucceedsAndMeReturnsAdmin() throws Exception {
        ResponseEntity<String> login = login(ADMIN_PASSWORD);

        assertThat(login.getStatusCode().value()).isEqualTo(200);
        String token = json(login).path("data").path("token").asText();
        assertThat(token).isNotBlank();
        assertThat(json(login).path("data").path("expiresIn").asLong()).isLessThanOrEqualTo(7200);

        ResponseEntity<String> me = rest.exchange(
                "/admin/api/auth/me", HttpMethod.GET, withToken(token), String.class);
        assertThat(me.getStatusCode().value()).isEqualTo(200);
        assertThat(json(me).path("data").path("username").asText()).isEqualTo("admin");
    }

    @Test
    @Order(5)
    void protectedApiWithoutTokenReturns401() {
        ResponseEntity<String> response = rest.getForEntity("/admin/api/auth/me", String.class);

        assertThat(response.getStatusCode().value()).isEqualTo(401);
        assertThat(json(response).path("code").asInt()).isEqualTo(401);
    }

    @Test
    @Order(6)
    void protectedApiWithGarbageTokenReturns401() {
        ResponseEntity<String> response = rest.exchange(
                "/admin/api/auth/me", HttpMethod.GET, withToken("garbage.token.value"), String.class);

        assertThat(response.getStatusCode().value()).isEqualTo(401);
    }

    @Test
    @Order(7)
    void protectedApiWithExpiredTokenReturns401() {
        String expired = Jwts.builder()
                .subject("1")
                .expiration(Date.from(Instant.now().minusSeconds(60)))
                .signWith(Keys.hmacShaKeyFor(TEST_SECRET.getBytes(StandardCharsets.UTF_8)))
                .compact();

        ResponseEntity<String> response = rest.exchange(
                "/admin/api/auth/me", HttpMethod.GET, withToken(expired), String.class);

        assertThat(response.getStatusCode().value()).isEqualTo(401);
    }

    @Test
    @Order(8)
    void refreshIssuesNewWorkingToken() throws Exception {
        String oldToken = json(login(ADMIN_PASSWORD)).path("data").path("token").asText();

        ResponseEntity<String> refresh = rest.exchange(
                "/admin/api/auth/refresh", HttpMethod.POST, withToken(oldToken), String.class);

        assertThat(refresh.getStatusCode().value()).isEqualTo(200);
        String newToken = json(refresh).path("data").path("token").asText();
        assertThat(newToken).isNotBlank().isNotEqualTo(oldToken);

        ResponseEntity<String> me = rest.exchange(
                "/admin/api/auth/me", HttpMethod.GET, withToken(newToken), String.class);
        assertThat(me.getStatusCode().value()).isEqualTo(200);
    }

    @Test
    @Order(9)
    void fifthFailureLocksAccountForTenMinutes() {
        resetLoginState();

        for (int i = 1; i <= 5; i++) {
            ResponseEntity<String> response = login("wrong-password");
            assertThat(response.getStatusCode().value()).isEqualTo(401);
        }

        Integer failedAttempts = jdbc.queryForObject(
                "SELECT failed_attempts FROM admin_user WHERE username = 'admin'", Integer.class);
        assertThat(failedAttempts).isEqualTo(5);

        java.sql.Timestamp lockedUntil = jdbc.queryForObject(
                "SELECT locked_until FROM admin_user WHERE username = 'admin'", java.sql.Timestamp.class);
        assertThat(lockedUntil).isAfter(
                java.sql.Timestamp.from(Instant.now().plusSeconds(9 * 60)));
    }

    @Test
    @Order(10)
    void lockedAccountRejectsEvenCorrectPassword() {
        ResponseEntity<String> response = login(ADMIN_PASSWORD);

        assertThat(response.getStatusCode().value()).isEqualTo(403);
        assertThat(json(response).path("message").asText()).contains("锁定");
    }

    @Test
    @Order(11)
    void successAfterLockExpiryClearsCounters() throws Exception {
        jdbc.update("UPDATE admin_user SET locked_until = NULL WHERE username = 'admin'");

        ResponseEntity<String> response = login(ADMIN_PASSWORD);

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        String token = json(response).path("data").path("token").asText();
        assertThat(token).isNotBlank();

        Map<String, Object> state = jdbc.queryForMap(
                "SELECT failed_attempts, locked_until FROM admin_user WHERE username = 'admin'");
        assertThat((Integer) state.get("failed_attempts")).isZero();
        assertThat(state.get("locked_until")).isNull();
    }

    @Test
    @Order(12)
    void wrongUsernameReturns401() {
        ResponseEntity<String> response = rest.postForEntity(
                "/admin/api/auth/login", body("nobody", ADMIN_PASSWORD), String.class);

        assertThat(response.getStatusCode().value()).isEqualTo(401);
    }

    private void resetLoginState() {
        jdbc.update("UPDATE admin_user SET failed_attempts = 0, locked_until = NULL WHERE username = 'admin'");
    }

    private ResponseEntity<String> login(String password) {
        return rest.postForEntity("/admin/api/auth/login", body("admin", password), String.class);
    }

    private HttpEntity<String> body(String username, String password) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        return new HttpEntity<>("{\"username\":\"" + username + "\",\"password\":\"" + password + "\"}", headers);
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
