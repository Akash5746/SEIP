package com.seip.gateway.filter;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.UUID;

/**
 * Global logging / tracing filter.
 * <p>
 * Assigns a unique {@code X-Correlation-Id} header to every request (if not already
 * present) and logs request / response metadata for distributed tracing purposes.
 * Runs at order -90 (after the JWT filter at -100).
 */
@Slf4j
@Component
public class RequestLoggingFilter implements GlobalFilter, Ordered {

    private static final String CORRELATION_ID_HEADER = "X-Correlation-Id";

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();

        // Reuse correlation id if already set by caller, otherwise generate one
        String correlationId = request.getHeaders().getFirst(CORRELATION_ID_HEADER);
        if (correlationId == null || correlationId.isBlank()) {
            correlationId = UUID.randomUUID().toString();
        }

        final String finalCorrelationId = correlationId;

        // Store correlation id in exchange attributes so other filters could use it if needed
        exchange.getAttributes().put(CORRELATION_ID_HEADER, finalCorrelationId);

        long startTime = System.currentTimeMillis();

        log.info("GATEWAY IN  | correlationId={} | method={} | path={} | remoteAddr={}",
                finalCorrelationId,
                request.getMethod(),
                request.getURI().getPath(),
                request.getRemoteAddress());

        return chain.filter(exchange)
                .doFinally(signalType -> {
                    ServerHttpResponse response = exchange.getResponse();
                    long elapsed = System.currentTimeMillis() - startTime;
                    log.info("GATEWAY OUT | correlationId={} | status={} | elapsed={}ms",
                            finalCorrelationId,
                            response.getStatusCode(),
                            elapsed);
                });
    }

    @Override
    public int getOrder() {
        return -90;
    }
}
