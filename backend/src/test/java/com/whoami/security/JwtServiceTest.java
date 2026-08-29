package com.whoami.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.whoami.config.JwtProperties;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import javax.crypto.SecretKey;
import org.junit.jupiter.api.Test;

class JwtServiceTest {

    private static final String SECRET = "unit-test-jwt-secret-0123456789abcdef";
    private static final String OTHER_SECRET = "another-jwt-secret-0123456789abcdef";

    private final JwtService jwtService = new JwtService(new JwtProperties(SECRET, 7200));

    @Test
    void issueThenParseRoundTrip() {
        String token = jwtService.issue(42L);

        assertThat(jwtService.parseAdminId(token)).isEqualTo(42L);
    }

    @Test
    void expiredTokenIsRejected() {
        SecretKey key = Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8));
        String expired = Jwts.builder()
                .subject("1")
                .expiration(Date.from(Instant.now().minusSeconds(60)))
                .signWith(key)
                .compact();

        assertThatThrownBy(() -> jwtService.parseAdminId(expired)).isInstanceOf(JwtException.class);
    }

    @Test
    void tokenSignedWithOtherSecretIsRejected() {
        SecretKey key = Keys.hmacShaKeyFor(OTHER_SECRET.getBytes(StandardCharsets.UTF_8));
        String foreign = Jwts.builder()
                .subject("1")
                .expiration(Date.from(Instant.now().plusSeconds(3600)))
                .signWith(key)
                .compact();

        assertThatThrownBy(() -> jwtService.parseAdminId(foreign)).isInstanceOf(JwtException.class);
    }

    @Test
    void shortSecretIsRejectedAtStartup() {
        assertThatThrownBy(() -> new JwtService(new JwtProperties("too-short", 7200)))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void expiresInIsClampedToTwoHours() {
        JwtService service = new JwtService(new JwtProperties(SECRET, 99999));

        assertThat(service.expiresIn()).isEqualTo(7200);
    }
}
