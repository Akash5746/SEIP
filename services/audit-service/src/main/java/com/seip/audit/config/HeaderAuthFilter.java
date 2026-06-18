package com.seip.audit.config;

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

@Slf4j
@Component
public class HeaderAuthFilter extends OncePerRequestFilter {

    static final String HEADER_USER_ID   = "X-User-Id";
    static final String HEADER_USER_ROLE = "X-User-Role";
    static final String HEADER_USERNAME  = "X-Username";

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        String userId   = request.getHeader(HEADER_USER_ID);
        String role     = request.getHeader(HEADER_USER_ROLE);
        String username = request.getHeader(HEADER_USERNAME);

        if (StringUtils.hasText(userId) && StringUtils.hasText(role)) {
            String authority = role.startsWith("ROLE_") ? role : "ROLE_" + role;
            List<SimpleGrantedAuthority> authorities = List.of(new SimpleGrantedAuthority(authority));

            UsernamePasswordAuthenticationToken auth =
                    new UsernamePasswordAuthenticationToken(username != null ? username : userId, null, authorities);

            SecurityContextHolder.getContext().setAuthentication(auth);
            log.debug("Authenticated via gateway headers: userId={}, role={}", userId, role);
        }

        filterChain.doFilter(request, response);
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        return path.startsWith("/actuator") || path.startsWith("/api-docs") || path.startsWith("/swagger-ui");
    }
}
