package com.seip.gateway.filter;

import com.seip.gateway.security.JwtUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * Global JWT authentication filter for Spring Cloud Gateway.
 * Runs before all route filters (order = -100).
 * <p>
 * - Skips open / public endpoints.
 * - Validates the Bearer JWT token on every other request.
 * - Injects X-Auth-* headers so downstream services can trust the caller identity.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter implements GlobalFilter, Ordered {

    private final JwtUtil jwtUtil;

    /**
     * Paths that do NOT require an Authorization header.
     * Matching is prefix-based (startsWith).
     */
    private static final List<String> OPEN_ENDPOINTS = List.of(
            "/auth/register",
            "/auth/login",
            "/auth/refresh",
            "/actuator",
            "/swagger-ui",
            "/api-docs",
            "/webjars"
    );

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String path = exchange.getRequest().getURI().getPath();
        log.debug("Gateway filter processing path: {}", path);

        // Pass open / public endpoints through without token check
        boolean isOpenEndpoint = OPEN_ENDPOINTS.stream().anyMatch(path::startsWith);
        if (isOpenEndpoint) {
            log.debug("Open endpoint detected, skipping JWT validation: {}", path);
            return chain.filter(exchange);
        }

        // Retrieve Authorization header
        String authHeader = exchange.getRequest().getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            log.warn("Missing or malformed Authorization header on path: {}", path);
            return onError(exchange, "Missing or invalid Authorization header", HttpStatus.UNAUTHORIZED);
        }

        String token = authHeader.substring(7);

        try {
            if (!jwtUtil.validateToken(token)) {
                log.warn("Invalid or expired JWT token on path: {}", path);
                return onError(exchange, "Invalid or expired token", HttpStatus.UNAUTHORIZED);
            }

            // Propagate authenticated user context to downstream microservices
            String userId   = jwtUtil.extractUserId(token);
            String username = jwtUtil.extractUsername(token);
            String role     = jwtUtil.extractRole(token);

            log.debug("JWT valid – userId={}, username={}, role={}", userId, username, role);

            ServerHttpRequest mutatedRequest = exchange.getRequest().mutate()
                    .header("X-Auth-User-Id",   userId)
                    .header("X-Auth-Username",  username)
                    .header("X-Auth-User-Role", role)
                    .build();

            return chain.filter(exchange.mutate().request(mutatedRequest).build());

        } catch (Exception e) {
            log.error("Token validation failed on path {}: {}", path, e.getMessage());
            return onError(exchange, "Token validation failed", HttpStatus.UNAUTHORIZED);
        }
    }

    /**
     * Writes a standardised JSON error response and completes the reactive stream.
     */
    private Mono<Void> onError(ServerWebExchange exchange, String message, HttpStatus status) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(status);
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);

        String body = String.format(
                "{\"success\":false,\"message\":\"%s\",\"data\":null,\"timestamp\":\"%s\"}",
                message, java.time.Instant.now().toString()
        );

        DataBuffer buffer = response.bufferFactory()
                .wrap(body.getBytes(StandardCharsets.UTF_8));

        return response.writeWith(Mono.just(buffer));
    }

    /** Run before all other gateway filters. */
    @Override
    public int getOrder() {
        return -100;
    }
}
