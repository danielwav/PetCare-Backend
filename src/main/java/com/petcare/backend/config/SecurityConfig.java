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
								"/api/auth/**",
								"/api/activate-account",
								"/api/health",
								"/swagger-ui.html",
								"/swagger-ui/**",
								"/v3/api-docs/**"
						).permitAll()

						/* === ADMIN only === */
						.requestMatchers(HttpMethod.POST,   "/api/usuarios/**").hasRole("ADMIN")
						.requestMatchers(HttpMethod.GET,    "/api/usuarios/**").hasRole("ADMIN")
						.requestMatchers(HttpMethod.PUT,    "/api/usuarios/**").hasRole("ADMIN")
						.requestMatchers(HttpMethod.DELETE, "/api/usuarios/**").hasRole("ADMIN")

						.requestMatchers(HttpMethod.POST,   "/api/asistentes/**").hasRole("ADMIN")
						.requestMatchers(HttpMethod.GET,    "/api/asistentes/**").hasRole("ADMIN")
						.requestMatchers(HttpMethod.PUT,    "/api/asistentes/**").hasRole("ADMIN")
						.requestMatchers(HttpMethod.DELETE, "/api/asistentes/**").hasRole("ADMIN")

						.requestMatchers(HttpMethod.POST,   "/api/veterinarios/**").hasRole("ADMIN")
						.requestMatchers(HttpMethod.GET,    "/api/veterinarios/**").hasAnyRole("ADMIN", "ASISTENTE", "VETERINARIO")
						.requestMatchers(HttpMethod.PUT,    "/api/veterinarios/**").hasRole("ADMIN")
						.requestMatchers(HttpMethod.DELETE, "/api/veterinarios/**").hasRole("ADMIN")

						.requestMatchers(HttpMethod.GET,    "/api/duenios/**").hasAnyRole("ADMIN", "ASISTENTE", "VETERINARIO", "DUENIO")
						.requestMatchers(HttpMethod.POST,   "/api/duenios/**").hasAnyRole("ADMIN", "ASISTENTE")
						.requestMatchers(HttpMethod.PUT,    "/api/duenios/**").hasAnyRole("ADMIN", "ASISTENTE", "DUENIO")
						.requestMatchers(HttpMethod.DELETE, "/api/duenios/**").hasAnyRole("ADMIN", "ASISTENTE")

						.requestMatchers(HttpMethod.POST,   "/api/mascotas/**").hasAnyRole("ADMIN", "ASISTENTE", "DUENIO")
						.requestMatchers(HttpMethod.GET,    "/api/mascotas/**").hasAnyRole("ADMIN", "ASISTENTE", "VETERINARIO", "DUENIO")
						.requestMatchers(HttpMethod.PUT,    "/api/mascotas/**").hasAnyRole("ADMIN", "ASISTENTE", "DUENIO")
						.requestMatchers(HttpMethod.DELETE, "/api/mascotas/**").hasAnyRole("ADMIN", "ASISTENTE", "DUENIO")

						.requestMatchers("/api/citas/**").hasAnyRole("ADMIN", "ASISTENTE", "VETERINARIO", "DUENIO")

						.requestMatchers("/api/alertas/**").hasAnyRole("ADMIN", "ASISTENTE", "VETERINARIO")

						.requestMatchers("/api/vacunas/**").hasAnyRole("ADMIN", "ASISTENTE", "VETERINARIO", "DUENIO")

						.requestMatchers("/api/servicios/**").hasAnyRole("ADMIN", "ASISTENTE", "VETERINARIO")

						.requestMatchers("/api/atencion-clinica/**").hasAnyRole("ADMIN", "ASISTENTE", "VETERINARIO", "DUENIO")

						.requestMatchers("/api/controles-mensuales/**").hasAnyRole("ADMIN", "ASISTENTE", "VETERINARIO")

						.requestMatchers("/api/inasistencias/**").hasAnyRole("ADMIN", "ASISTENTE", "VETERINARIO")

						.requestMatchers("/api/reportes/**").hasAnyRole("ADMIN", "ASISTENTE", "VETERINARIO")

						.requestMatchers("/api/horarios-semanales/**").hasAnyRole("ADMIN", "ASISTENTE", "VETERINARIO")

						.requestMatchers("/api/notas-seguimiento/**").hasAnyRole("ADMIN", "ASISTENTE", "VETERINARIO")

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
