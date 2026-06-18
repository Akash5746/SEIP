package com.seip.gateway.exception;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.web.reactive.error.ErrorWebExceptionHandler;
import org.springframework.cloud.gateway.support.NotFoundException;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

/**
 * Global exception handler for the reactive API Gateway.
 * <p>
 * Catches all unhandled exceptions that escape the filter chain and
 * converts them into a standardised JSON error response matching the
 * {@code ApiResponse<T>} contract used across the platform.
 * <p>
 * Order(-2) ensures it runs before the default Spring Boot error handler
 * (order = -1) but after the gateway's own internal handler.
 */
@Slf4j
@Order(-2)
@Component
@RequiredArgsConstructor
public class GlobalErrorHandler implements ErrorWebExceptionHandler {

    private final ObjectMapper objectMapper;

    @Override
    public Mono<Void> handle(ServerWebExchange exchange, Throwable ex) {
        ServerHttpResponse response = exchange.getResponse();

        if (response.isCommitted()) {
            return Mono.error(ex);
        }

        HttpStatus status = resolveStatus(ex);
        String message    = resolveMessage(ex, status);

        java.io.StringWriter sw = new java.io.StringWriter();
        ex.printStackTrace(new java.io.PrintWriter(sw));
        String stackTrace = sw.toString();

        log.error("Gateway error [{}] on path '{}'. Exception Class: {}, Message: {}, StackTrace: \n{}",
                status.value(),
                exchange.getRequest().getURI().getPath(),
                ex.getClass().getName(),
                ex.getMessage(),
                stackTrace);

        response.setStatusCode(status);
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);

        Map<String, Object> body = new HashMap<>();
        body.put("success",   false);
        body.put("message",   message);
        body.put("data",      null);
        body.put("timestamp", Instant.now().toString());
        body.put("path",      exchange.getRequest().getURI().getPath());
        body.put("status",    status.value());

        byte[] bytes;
        try {
            bytes = objectMapper.writeValueAsBytes(body);
        } catch (JsonProcessingException jpe) {
            log.error("Failed to serialise error body", jpe);
            bytes = ("{\"success\":false,\"message\":\"Internal serialisation error\"}")
                    .getBytes(StandardCharsets.UTF_8);
        }

        DataBuffer buffer = response.bufferFactory().wrap(bytes);
        return response.writeWith(Mono.just(buffer));
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private HttpStatus resolveStatus(Throwable ex) {
        if (ex instanceof ResponseStatusException rse) {
            return HttpStatus.resolve(rse.getStatusCode().value()) != null
                    ? HttpStatus.resolve(rse.getStatusCode().value())
                    : HttpStatus.INTERNAL_SERVER_ERROR;
        }
        if (ex instanceof NotFoundException) {
            return HttpStatus.SERVICE_UNAVAILABLE;
        }
        return HttpStatus.INTERNAL_SERVER_ERROR;
    }

    private String resolveMessage(Throwable ex, HttpStatus status) {
        return switch (status) {
            case NOT_FOUND            -> "Requested resource not found";
            case SERVICE_UNAVAILABLE  -> "Downstream service is currently unavailable";
            case UNAUTHORIZED         -> "Authentication required";
            case FORBIDDEN            -> "Access denied";
            case BAD_REQUEST          -> "Bad request: " + sanitise(ex.getMessage());
            case GATEWAY_TIMEOUT      -> "Upstream service timed out";
            default                   -> "An unexpected error occurred";
        };
    }

    /** Strips potential PII / stack info from exception messages shown to clients. */
    private String sanitise(String raw) {
        if (raw == null) return "unknown";
        // Limit length and remove newlines to keep the JSON response clean
        return raw.replace("\n", " ").replace("\r", "").substring(0, Math.min(raw.length(), 200));
    }
}
