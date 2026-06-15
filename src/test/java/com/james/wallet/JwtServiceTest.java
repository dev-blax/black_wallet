package com.james.wallet;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.security.SignatureException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * UNIT test — the bottom of the pyramid.
 *
 * No Spring context, no database, no @SpringBootTest. We construct JwtService by hand,
 * so this runs in milliseconds. A true unit test exercises ONE class in isolation; the
 * only collaborators here are the jjwt library and a hand-made secret.
 */
class JwtServiceTest {

    // 48 ASCII chars = 48 bytes, comfortably over the 32-byte HS256 minimum.
    private static final String SECRET = "test-secret-test-secret-test-secret-test-secret!";

    private final JwtService jwt = new JwtService(SECRET, 3600);

    @Test
    void issuesTokenThatParsesBackToTheSameClaims() {
        String token = jwt.issue(42L, "alice@example.com");

        Claims claims = jwt.parse(token);

        assertThat(claims.getSubject()).isEqualTo("42");
        assertThat(claims.get("email", String.class)).isEqualTo("alice@example.com");
        assertThat(claims.getExpiration()).isAfter(claims.getIssuedAt());
    }

    @Test
    void rejectsTokenSignedWithADifferentSecret() {
        // A token minted by a DIFFERENT service (attacker forging with the wrong key)...
        JwtService attacker = new JwtService("another-secret-another-secret-32bytes-xx", 3600);
        String forged = attacker.issue(1L, "mallory@example.com");

        // ...must fail signature verification on our service.
        assertThatThrownBy(() -> jwt.parse(forged))
                .isInstanceOf(SignatureException.class);
    }

    @Test
    void rejectsTamperedToken() {
        String token = jwt.issue(7L, "bob@example.com");
        String tampered = token.substring(0, token.length() - 2) + "xx";

        assertThatThrownBy(() -> jwt.parse(tampered))
                .isInstanceOf(RuntimeException.class); // signature / malformed
    }

    @Test
    void rejectsAlreadyExpiredToken() throws InterruptedException {
        JwtService shortLived = new JwtService(SECRET, 1); // 1-second TTL
        String token = shortLived.issue(99L, "carol@example.com");

        Thread.sleep(1100); // let it expire

        assertThatThrownBy(() -> shortLived.parse(token))
                .isInstanceOf(io.jsonwebtoken.ExpiredJwtException.class);
    }

    @Test
    void refusesToStartWithAShortSecret() {
        // HS256 requires a key of at least 256 bits (32 bytes). Fail fast at construction.
        assertThatThrownBy(() -> new JwtService("too-short", 3600))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("at least 32 bytes");
    }
}
