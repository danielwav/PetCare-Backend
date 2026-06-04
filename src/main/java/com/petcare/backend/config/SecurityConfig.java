package com.petcare.backend.config;

import com.petcare.backend.security.JwtAuthenticationFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@RequiredArgsConstructor
@EnableMethodSecurity
public class SecurityConfig {

	private final JwtAuthenticationFilter jwtAuthenticationFilter;

	@Bean
	SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
		return http
				.cors(cors -> {})
				.csrf(AbstractHttpConfigurer::disable)
				.sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
				.authorizeHttpRequests(auth -> auth
						.requestMatchers(
								"/api/auth/**",
								"/api/health",
								"/actuator/health",
								"/swagger-ui.html",
								"/swagger-ui/**",
								"/v3/api-docs/**"
						).permitAll()
						.requestMatchers(org.springframework.http.HttpMethod.POST, "/api/servicios", "/api/servicios/calcular-costo").hasRole("ADMIN")
						.requestMatchers(org.springframework.http.HttpMethod.GET, "/api/servicios", "/api/servicios/**").hasAnyRole("ADMIN", "ASISTENTE", "VETERINARIO")
						.requestMatchers(org.springframework.http.HttpMethod.GET, "/api/veterinarios", "/api/veterinarios/**").hasAnyRole("ADMIN", "ASISTENTE", "VETERINARIO")
						.requestMatchers(org.springframework.http.HttpMethod.GET, "/api/asistentes", "/api/asistentes/**").hasAnyRole("ADMIN", "ASISTENTE", "VETERINARIO")
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
