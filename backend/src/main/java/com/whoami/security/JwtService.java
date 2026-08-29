package com.whoami.security;

import com.whoami.config.JwtProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import javax.crypto.SecretKey;
import org.springframework.stereotype.Component;

@Component
public class JwtService {

    public static final int MAX_EXPIRES_IN_SECONDS = 7200;

    private final SecretKey key;
    private final long expiresInSeconds;

    public JwtService(JwtProperties properties) {
        String secret = properties.secret();
        if (secret == null || secret.getBytes(StandardCharsets.UTF_8).length < 32) {
            throw new IllegalStateException("JWT_SECRET 未配置或长度不足 32 字符");
        }
        Integer configured = properties.expiresInSeconds();
        this.expiresInSeconds = configured == null || configured <= 0
                ? MAX_EXPIRES_IN_SECONDS
                : Math.min(configured, MAX_EXPIRES_IN_SECONDS);
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    public String issue(long adminId) {
        Instant now = Instant.now();
        return Jwts.builder()
                .subject(String.valueOf(adminId))
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plusSeconds(expiresInSeconds)))
                .signWith(key)
                .compact();
    }

    public long expiresIn() {
        return expiresInSeconds;
    }

    public long parseAdminId(String token) {
        Claims claims = Jwts.parser().verifyWith(key).build()
                .parseSignedClaims(token)
                .getPayload();
        return Long.parseLong(claims.getSubject());
    }
}
