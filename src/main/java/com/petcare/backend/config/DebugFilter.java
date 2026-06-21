package com.petcare.backend.config;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.Order;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
@Order(Integer.MIN_VALUE)
public class DebugFilter implements Filter {

    private static final Logger log = LoggerFactory.getLogger(DebugFilter.class);

    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain) throws IOException, ServletException {
        HttpServletRequest req = (HttpServletRequest) servletRequest;
        String path = req.getRequestURI();

        if (path.startsWith("/api/")) {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth != null) {
                log.info("=== DEBUG === Path: {} | Auth: {} | Authorities: {} | Authenticated: {}",
                        path, auth.getName(), auth.getAuthorities(), auth.isAuthenticated());
            } else {
                log.info("=== DEBUG === Path: {} | NO AUTHENTICATION", path);
            }
        }

        filterChain.doFilter(servletRequest, servletResponse);

        if (path.startsWith("/api/")) {
            int status = ((HttpServletResponse) servletResponse).getStatus();
            if (status == 403) {
                Authentication auth = SecurityContextHolder.getContext().getAuthentication();
                if (auth != null) {
                    log.info("=== DEBUG 403 === Path: {} | Auth: {} | Authorities: {} | Authenticated: {}",
                            path, auth.getName(), auth.getAuthorities(), auth.isAuthenticated());
                } else {
                    log.info("=== DEBUG 403 === Path: {} | NO AUTHENTICATION", path);
                }
            }
        }
    }
}
