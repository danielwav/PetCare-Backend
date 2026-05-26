package com.petcare.backend.domain.service;

import com.petcare.backend.domain.dto.request.DuenioRequest;
import com.petcare.backend.domain.dto.request.MascotaRequest;
import com.petcare.backend.domain.dto.request.RegisterRequest;
import com.petcare.backend.domain.dto.response.AuthResponse;
import com.petcare.backend.domain.dto.response.DuenioResponse;
import com.petcare.backend.domain.dto.response.MascotaResponse;
import com.petcare.backend.persistence.enums.SexoMascota;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
class MascotaServiceTest {

	@Autowired
	private DuenioService duenioService;

	@Autowired
	private AuthService authService;

	@Autowired
	private MascotaService mascotaService;

	@Test
	void createFindUpdateAndDeactivateMascota() {
		DuenioResponse duenio = createDuenio("71000001", "mascota.duenio@test.com");

			MascotaResponse created = mascotaService.create(new MascotaRequest(
				duenio.id(),
				"Luna",
				"Perro",
				"Labrador",
				SexoMascota.HEMBRA,
				LocalDate.of(2022, 5, 10),
				"Dorado",
				new BigDecimal("18.50"),
				"Vacunas al dia",
				null
		));

		MascotaResponse found = mascotaService.findById(created.id());
		MascotaResponse updated = mascotaService.update(created.id(), new MascotaRequest(
				duenio.id(),
				"Luna",
				"Perro",
				"Labrador Retriever",
				SexoMascota.HEMBRA,
				LocalDate.of(2022, 5, 10),
				"Dorado",
				new BigDecimal("19.20"),
				"Control mensual pendiente",
				null
		));

		mascotaService.deactivate(created.id());
		MascotaResponse inactive = mascotaService.findById(created.id());

		assertThat(found.duenioId()).isEqualTo(duenio.id());
		assertThat(found.nombre()).isEqualTo("Luna");
		assertThat(found.edadAnios()).isEqualTo(java.time.Period.between(LocalDate.of(2022, 5, 10), LocalDate.now()).getYears());
		assertThat(updated.raza()).isEqualTo("Labrador Retriever");
		assertThat(updated.pesoKg()).isEqualByComparingTo("19.20");
		assertThat(inactive.active()).isFalse();
	}

	@Test
	void listMascotasByDuenioAndSearch() {
		DuenioResponse duenio = createDuenio("71000002", "duenio.busqueda@test.com");
		DuenioResponse otherDuenio = createDuenio("71000003", "otro.duenio@test.com");

		mascotaService.create(new MascotaRequest(
				duenio.id(),
				"Milo",
				"Gato",
				"Siames",
				SexoMascota.MACHO,
				LocalDate.of(2021, 2, 12),
				"Crema",
				new BigDecimal("4.80"),
				null,
				null
		));
		mascotaService.create(new MascotaRequest(
				otherDuenio.id(),
				"Rocky",
				"Perro",
				"Beagle",
				SexoMascota.MACHO,
				LocalDate.of(2020, 9, 3),
				"Tricolor",
				new BigDecimal("12.00"),
				null,
				null
		));

		List<MascotaResponse> byDuenio = mascotaService.findByDuenio(duenio.id());
		List<MascotaResponse> searchResults = mascotaService.findAll("siames", null, true);

		assertThat(byDuenio).hasSize(1);
		assertThat(byDuenio.getFirst().nombre()).isEqualTo("Milo");
		assertThat(searchResults).hasSize(1);
		assertThat(searchResults.getFirst().especie()).isEqualTo("Gato");
	}

	@Test
	void rejectMascotaForInactiveDuenio() {
		DuenioResponse duenio = createDuenio("71000004", "duenio.inactivo@test.com");
		duenioService.deactivate(duenio.id());

		assertThatThrownBy(() -> mascotaService.create(new MascotaRequest(
				duenio.id(),
				"Nala",
				"Gato",
				"Mestizo",
				SexoMascota.HEMBRA,
				LocalDate.of(2023, 1, 8),
				null,
				null,
				null,
				null
		))).isInstanceOf(IllegalArgumentException.class);
	}

	@Test
	void duenioCanOnlyConsultOwnMascotas() {
		authService.register(new RegisterRequest("Admin", "admin.mascota@test.com", "secret123"));
		AuthResponse ownerUser = authService.register(new RegisterRequest("Owner", "owner.mascota@test.com", "secret123"));
		AuthResponse otherUser = authService.register(new RegisterRequest("Other", "other.mascota@test.com", "secret123"));
		DuenioResponse owner = createDuenio("71000005", "owner.pet@test.com", ownerUser.user().id());
		DuenioResponse otherOwner = createDuenio("71000006", "other.pet@test.com", otherUser.user().id());
		MascotaResponse ownPet = createMascota(owner.id(), "Lola");
		MascotaResponse otherPet = createMascota(otherOwner.id(), "Toby");

		List<MascotaResponse> ownPets = mascotaService.findAllForDuenio(ownerUser.user().email(), null, true);
		MascotaResponse ownPetById = mascotaService.findByIdForDuenio(ownPet.id(), ownerUser.user().email());

		assertThat(ownPets).extracting(MascotaResponse::id).containsExactly(ownPet.id());
		assertThat(ownPetById.nombre()).isEqualTo("Lola");
		assertThatThrownBy(() -> mascotaService.findByIdForDuenio(otherPet.id(), ownerUser.user().email()))
				.isInstanceOf(AccessDeniedException.class);
		assertThatThrownBy(() -> mascotaService.findByDuenioForDuenio(ownerUser.user().email(), otherOwner.id()))
				.isInstanceOf(AccessDeniedException.class);
	}

	private DuenioResponse createDuenio(String documento, String email) {
		return createDuenio(documento, email, null);
	}

	private DuenioResponse createDuenio(String documento, String email, Long usuarioId) {
		return duenioService.create(new DuenioRequest(
				usuarioId,
				"Nombre",
				"Apellido",
				"DNI",
				documento,
				"999555666",
				email,
				null
		));
	}

	private MascotaResponse createMascota(Long duenioId, String nombre) {
		return mascotaService.create(new MascotaRequest(
				duenioId,
				nombre,
				"Perro",
				"Mestizo",
				SexoMascota.HEMBRA,
				LocalDate.now().minusYears(1),
				null,
				new BigDecimal("8.50"),
				null,
				null
		));
	}
}
