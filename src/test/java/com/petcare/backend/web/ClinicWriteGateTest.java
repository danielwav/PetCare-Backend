package com.petcare.backend.web;

import com.petcare.backend.persistence.entity.Clinica;
import com.petcare.backend.persistence.entity.Usuario;
import com.petcare.backend.persistence.enums.EstadoClinica;
import com.petcare.backend.persistence.enums.PlanClinica;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class ClinicWriteGateTest {
	@Autowired MockMvc mvc;
	@Autowired EntityManager em;

	@Test
	void expiredOwnerAndAdminCannotWriteButCanRead() throws Exception {
		clinic(PlanClinica.TRIAL, EstadoClinica.ACTIVA);
		for (String role : new String[]{"DUENIO", "ADMIN"}) {
			mvc.perform(post("/api/mascotas").with(user("test@test.com").roles(role)))
					.andExpect(status().isForbidden()).andExpect(jsonPath("$.code").value("TRIAL_EXPIRED"));
			mvc.perform(get("/api/clinicas/me").with(user("test@test.com").roles(role)))
					.andExpect(status().isOk()).andExpect(jsonPath("$.readOnly").value(true));
		}
		mvc.perform(put("/api/clinicas/me").with(user("test@test.com").roles("ADMIN")))
				.andExpect(status().isForbidden()).andExpect(jsonPath("$.code").value("TRIAL_EXPIRED"));
		mvc.perform(get("/api/clinicas/me/plan").with(user("test@test.com").roles("ADMIN")))
				.andExpect(status().isOk()).andExpect(jsonPath("$.status").value("EXPIRED"));
		for (String role : new String[]{"DUENIO", "VETERINARIO", "ASISTENTE"}) {
			mvc.perform(get("/api/clinicas/me/plan").with(user("test@test.com").roles(role)))
					.andExpect(status().isForbidden());
		}
	}

	@Test
	void freeAndSuspendedClinicsCannotWriteButKeepReads() throws Exception {
		Clinica c = clinic(PlanClinica.FREE, EstadoClinica.ACTIVA);
		for (EstadoClinica state : new EstadoClinica[]{EstadoClinica.ACTIVA, EstadoClinica.SUSPENDIDA}) {
			c.setEstado(state);
			mvc.perform(delete("/api/mascotas/1").with(user("test@test.com").roles("ADMIN")))
					.andExpect(status().isForbidden()).andExpect(jsonPath("$.code").value("CLINIC_READ_ONLY"));
			mvc.perform(get("/api/clinicas/me").with(user("test@test.com").roles("ADMIN")))
					.andExpect(status().isOk());
		}
	}

	@Test
	void readOnlyCostPostAllowedButAuthBusinessWritesNotBypassed() throws Exception {
		clinic(PlanClinica.TRIAL, EstadoClinica.ACTIVA);
		mvc.perform(post("/api/servicios/calcular-costo").with(user("test@test.com").roles("ADMIN"))
				.contentType("application/json").content("{}"))
				.andExpect(status().isBadRequest());
		mvc.perform(post("/api/auth/future-business-action").with(user("test@test.com").roles("ADMIN")))
				.andExpect(status().isForbidden()).andExpect(jsonPath("$.code").value("TRIAL_EXPIRED"));
		for (String path : new String[]{"login", "refresh", "register", "register-clinic", "change-password", "set-password"}) {
			mvc.perform(post("/api/auth/" + path).with(user("test@test.com").roles("ADMIN"))
					.contentType("application/json").content("{}"))
					.andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").doesNotExist());
		}
	}

	private Clinica clinic(PlanClinica plan, EstadoClinica state) {
		LocalDateTime now = LocalDateTime.now(java.time.Clock.systemUTC());
		Clinica c = Clinica.builder().nombre("Test").slug("test").plan(plan).estado(state)
				.trialStartedAt(now.minusDays(15)).trialEndsAt(now.minusDays(1)).createdAt(now).updatedAt(now).build();
		em.persist(c);
		em.persist(Usuario.builder().fullName("Test").email("test@test.com").password("test").active(true)
				.clinica(c).createdAt(now).build());
		return c;
	}
}
