package com.seip.expense.security;

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
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Trusts X-Auth-* headers set by the API Gateway after JWT validation.
 * The gateway is responsible for authentication; this service just reads the pre-validated headers.
 */
@Slf4j
@Component
public class HeaderAuthFilter extends OncePerRequestFilter {

    public static final String HEADER_USER_ID = "X-Auth-User-Id";
    public static final String HEADER_USER_EMAIL = "X-Auth-User-Email";
    public static final String HEADER_USER_ROLES = "X-Auth-User-Roles";

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        String userId = request.getHeader(HEADER_USER_ID);
        String email = request.getHeader(HEADER_USER_EMAIL);
        String rolesHeader = request.getHeader(HEADER_USER_ROLES);

        if (StringUtils.hasText(userId)) {
            List<SimpleGrantedAuthority> authorities = Collections.emptyList();
            if (StringUtils.hasText(rolesHeader)) {
                authorities = Arrays.stream(rolesHeader.split(","))
                        .map(String::trim)
                        .filter(StringUtils::hasText)
                        .map(role -> new SimpleGrantedAuthority("ROLE_" + role))
                        .collect(Collectors.toList());
            }

            GatewayAuthenticationPrincipal principal = new GatewayAuthenticationPrincipal(
                    Long.parseLong(userId), email, rolesHeader);

            UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(principal, null, authorities);

            SecurityContextHolder.getContext().setAuthentication(authentication);
            log.debug("Authenticated user {} with roles {} from gateway headers", userId, rolesHeader);
        }

        filterChain.doFilter(request, response);
    }
}
