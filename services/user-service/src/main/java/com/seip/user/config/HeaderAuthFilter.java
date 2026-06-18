package com.seip.user.config;

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
import java.util.Collections;
import java.util.List;

@Slf4j
@Component
public class HeaderAuthFilter extends OncePerRequestFilter {

    public static final String HEADER_USER_ID   = "X-Auth-User-Id";
    public static final String HEADER_USERNAME  = "X-Auth-Username";
    public static final String HEADER_USER_ROLE = "X-Auth-User-Role";

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        String userId   = request.getHeader(HEADER_USER_ID);
        String username = request.getHeader(HEADER_USERNAME);
        String role     = request.getHeader(HEADER_USER_ROLE);

        if (StringUtils.hasText(userId) && StringUtils.hasText(username)) {
            List<SimpleGrantedAuthority> authorities = StringUtils.hasText(role)
                    ? Collections.singletonList(new SimpleGrantedAuthority("ROLE_" + role.toUpperCase()))
                    : Collections.emptyList();

            UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(username, null, authorities);

            authentication.setDetails(userId);
            SecurityContextHolder.getContext().setAuthentication(authentication);

            log.debug("Authenticated user '{}' with role '{}' from gateway headers", username, role);
        } else {
            log.debug("No gateway auth headers present for request: {}", request.getRequestURI());
        }

        filterChain.doFilter(request, response);
    }
}
