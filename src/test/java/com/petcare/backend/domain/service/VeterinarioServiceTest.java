package com.petcare.backend.domain.service;

import com.petcare.backend.domain.dto.request.HorarioVeterinarioRequest;
import com.petcare.backend.domain.dto.request.VeterinarioRequest;
import com.petcare.backend.domain.dto.response.DisponibilidadVeterinarioResponse;
import com.petcare.backend.domain.dto.response.VeterinarioResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
class VeterinarioServiceTest {

	@Autowired
	private VeterinarioService veterinarioService;

	@Test
	void createFindUpdateAndDeactivateVeterinario() {
		VeterinarioResponse created = veterinarioService.create(baseRequest(
				"CMVP-001",
				"ana.vet@test.com",
				"Medicina general"
		));

		VeterinarioResponse found = veterinarioService.findById(created.id());
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
		));

		veterinarioService.deactivate(created.id());
		VeterinarioResponse inactive = veterinarioService.findById(created.id());

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
		));
		LocalDate nextMonday = nextDate(DayOfWeek.MONDAY);

		DisponibilidadVeterinarioResponse disponibilidad = veterinarioService.findDisponibilidad(
				veterinario.id(),
				nextMonday,
				null
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
	void searchByEspecialidadAndRejectDuplicatedFields() {
		veterinarioService.create(baseRequest("CMVP-003", "luisa.vet@test.com", "Odontologia"));

		List<VeterinarioResponse> results = veterinarioService.findAll("odonto", true);

		assertThat(results).hasSize(1);
		assertThat(results.getFirst().numeroColegiatura()).isEqualTo("CMVP-003");

		assertThatThrownBy(() -> veterinarioService.create(baseRequest(
				"CMVP-003",
				"otro.vet@test.com",
				"Medicina general"
		))).isInstanceOf(IllegalArgumentException.class);

		assertThatThrownBy(() -> veterinarioService.create(baseRequest(
				"CMVP-004",
				"luisa.vet@test.com",
				"Medicina general"
		))).isInstanceOf(IllegalArgumentException.class);
	}

	@Test
	void rejectInvalidHorario() {
		VeterinarioRequest request = new VeterinarioRequest(
				null,
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

		assertThatThrownBy(() -> veterinarioService.create(request))
				.isInstanceOf(IllegalArgumentException.class);
	}

	private VeterinarioRequest baseRequest(String colegiatura, String email, String especialidad) {
		return new VeterinarioRequest(
				null,
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
		LocalDate date = LocalDate.now();
		while (date.getDayOfWeek() != dayOfWeek) {
			date = date.plusDays(1);
		}
		return date;
	}
}
