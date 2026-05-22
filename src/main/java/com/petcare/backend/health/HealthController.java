package com.petcare.backend.health;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/health")
@RequiredArgsConstructor
public class HealthController {

	private final JdbcTemplate jdbcTemplate;

	@GetMapping
	ResponseEntity<Map<String, String>> health() {
		try {
			jdbcTemplate.queryForObject("select 1", Integer.class);
			return ResponseEntity.ok(Map.of(
					"status", "UP",
					"database", "UP"
			));
		} catch (RuntimeException exception) {
			return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(Map.of(
					"status", "DOWN",
					"database", "DOWN"
			));
		}
	}
}
