package com.petcare.backend.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

	private static final String BEARER_PREFIX = "Bearer ";

	private final JwtService jwtService;
	private final JwtIdentityService jwtIdentityService;

	@Override
	protected void doFilterInternal(
			@NonNull HttpServletRequest request,
			@NonNull HttpServletResponse response,
			@NonNull FilterChain filterChain
	) throws ServletException, IOException {
		String token = resolveToken(request);

		if (token != null && jwtService.isValidAccessToken(token) && SecurityContextHolder.getContext().getAuthentication() == null) {
			try {
				var usuario = jwtIdentityService.resolve(token);
				var userDetails = User.withUsername(usuario.getEmail()).password(usuario.getPassword())
						.credentialsExpired(Boolean.TRUE.equals(usuario.getForcePasswordChange()))
						.authorities(usuario.getRoles().stream().filter(role -> Boolean.TRUE.equals(role.getActive()))
								.map(role -> role.getName().name()).toArray(String[]::new))
						.build();
				var authentication = new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
				authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
				SecurityContextHolder.getContext().setAuthentication(authentication);
			} catch (AccessDeniedException e) {
				SecurityContextHolder.clearContext();
				response.sendError(HttpServletResponse.SC_FORBIDDEN);
				return;
			} catch (AuthenticationException | io.jsonwebtoken.JwtException | IllegalArgumentException e) {
				SecurityContextHolder.clearContext();
			}
		}

		var authentication = SecurityContextHolder.getContext().getAuthentication();
		if (authentication != null && authentication.getPrincipal() instanceof org.springframework.security.core.userdetails.UserDetails details
				&& !details.isCredentialsNonExpired()) {
			String path = request.getRequestURI().substring(request.getContextPath().length());
			boolean accountRequest = (request.getMethod().equals("GET") && path.equals("/api/auth/me"))
					|| (request.getMethod().equals("POST") && java.util.Set.of("/api/auth/login", "/api/auth/refresh",
							"/api/auth/change-password", "/api/auth/logout").contains(path));
			if (!accountRequest && !request.getMethod().equals("OPTIONS")) {
				response.sendError(HttpServletResponse.SC_FORBIDDEN);
				return;
			}
		}

		filterChain.doFilter(request, response);
	}

	private String resolveToken(HttpServletRequest request) {
		String authorizationHeader = request.getHeader("Authorization");

		if (authorizationHeader == null || !authorizationHeader.startsWith(BEARER_PREFIX)) {
			return null;
		}

		return authorizationHeader.substring(BEARER_PREFIX.length());
	}
}
