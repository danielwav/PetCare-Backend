package com.petcare.backend.config;

import com.petcare.backend.security.JwtAuthenticationFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@RequiredArgsConstructor
public class SecurityConfig {

	private final JwtAuthenticationFilter jwtAuthenticationFilter;

	@Bean
	SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
		return http
				.cors(cors -> {})
				.csrf(AbstractHttpConfigurer::disable)
				.sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
				.authorizeHttpRequests(auth -> auth
						.requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
						.requestMatchers(
								"/api/activate-account",
								"/api/health",
								"/swagger-ui.html",
								"/swagger-ui/**",
								"/v3/api-docs/**"
						).permitAll()

						.requestMatchers(
								"/api/auth/register",
								"/api/auth/register-clinic",
								"/api/auth/login",
								"/api/auth/refresh",
								"/api/auth/set-password",
								"/api/auth/activate-account",
								"/api/auth/activate/**"
						).permitAll()

						.requestMatchers(HttpMethod.GET, "/api/usuarios/**")
						.hasAnyAuthority("ROLE_ADMIN", "ROLE_VETERINARIO", "ROLE_ASISTENTE")
						.requestMatchers("/api/usuarios/**")
						.hasAuthority("ROLE_ADMIN")

						.requestMatchers("/api/reportes/**", "/api/inasistencias/**", "/api/alertas/dia")
						.hasAnyAuthority("ROLE_ADMIN", "ROLE_VETERINARIO", "ROLE_ASISTENTE")
						.requestMatchers("/api/controles-mensuales/**",
								"/api/mascotas/*/controles-mensuales",
								"/api/notas-seguimiento/**",
								"/api/horarios-semanales/**")
						.hasAnyAuthority("ROLE_ADMIN", "ROLE_VETERINARIO", "ROLE_ASISTENTE")
						.requestMatchers(HttpMethod.POST, "/api/citas/*/atencion")
						.hasAnyAuthority("ROLE_ADMIN", "ROLE_VETERINARIO")
						.requestMatchers(HttpMethod.PATCH, "/api/citas/*/inasistencia")
						.hasAnyAuthority("ROLE_ADMIN", "ROLE_VETERINARIO", "ROLE_ASISTENTE")
						.requestMatchers(HttpMethod.PUT, "/api/citas/**")
						.hasAnyAuthority("ROLE_ADMIN", "ROLE_VETERINARIO", "ROLE_ASISTENTE")
						.requestMatchers(HttpMethod.GET, "/api/citas/alertas-confirmacion")
						.hasAnyAuthority("ROLE_ADMIN", "ROLE_VETERINARIO", "ROLE_ASISTENTE")
						.requestMatchers(HttpMethod.GET, "/api/atenciones/**")
						.hasAnyAuthority("ROLE_ADMIN", "ROLE_VETERINARIO", "ROLE_ASISTENTE", "ROLE_DUENIO")
						.requestMatchers(HttpMethod.POST, "/api/vacunas")
						.hasAnyAuthority("ROLE_ADMIN", "ROLE_VETERINARIO", "ROLE_ASISTENTE")
						.requestMatchers(HttpMethod.PUT, "/api/vacunas/**")
						.hasAnyAuthority("ROLE_ADMIN", "ROLE_VETERINARIO", "ROLE_ASISTENTE")
						.requestMatchers(HttpMethod.PATCH, "/api/vacunas/**")
						.hasAnyAuthority("ROLE_ADMIN", "ROLE_VETERINARIO", "ROLE_ASISTENTE")
						.requestMatchers(HttpMethod.DELETE, "/api/vacunas/**")
						.hasAnyAuthority("ROLE_ADMIN", "ROLE_VETERINARIO", "ROLE_ASISTENTE")

						.requestMatchers(HttpMethod.POST, "/api/duenios")
						.hasAnyAuthority("ROLE_ADMIN", "ROLE_VETERINARIO", "ROLE_ASISTENTE")
						.requestMatchers(HttpMethod.DELETE, "/api/duenios/**")
						.hasAnyAuthority("ROLE_ADMIN", "ROLE_VETERINARIO", "ROLE_ASISTENTE")

						.requestMatchers(HttpMethod.GET, "/api/clinicas/me").authenticated()
						.requestMatchers(HttpMethod.PUT, "/api/clinicas/me")
						.hasAuthority("ROLE_ADMIN")

						.anyRequest().authenticated())
				.addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
				.build();
	}

	@Bean
	AuthenticationManager authenticationManager(AuthenticationConfiguration authenticationConfiguration) throws Exception {
		return authenticationConfiguration.getAuthenticationManager();
	}

	@Bean
	PasswordEncoder passwordEncoder() {
		return new BCryptPasswordEncoder();
	}
}