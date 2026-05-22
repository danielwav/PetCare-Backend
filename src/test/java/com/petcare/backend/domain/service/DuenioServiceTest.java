package com.petcare.backend.domain.service;

import com.petcare.backend.domain.dto.request.DuenioRequest;
import com.petcare.backend.domain.dto.response.DuenioResponse;
import com.petcare.backend.domain.repository.DuenioRepository;
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
class DuenioServiceTest {

	@Autowired
	private DuenioService duenioService;

	@Autowired
	private DuenioRepository duenioRepository;

	@Test
	void createFindUpdateAndDeactivateDuenio() {
		DuenioResponse created = duenioService.create(new DuenioRequest(
				null,
				"Maria",
				"Lopez",
				"DNI",
				"70000001",
				"999111222",
				"maria.lopez@test.com",
				"Av. PetCare 123"
		));

		DuenioResponse found = duenioService.findById(created.id());
		DuenioResponse updated = duenioService.update(created.id(), new DuenioRequest(
				null,
				"Maria Fernanda",
				"Lopez",
				"DNI",
				"70000001",
				"999111333",
				"maria.fernanda@test.com",
				"Av. PetCare 456"
		));

		duenioService.deactivate(created.id());
		DuenioResponse inactive = duenioService.findById(created.id());

		assertThat(found.email()).isEqualTo("maria.lopez@test.com");
		assertThat(updated.nombres()).isEqualTo("Maria Fernanda");
		assertThat(updated.telefono()).isEqualTo("999111333");
		assertThat(inactive.active()).isFalse();
	}

	@Test
	void searchDueniosByTextAndActiveStatus() {
		duenioRepository.deleteAll();

		duenioService.create(new DuenioRequest(
				null,
				"Carlos",
				"Paredes",
				"DNI",
				"70000002",
				"999222333",
				"carlos.paredes@test.com",
				null
		));
		DuenioResponse inactive = duenioService.create(new DuenioRequest(
				null,
				"Lucia",
				"Ramos",
				"DNI",
				"70000003",
				"999333444",
				"lucia.ramos@test.com",
				null
		));
		duenioService.deactivate(inactive.id());

		List<DuenioResponse> activeResults = duenioService.findAll("paredes", true);
		List<DuenioResponse> inactiveResults = duenioService.findAll(null, false);

		assertThat(activeResults).hasSize(1);
		assertThat(activeResults.getFirst().email()).isEqualTo("carlos.paredes@test.com");
		assertThat(inactiveResults).hasSize(1);
		assertThat(inactiveResults.getFirst().email()).isEqualTo("lucia.ramos@test.com");
	}

	@Test
	void rejectDuplicatedEmailAndDocument() {
		duenioRepository.deleteAll();

		duenioService.create(new DuenioRequest(
				null,
				"Ana",
				"Torres",
				"DNI",
				"70000004",
				"999444555",
				"ana.torres@test.com",
				null
		));

		assertThatThrownBy(() -> duenioService.create(new DuenioRequest(
				null,
				"Ana",
				"Duplicada",
				"DNI",
				"70000005",
				"999444556",
				"ana.torres@test.com",
				null
		))).isInstanceOf(IllegalArgumentException.class);

		assertThatThrownBy(() -> duenioService.create(new DuenioRequest(
				null,
				"Documento",
				"Duplicado",
				"DNI",
				"70000004",
				"999444557",
				"documento.duplicado@test.com",
				null
		))).isInstanceOf(IllegalArgumentException.class);
	}
}
