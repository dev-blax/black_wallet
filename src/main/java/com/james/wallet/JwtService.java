package com.james.wallet;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.Map;

@Service
public class JwtService {

    private final SecretKey key;
    private final Duration ttl;

    public JwtService(@Value("${wallet.jwt.secret}") String secret,
                      @Value("${wallet.jwt.ttl-seconds}") long ttlSeconds) {
        byte[] keyBytes = secret.getBytes(StandardCharsets.UTF_8);
        if (keyBytes.length < 32) {
            throw new IllegalStateException("wallet.jwt.secret must be at least 32 bytes (got " + keyBytes.length + ")");
        }
        this.key = new SecretKeySpec(keyBytes, "HmacSHA256");
        this.ttl = Duration.ofSeconds(ttlSeconds);
    }

    public String issue(Long userId, String email) {
        Instant now = Instant.now();
        return Jwts.builder()
                .subject(String.valueOf(userId))
                .claims(Map.of("email", email))
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plus(ttl)))
                .signWith(key)
                .compact();
    }

    public Claims parse(String token) {
        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
