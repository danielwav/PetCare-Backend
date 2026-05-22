package com.petcare.backend.domain.service;

import com.petcare.backend.domain.dto.request.AsistenteRequest;
import com.petcare.backend.domain.dto.request.LoginRequest;
import com.petcare.backend.domain.dto.response.AsistenteResponse;
import com.petcare.backend.domain.dto.response.AuthResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
class AsistenteServiceTest {

	@Autowired
	private AsistenteService asistenteService;

	@Autowired
	private AuthService authService;

	@Test
	void createAssistantCreatesUserAndAssignsAssistantRole() {
		AsistenteResponse created = asistenteService.create(baseRequest(
				"Maria",
				"Lopez",
				"70500001",
				"maria.asistente@test.com",
				"Agenda de citas y atencion al cliente",
				"secret123"
		));

		AuthResponse login = authService.login(new LoginRequest("maria.asistente@test.com", "secret123"));

		assertThat(created.usuarioId()).isNotNull();
		assertThat(created.funciones()).isEqualTo("Agenda de citas y atencion al cliente");
		assertThat(login.user().roles()).contains("ROLE_ASISTENTE");
	}

	@Test
	void updateSearchDeactivateAndActivateAssistant() {
		AsistenteResponse created = asistenteService.create(baseRequest(
				"Rosa",
				"Perez",
				"70500002",
				"rosa.asistente@test.com",
				"Recepcion",
				"secret123"
		));

		AsistenteResponse updated = asistenteService.update(created.id(), new AsistenteRequest(
				null,
				"Rosa",
				"Perez",
				"DNI",
				"70500002",
				"999888111",
				"rosa.asistente@test.com",
				"Recepcion, agenda y caja",
				null
		));
		List<AsistenteResponse> results = asistenteService.findAll("caja", true);
		asistenteService.deactivate(created.id());
		AsistenteResponse inactive = asistenteService.findById(created.id());
		AsistenteResponse active = asistenteService.activate(created.id());

		assertThat(updated.funciones()).isEqualTo("Recepcion, agenda y caja");
		assertThat(results).extracting(AsistenteResponse::id).containsExactly(created.id());
		assertThat(inactive.active()).isFalse();
		assertThat(active.active()).isTrue();
	}

	@Test
	void rejectDuplicateAssistantEmailOrDocument() {
		asistenteService.create(baseRequest(
				"Claudia",
				"Ramos",
				"70500003",
				"claudia.asistente@test.com",
				"Recepcion",
				"secret123"
		));

		assertThatThrownBy(() -> asistenteService.create(baseRequest(
				"Claudia",
				"Soto",
				"70500004",
				"claudia.asistente@test.com",
				"Agenda",
				"secret123"
		))).isInstanceOf(IllegalArgumentException.class);
		assertThatThrownBy(() -> asistenteService.create(baseRequest(
				"Ana",
				"Ramos",
				"70500003",
				"ana.asistente@test.com",
				"Caja",
				"secret123"
		))).isInstanceOf(IllegalArgumentException.class);
	}

	private AsistenteRequest baseRequest(
			String nombres,
			String apellidos,
			String documento,
			String email,
			String funciones,
			String password
	) {
		return new AsistenteRequest(
				null,
				nombres,
				apellidos,
				"DNI",
				documento,
				"999888777",
				email,
				funciones,
				password
		);
	}
}
