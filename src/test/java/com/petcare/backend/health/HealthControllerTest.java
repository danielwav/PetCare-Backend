package com.petcare.backend.health;

import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class HealthControllerTest {

	@Test
	void healthReturnsUp() throws Exception {
		JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
		when(jdbcTemplate.queryForObject(eq("select 1"), eq(Integer.class))).thenReturn(1);
		MockMvc mockMvc = MockMvcBuilders.standaloneSetup(new HealthController(jdbcTemplate)).build();

		mockMvc.perform(get("/api/health"))
				.andExpect(status().isOk())
				.andExpect(content().json("{\"status\":\"UP\",\"database\":\"UP\"}"));
	}

	@Test
	void healthReturnsUnavailableWhenDatabaseIsDown() throws Exception {
		JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
		when(jdbcTemplate.queryForObject(eq("select 1"), eq(Integer.class)))
				.thenThrow(new IllegalStateException("database down"));
		MockMvc mockMvc = MockMvcBuilders.standaloneSetup(new HealthController(jdbcTemplate)).build();

		mockMvc.perform(get("/api/health"))
				.andExpect(status().isServiceUnavailable())
				.andExpect(content().json("{\"status\":\"DOWN\",\"database\":\"DOWN\"}"));
	}
}
