package com.seip.fraud.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

/**
 * Filter that reads X-Employee-Id and X-Employee-Role headers propagated by the API gateway
 * and establishes a lightweight security context. No JWT validation is performed here —
 * auth is handled upstream by the auth-service / gateway.
 */
@Slf4j
@Component
public class HeaderAuthFilter extends OncePerRequestFilter {

    private static final String EMPLOYEE_ID_HEADER  = "X-Employee-Id";
    private static final String EMPLOYEE_ROLE_HEADER = "X-Employee-Role";
    private static final String SERVICE_NAME_HEADER  = "X-Service-Name";

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        String employeeId  = request.getHeader(EMPLOYEE_ID_HEADER);
        String role        = request.getHeader(EMPLOYEE_ROLE_HEADER);
        String serviceName = request.getHeader(SERVICE_NAME_HEADER);

        // Allow internal service-to-service calls
        if (StringUtils.hasText(serviceName)) {
            log.debug("Internal service call from: {}", serviceName);
            var auth = new UsernamePasswordAuthenticationToken(
                    serviceName, null,
                    List.of(new SimpleGrantedAuthority("ROLE_SERVICE")));
            SecurityContextHolder.getContext().setAuthentication(auth);
            filterChain.doFilter(request, response);
            return;
        }

        if (StringUtils.hasText(employeeId)) {
            String grantedRole = StringUtils.hasText(role) ? "ROLE_" + role.toUpperCase() : "ROLE_USER";
            var auth = new UsernamePasswordAuthenticationToken(
                    employeeId, null,
                    List.of(new SimpleGrantedAuthority(grantedRole)));
            SecurityContextHolder.getContext().setAuthentication(auth);
            log.debug("Authenticated employee: id={}, role={}", employeeId, grantedRole);
        }

        filterChain.doFilter(request, response);
    }
}
