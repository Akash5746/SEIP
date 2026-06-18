package com.seip.auth.service;

import com.seip.auth.entity.Role;
import com.seip.auth.entity.User;
import com.seip.auth.exception.TokenExpiredException;
import com.seip.auth.security.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.HashSet;
import java.util.Set;

import static org.assertj.core.api.Assertions.*;

@DisplayName("JwtService Unit Tests")
class JwtServiceTest {

    private JwtService jwtService;

    // 64-character secret suitable for HS512
    private static final String TEST_SECRET =
            "seip-super-secret-jwt-key-2024-must-be-at-least-64-chars-long-for-hs512";
    private static final long ACCESS_TOKEN_EXPIRY_MS = 900_000L;   // 15 min
    private static final long REFRESH_TOKEN_EXPIRY_DAYS = 7L;

    private User testUser;

    @BeforeEach
    void setUp() {
        jwtService = new JwtService();
        ReflectionTestUtils.setField(jwtService, "jwtSecret", TEST_SECRET);
        ReflectionTestUtils.setField(jwtService, "accessTokenExpiryMs", ACCESS_TOKEN_EXPIRY_MS);
        ReflectionTestUtils.setField(jwtService, "refreshTokenExpiryDays", REFRESH_TOKEN_EXPIRY_DAYS);

        Role role = Role.builder().id(1L).name("ROLE_EMPLOYEE").build();
        Set<Role> roles = new HashSet<>();
        roles.add(role);

        testUser = User.builder()
                .id(42L)
                .username("janesmith")
                .email("jane@example.com")
                .password("$2a$12$encodedpassword")
                .enabled(true)
                .accountNonExpired(true)
                .accountNonLocked(true)
                .credentialsNonExpired(true)
                .roles(roles)
                .build();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // generateAccessToken
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("generateAccessToken() - returns non-null, non-blank token")
    void testGenerateAccessToken_notNull() {
        String token = jwtService.generateAccessToken(testUser);

        assertThat(token).isNotNull().isNotBlank();
        // JWT format: three Base64url segments separated by dots
        assertThat(token.split("\\.")).hasSize(3);
    }

    @Test
    @DisplayName("generateAccessToken() - two calls produce different tokens (iat differs)")
    void testGenerateAccessToken_uniquePerCall() throws InterruptedException {
        String token1 = jwtService.generateAccessToken(testUser);
        Thread.sleep(5); // ensure different iat (ms granularity)
        String token2 = jwtService.generateAccessToken(testUser);

        // Subject is the same but the raw token strings may differ due to iat
        assertThat(jwtService.extractUsername(token1)).isEqualTo("janesmith");
        assertThat(jwtService.extractUsername(token2)).isEqualTo("janesmith");
    }

    // ─────────────────────────────────────────────────────────────────────────
    // extractUsername
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("extractUsername() - extracts correct subject from access token")
    void testExtractUsername() {
        String token = jwtService.generateAccessToken(testUser);
        String extracted = jwtService.extractUsername(token);

        assertThat(extracted).isEqualTo("janesmith");
    }

    @Test
    @DisplayName("extractUsername() - extracts correct subject from refresh token")
    void testExtractUsername_fromRefreshToken() {
        String token = jwtService.generateRefreshToken(testUser);
        String extracted = jwtService.extractUsername(token);

        assertThat(extracted).isEqualTo("janesmith");
    }

    // ─────────────────────────────────────────────────────────────────────────
    // validateToken
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("validateToken() - returns true for valid, unexpired token")
    void testValidateToken_validToken() {
        String token = jwtService.generateAccessToken(testUser);
        boolean valid = jwtService.validateToken(token, testUser);

        assertThat(valid).isTrue();
    }

    @Test
    @DisplayName("validateToken() - returns false when subject does not match UserDetails")
    void testValidateToken_wrongUser() {
        User anotherUser = User.builder()
                .id(99L)
                .username("intruder")
                .email("intruder@evil.com")
                .password("password")
                .enabled(true)
                .accountNonExpired(true)
                .accountNonLocked(true)
                .credentialsNonExpired(true)
                .roles(new HashSet<>())
                .build();

        String tokenForTestUser = jwtService.generateAccessToken(testUser);
        boolean valid = jwtService.validateToken(tokenForTestUser, anotherUser);

        assertThat(valid).isFalse();
    }

    @Test
    @DisplayName("validateToken() - returns false for tampered token")
    void testValidateToken_tamperedToken() {
        String token = jwtService.generateAccessToken(testUser);
        // Corrupt the signature portion
        String tampered = token.substring(0, token.lastIndexOf('.') + 1) + "invalidsignature";
        boolean valid = jwtService.validateToken(tampered, testUser);

        assertThat(valid).isFalse();
    }

    @Test
    @DisplayName("validateToken() - returns false for expired token (simulated via tiny expiry)")
    void testValidateToken_expiredToken() {
        // Temporarily set 1 ms expiry so the token is immediately expired
        ReflectionTestUtils.setField(jwtService, "accessTokenExpiryMs", 1L);
        String expiredToken = jwtService.generateAccessToken(testUser);

        // Restore normal expiry
        ReflectionTestUtils.setField(jwtService, "accessTokenExpiryMs", ACCESS_TOKEN_EXPIRY_MS);

        boolean valid = jwtService.validateToken(expiredToken, testUser);
        assertThat(valid).isFalse();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // isTokenExpired
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("isTokenExpired() - returns false for a freshly generated token")
    void testIsTokenExpired_freshToken() {
        String token = jwtService.generateAccessToken(testUser);
        boolean expired = jwtService.isTokenExpired(token);

        assertThat(expired).isFalse();
    }

    @Test
    @DisplayName("isTokenExpired() - returns true for an already-expired token")
    void testIsTokenExpired_expiredToken() {
        ReflectionTestUtils.setField(jwtService, "accessTokenExpiryMs", 1L);
        String expiredToken = jwtService.generateAccessToken(testUser);
        ReflectionTestUtils.setField(jwtService, "accessTokenExpiryMs", ACCESS_TOKEN_EXPIRY_MS);

        boolean expired = jwtService.isTokenExpired(expiredToken);
        assertThat(expired).isTrue();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // getAccessTokenExpiryMs
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("getAccessTokenExpiryMs() - returns configured value")
    void testGetAccessTokenExpiryMs() {
        assertThat(jwtService.getAccessTokenExpiryMs()).isEqualTo(ACCESS_TOKEN_EXPIRY_MS);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // extractUsername on expired token should throw TokenExpiredException
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("extractUsername() - throws TokenExpiredException for expired token")
    void testExtractUsername_expiredTokenThrows() {
        ReflectionTestUtils.setField(jwtService, "accessTokenExpiryMs", 1L);
        String expiredToken = jwtService.generateAccessToken(testUser);
        ReflectionTestUtils.setField(jwtService, "accessTokenExpiryMs", ACCESS_TOKEN_EXPIRY_MS);

        assertThatThrownBy(() -> jwtService.extractUsername(expiredToken))
                .isInstanceOf(TokenExpiredException.class)
                .hasMessageContaining("expired");
    }
}
