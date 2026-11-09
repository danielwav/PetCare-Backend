package com.petcare.backend.domain.service;

import com.petcare.backend.domain.dto.request.HorarioVeterinarioRequest;
import com.petcare.backend.domain.dto.request.VeterinarioRequest;
import com.petcare.backend.domain.dto.response.DisponibilidadVeterinarioResponse;
import com.petcare.backend.domain.dto.response.VeterinarioResponse;
import com.petcare.backend.domain.repository.UsuarioRepository;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.CALLS_REAL_METHODS;
import static org.mockito.Mockito.mockStatic;

@SpringBootTest
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
class VeterinarioServiceTest {

	@Autowired
	private AuthService authService;

	@Autowired
	private UsuarioRepository usuarioRepository;

	@Autowired
	private VeterinarioService veterinarioService;

	@Autowired
	private ClinicaService clinicaService;

	private Long clinicaId() {
		return clinicaService.getOrCreateDefaultClinic().getId();
	}

	@Test
	void createFindUpdateAndDeactivateVeterinario() {
		VeterinarioResponse created = veterinarioService.create(baseRequest(
				"CMVP-001",
				"ana.vet@test.com",
				"Medicina general"
		), clinicaId());

		VeterinarioResponse found = veterinarioService.findById(created.id(), clinicaId());
		VeterinarioResponse updated = veterinarioService.update(created.id(), new VeterinarioRequest(
				null,
				"Ana Maria",
				"Salas",
				"CMVP-001",
				"Dermatologia",
				"999888777",
				"ana.derma@test.com",
				List.of(new HorarioVeterinarioRequest(
						DayOfWeek.TUESDAY,
						LocalTime.of(10, 0),
						LocalTime.of(12, 0),
						30
				))
		), clinicaId());

		veterinarioService.deactivate(created.id(), clinicaId());
		VeterinarioResponse inactive = veterinarioService.findById(created.id(), clinicaId());

		assertThat(found.email()).isEqualTo("ana.vet@test.com");
		assertThat(found.horarios()).hasSize(1);
		assertThat(updated.nombres()).isEqualTo("Ana Maria");
		assertThat(updated.especialidad()).isEqualTo("Dermatologia");
		assertThat(updated.horarios().getFirst().diaSemana()).isEqualTo(DayOfWeek.TUESDAY);
		assertThat(inactive.active()).isFalse();
		assertThat(inactive.horarios()).allMatch(horario -> !horario.active());
	}

	@Test
	void calculateDisponibilidadFromHorario() {
		VeterinarioResponse veterinario = veterinarioService.create(baseRequest(
				"CMVP-002",
				"carlos.vet@test.com",
				"Cirugia menor"
		), clinicaId());
		LocalDate nextMonday = nextDate(DayOfWeek.MONDAY);

		DisponibilidadVeterinarioResponse disponibilidad = veterinarioService.findDisponibilidad(
				veterinario.id(),
				nextMonday,
				null,
				clinicaId()
		);

		assertThat(disponibilidad.fecha()).isEqualTo(nextMonday);
		assertThat(disponibilidad.horariosDisponibles()).containsExactly(
				LocalTime.of(9, 0),
				LocalTime.of(9, 30),
				LocalTime.of(10, 0),
				LocalTime.of(10, 30)
		);
	}

	@Test
	void todayOnlyOffersFutureSlotsOnTheScheduleBlockGrid() {
		LocalDate today = LocalDate.now();
		VeterinarioRequest base = baseRequest("CMVP-TODAY", "today.vet@test.com", "Medicina general");
		VeterinarioResponse veterinario = veterinarioService.create(new VeterinarioRequest(
				base.usuarioId(), base.nombres(), base.apellidos(), base.numeroColegiatura(),
				base.especialidad(), base.telefono(), base.email(),
				List.of(new HorarioVeterinarioRequest(today.getDayOfWeek(), LocalTime.of(9, 0), LocalTime.of(11, 0), 30))
		), clinicaId());
		LocalDateTime now = today.atTime(10, 0);

		try (MockedStatic<LocalDateTime> time = mockStatic(LocalDateTime.class, CALLS_REAL_METHODS)) {
			time.when(LocalDateTime::now).thenReturn(now);
			assertThat(veterinarioService.findDisponibilidad(veterinario.id(), today, 15, clinicaId())
					.horariosDisponibles()).containsExactly(LocalTime.of(10, 30));
		}
	}

