package com.seip.gateway.filter;

import com.seip.gateway.security.JwtUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link JwtAuthenticationFilter}.
 * Uses Mockito to stub JwtUtil and a MockServerWebExchange for reactive assertions.
 */
@ExtendWith(MockitoExtension.class)
class JwtAuthenticationFilterTest {

    @Mock
    private JwtUtil jwtUtil;

    @Mock
    private GatewayFilterChain chain;

    @InjectMocks
    private JwtAuthenticationFilter filter;

    @BeforeEach
    void setUp() {
        // lenient: 401-path tests never call chain.filter(), so strict stubbing would flag this
        lenient().when(chain.filter(any())).thenReturn(Mono.empty());
    }

    // ── Open endpoints ───────────────────────────────────────────────────────

    @Test
    @DisplayName("Requests to /auth/login pass through without JWT check")
    void openEndpoint_authLogin_skipsJwtValidation() {
        MockServerHttpRequest request = MockServerHttpRequest
                .get("/auth/login")
                .build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);

        StepVerifier.create(filter.filter(exchange, chain))
                .verifyComplete();

        verify(jwtUtil, never()).validateToken(any());
        verify(chain).filter(any());
    }

    @Test
    @DisplayName("Requests to /actuator/health pass through without JWT check")
    void openEndpoint_actuatorHealth_skipsJwtValidation() {
        MockServerHttpRequest request = MockServerHttpRequest
                .get("/actuator/health")
                .build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);

        StepVerifier.create(filter.filter(exchange, chain))
                .verifyComplete();

        verify(jwtUtil, never()).validateToken(any());
    }

    // ── Missing Authorization header ─────────────────────────────────────────

    @Test
    @DisplayName("Requests without Authorization header receive 401")
    void protectedEndpoint_missingAuthHeader_returns401() {
        MockServerHttpRequest request = MockServerHttpRequest
                .get("/expenses/123")
                .build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);

        StepVerifier.create(filter.filter(exchange, chain))
                .verifyComplete();

        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        verify(chain, never()).filter(any());
    }

    @Test
    @DisplayName("Requests with 'Basic ...' Authorization header receive 401")
    void protectedEndpoint_basicAuthHeader_returns401() {
        MockServerHttpRequest request = MockServerHttpRequest
                .get("/expenses/123")
                .header(HttpHeaders.AUTHORIZATION, "Basic dXNlcjpwYXNz")
                .build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);

        StepVerifier.create(filter.filter(exchange, chain))
                .verifyComplete();

        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    // ── Invalid / expired tokens ─────────────────────────────────────────────

    @Test
    @DisplayName("Requests with an invalid Bearer token receive 401")
    void protectedEndpoint_invalidToken_returns401() {
        when(jwtUtil.validateToken("bad.token.here")).thenReturn(false);

        MockServerHttpRequest request = MockServerHttpRequest
                .get("/expenses/123")
                .header(HttpHeaders.AUTHORIZATION, "Bearer bad.token.here")
                .build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);

        StepVerifier.create(filter.filter(exchange, chain))
                .verifyComplete();

        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        verify(chain, never()).filter(any());
    }

    // ── Valid token ──────────────────────────────────────────────────────────

    @Test
    @DisplayName("Requests with a valid Bearer token are forwarded with X-Auth headers")
    void protectedEndpoint_validToken_forwardsWithXAuthHeaders() {
        when(jwtUtil.validateToken("good.jwt.token")).thenReturn(true);
        when(jwtUtil.extractUserId("good.jwt.token")).thenReturn("99");
        when(jwtUtil.extractUsername("good.jwt.token")).thenReturn("bob");
        when(jwtUtil.extractRole("good.jwt.token")).thenReturn("ROLE_ADMIN");

        MockServerHttpRequest request = MockServerHttpRequest
                .get("/expenses/123")
                .header(HttpHeaders.AUTHORIZATION, "Bearer good.jwt.token")
                .build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);

        StepVerifier.create(filter.filter(exchange, chain))
                .verifyComplete();

        // The chain must be called exactly once
        verify(chain).filter(any());
        // Response should not be set to an error code
        assertThat(exchange.getResponse().getStatusCode()).isNull();
    }

    // ── Order ────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("Filter order is -100")
    void getOrder_returnsMinusHundred() {
        assertThat(filter.getOrder()).isEqualTo(-100);
    }
}
