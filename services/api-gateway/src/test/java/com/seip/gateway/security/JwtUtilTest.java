package com.seip.gateway.security;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link JwtUtil}.
 * Uses a real JJWT-generated token to exercise every public method.
 */
class JwtUtilTest {

    private static final String SECRET =
            "test-secret-key-that-is-at-least-64-characters-long-for-hs512-algo";

    /**
     * A pre-built token signed with the test secret above:
     * <ul>
     *   <li>subject  = "alice"</li>
     *   <li>userId   = "42"</li>
     *   <li>role     = "ROLE_EMPLOYEE"</li>
     *   <li>expiry   = year 2099 (won't expire during test runs)</li>
     * </ul>
     */
    private static final String VALID_TOKEN = buildToken();

    private JwtUtil jwtUtil;

    @BeforeEach
    void setUp() {
        jwtUtil = new JwtUtil();
        ReflectionTestUtils.setField(jwtUtil, "secret", SECRET);
    }

    // ── extractUsername ──────────────────────────────────────────────────────

    @Test
    @DisplayName("extractUsername returns the subject from a valid token")
    void extractUsername_validToken_returnsSubject() {
        assertThat(jwtUtil.extractUsername(VALID_TOKEN)).isEqualTo("alice");
    }

    // ── extractUserId ────────────────────────────────────────────────────────

    @Test
    @DisplayName("extractUserId returns the userId claim from a valid token")
    void extractUserId_validToken_returnsUserId() {
        assertThat(jwtUtil.extractUserId(VALID_TOKEN)).isEqualTo("42");
    }

    // ── extractRole ──────────────────────────────────────────────────────────

    @Test
    @DisplayName("extractRole returns the role claim from a valid token")
    void extractRole_validToken_returnsRole() {
        assertThat(jwtUtil.extractRole(VALID_TOKEN)).isEqualTo("ROLE_EMPLOYEE");
    }

    // ── isTokenExpired ───────────────────────────────────────────────────────

    @Test
    @DisplayName("isTokenExpired returns false for a far-future expiry token")
    void isTokenExpired_futureDateToken_returnsFalse() {
        assertThat(jwtUtil.isTokenExpired(VALID_TOKEN)).isFalse();
    }

    // ── validateToken ────────────────────────────────────────────────────────

    @Test
    @DisplayName("validateToken returns true for a well-formed, non-expired token")
    void validateToken_validToken_returnsTrue() {
        assertThat(jwtUtil.validateToken(VALID_TOKEN)).isTrue();
    }

    @Test
    @DisplayName("validateToken returns false for a clearly invalid string")
    void validateToken_junkString_returnsFalse() {
        assertThat(jwtUtil.validateToken("not.a.jwt")).isFalse();
    }

    @Test
    @DisplayName("validateToken returns false for an empty string")
    void validateToken_emptyString_returnsFalse() {
        assertThat(jwtUtil.validateToken("")).isFalse();
    }

    // ── Token builder helper ─────────────────────────────────────────────────

    private static String buildToken() {
        SecretKey signingKey = Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8));
        return Jwts.builder()
                .subject("alice")
                .claim("userId", "42")
                .claim("role", "ROLE_EMPLOYEE")
                .expiration(new Date(Instant.parse("2099-01-01T00:00:00Z").toEpochMilli()))
                .signWith(signingKey)
                .compact();
    }
}