	@Test
	void lateScheduleDoesNotWrapAndLongDurationsDoNotFit() {
		VeterinarioRequest base = baseRequest("CMVP-LATE", "late.vet@test.com", "Medicina general");
		VeterinarioResponse veterinario = veterinarioService.create(new VeterinarioRequest(
				base.usuarioId(), base.nombres(), base.apellidos(), base.numeroColegiatura(),
				base.especialidad(), base.telefono(), base.email(),
				List.of(new HorarioVeterinarioRequest(DayOfWeek.MONDAY, LocalTime.of(23, 0), LocalTime.of(23, 59), 30))
		), clinicaId());
		LocalDate date = nextDate(DayOfWeek.MONDAY);

		assertThat(veterinarioService.findDisponibilidad(veterinario.id(), date, 30, clinicaId())
				.horariosDisponibles()).containsExactly(LocalTime.of(23, 0));
		assertThat(veterinarioService.findDisponibilidad(veterinario.id(), date, 15, clinicaId())
				.horariosDisponibles()).containsExactly(LocalTime.of(23, 0), LocalTime.of(23, 30));
		assertThat(veterinarioService.findDisponibilidad(veterinario.id(), date, 1470, clinicaId())
				.horariosDisponibles()).isEmpty();
		assertThat(veterinarioService.findDisponibilidad(veterinario.id(), date, Integer.MAX_VALUE, clinicaId())
				.horariosDisponibles()).isEmpty();
		assertThatThrownBy(() -> veterinarioService.findDisponibilidad(veterinario.id(), date, 14, clinicaId()))
				.isInstanceOf(IllegalArgumentException.class);
		assertThatThrownBy(() -> veterinarioService.findDisponibilidad(veterinario.id(), date, 30, clinicaId() + 1))
				.isInstanceOf(AccessDeniedException.class);
	}

	@Test
	void searchByEspecialidadAndRejectDuplicatedFields() {
		veterinarioService.create(baseRequest("CMVP-003", "luisa.vet@test.com", "Odontologia"), clinicaId());

		List<VeterinarioResponse> results = veterinarioService.findAll("odonto", true, clinicaId());

		assertThat(results).hasSize(1);
		assertThat(results.getFirst().numeroColegiatura()).isEqualTo("CMVP-003");

		assertThatThrownBy(() -> veterinarioService.create(baseRequest(
				"CMVP-003",
				"otro.vet@test.com",
				"Medicina general"
		), clinicaId())).isInstanceOf(IllegalArgumentException.class);

		assertThatThrownBy(() -> veterinarioService.create(baseRequest(
				"CMVP-004",
				"luisa.vet@test.com",
				"Medicina general"
		), clinicaId())).isInstanceOf(IllegalArgumentException.class);
	}

	@Test
	void rejectInvalidHorario() {
		VeterinarioRequest request = new VeterinarioRequest(
				ServiceTestFixtures.veterinarioUser(authService, usuarioRepository, clinicaId(), "marco.vet@test.com"),
				"Marco",
				"Ruiz",
				"CMVP-005",
				"Emergencias",
				"999777666",
				"marco.vet@test.com",
				List.of(new HorarioVeterinarioRequest(
						DayOfWeek.WEDNESDAY,
						LocalTime.of(12, 0),
						LocalTime.of(10, 0),
						30
				))
		);

		assertThatThrownBy(() -> veterinarioService.create(request, clinicaId()))
				.isInstanceOf(IllegalArgumentException.class);
	}

	private VeterinarioRequest baseRequest(String colegiatura, String email, String especialidad) {
		return new VeterinarioRequest(
				ServiceTestFixtures.veterinarioUser(authService, usuarioRepository, clinicaId(), email),
				"Ana",
				"Salas",
				colegiatura,
				especialidad,
				"999888777",
				email,
				List.of(new HorarioVeterinarioRequest(
						DayOfWeek.MONDAY,
						LocalTime.of(9, 0),
						LocalTime.of(11, 0),
						30
				))
		);
	}

	private LocalDate nextDate(DayOfWeek dayOfWeek) {
		LocalDate date = LocalDate.now().plusDays(1);
		while (date.getDayOfWeek() != dayOfWeek) {
			date = date.plusDays(1);
		}
		return date;
	}
}
