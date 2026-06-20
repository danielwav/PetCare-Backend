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

						.requestMatchers("/api/usuarios/**").hasAuthority("ROLE_ADMIN")

						.requestMatchers("/api/asistentes/**").hasAuthority("ROLE_ADMIN")

						.requestMatchers(HttpMethod.POST,   "/api/veterinarios/**").hasAuthority("ROLE_ADMIN")
						.requestMatchers(HttpMethod.GET,    "/api/veterinarios/**").hasAnyAuthority("ROLE_ADMIN", "ROLE_ASISTENTE", "ROLE_VETERINARIO")
						.requestMatchers(HttpMethod.PUT,    "/api/veterinarios/**").hasAuthority("ROLE_ADMIN")
						.requestMatchers(HttpMethod.DELETE, "/api/veterinarios/**").hasAuthority("ROLE_ADMIN")

						.requestMatchers(HttpMethod.GET,    "/api/duenios/**").hasAnyAuthority("ROLE_ADMIN", "ROLE_ASISTENTE", "ROLE_VETERINARIO", "ROLE_DUENIO")
						.requestMatchers(HttpMethod.POST,   "/api/duenios/**").hasAnyAuthority("ROLE_ADMIN", "ROLE_ASISTENTE")
						.requestMatchers(HttpMethod.PUT,    "/api/duenios/**").hasAnyAuthority("ROLE_ADMIN", "ROLE_ASISTENTE", "ROLE_DUENIO")
						.requestMatchers(HttpMethod.DELETE, "/api/duenios/**").hasAnyAuthority("ROLE_ADMIN", "ROLE_ASISTENTE")

						.requestMatchers(HttpMethod.POST,   "/api/mascotas/**").authenticated()
						.requestMatchers(HttpMethod.GET,    "/api/mascotas/**").authenticated()
						.requestMatchers(HttpMethod.PUT,    "/api/mascotas/**").authenticated()
						.requestMatchers(HttpMethod.DELETE, "/api/mascotas/**").authenticated()

						.requestMatchers("/api/citas/**").authenticated()

						.requestMatchers("/api/alertas/**").hasAnyAuthority("ROLE_ADMIN", "ROLE_ASISTENTE", "ROLE_VETERINARIO")

						.requestMatchers("/api/vacunas/**").hasAnyAuthority("ROLE_ADMIN", "ROLE_ASISTENTE", "ROLE_VETERINARIO", "ROLE_DUENIO")

						.requestMatchers("/api/servicios/**").hasAnyAuthority("ROLE_ADMIN", "ROLE_ASISTENTE", "ROLE_VETERINARIO")

						.requestMatchers("/api/atencion-clinica/**").hasAnyAuthority("ROLE_ADMIN", "ROLE_ASISTENTE", "ROLE_VETERINARIO", "ROLE_DUENIO")

						.requestMatchers("/api/controles-mensuales/**").hasAnyAuthority("ROLE_ADMIN", "ROLE_ASISTENTE", "ROLE_VETERINARIO")

						.requestMatchers("/api/inasistencias/**").hasAnyAuthority("ROLE_ADMIN", "ROLE_ASISTENTE", "ROLE_VETERINARIO")

						.requestMatchers("/api/reportes/**").hasAnyAuthority("ROLE_ADMIN", "ROLE_ASISTENTE", "ROLE_VETERINARIO")

						.requestMatchers("/api/horarios-semanales/**").hasAnyAuthority("ROLE_ADMIN", "ROLE_ASISTENTE", "ROLE_VETERINARIO")

						.requestMatchers("/api/notas-seguimiento/**").hasAnyAuthority("ROLE_ADMIN", "ROLE_ASISTENTE", "ROLE_VETERINARIO")

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