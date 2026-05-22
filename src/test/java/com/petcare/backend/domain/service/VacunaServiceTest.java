package com.petcare.backend.domain.service;

import com.petcare.backend.domain.dto.request.CitaRequest;
import com.petcare.backend.domain.dto.request.CostoCitaServicioRequest;
import com.petcare.backend.domain.dto.request.DuenioRequest;
import com.petcare.backend.domain.dto.request.HorarioVeterinarioRequest;
import com.petcare.backend.domain.dto.request.MascotaRequest;
import com.petcare.backend.domain.dto.request.ServicioRequest;
import com.petcare.backend.domain.dto.request.VacunaMascotaRequest;
import com.petcare.backend.domain.dto.request.VacunaRequest;
import com.petcare.backend.domain.dto.request.VeterinarioRequest;
import com.petcare.backend.domain.dto.response.CitaResponse;
import com.petcare.backend.domain.dto.response.DuenioResponse;
import com.petcare.backend.domain.dto.response.MascotaResponse;
import com.petcare.backend.domain.dto.response.ServicioResponse;
import com.petcare.backend.domain.dto.response.VacunaMascotaResponse;
import com.petcare.backend.domain.dto.response.VacunaResponse;
import com.petcare.backend.domain.dto.response.VeterinarioResponse;
import com.petcare.backend.persistence.enums.SexoMascota;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
class VacunaServiceTest {

	@Autowired
	private VacunaService vacunaService;

	@Autowired
	private DuenioService duenioService;

	@Autowired
	private MascotaService mascotaService;

	@Autowired
	private VeterinarioService veterinarioService;

	@Autowired
	private ServicioService servicioService;

	@Autowired
	private CitaService citaService;

	@Test
	void createUpdateAndDeactivateVaccineCatalog() {
		VacunaResponse created = vacunaService.create(baseVacunaRequest("Rabia", 365));
		VacunaResponse updated = vacunaService.update(created.id(), new VacunaRequest(
				"Rabia anual",
				"Proteccion antirrabica anual.",
				365
		));
		List<VacunaResponse> results = vacunaService.findAll("rabia", null);

		vacunaService.deactivate(created.id());
		VacunaResponse inactive = vacunaService.findById(created.id());

		assertThat(updated.nombre()).isEqualTo("Rabia anual");
		assertThat(results).hasSize(1);
		assertThat(inactive.active()).isFalse();
		assertThatThrownBy(() -> vacunaService.create(baseVacunaRequest("rabia anual", 365)))
				.isInstanceOf(IllegalArgumentException.class);
	}

	@Test
	void registerVaccineForPetAndCalculateNextDose() {
		TestData data = createBaseData();
		VacunaResponse vacuna = vacunaService.create(baseVacunaRequest("Triple felina", 180));
		CitaResponse cita = citaService.create(baseCitaRequest(data));

		VacunaMascotaResponse applied = vacunaService.registerForMascota(data.mascota().id(), new VacunaMascotaRequest(
				vacuna.id(),
				data.veterinario().id(),
				cita.id(),
				LocalDate.now(),
				"LOTE-001",
				null,
				"Primera dosis aplicada sin reacciones."
		));
		List<VacunaMascotaResponse> petVaccines = vacunaService.findByMascota(data.mascota().id());

		assertThat(applied.fechaProximaDosis()).isEqualTo(LocalDate.now().plusDays(180));
		assertThat(applied.citaId()).isEqualTo(cita.id());
		assertThat(applied.estadoAlerta()).isEqualTo("PROGRAMADA");
		assertThat(petVaccines).extracting(VacunaMascotaResponse::id).contains(applied.id());
	}

	@Test
	void findUpcomingAndAlerts() {
		TestData data = createBaseData();
		VacunaResponse vacuna = vacunaService.create(baseVacunaRequest("Parvovirus", null));
		VacunaMascotaResponse applied = vacunaService.registerForMascota(data.mascota().id(), new VacunaMascotaRequest(
				vacuna.id(),
				data.veterinario().id(),
				null,
				LocalDate.now(),
				null,
				LocalDate.now().plusDays(15),
				"Proxima dosis indicada manualmente."
		));

		List<VacunaMascotaResponse> upcoming = vacunaService.findUpcoming(20);
		List<VacunaMascotaResponse> alerts = vacunaService.findAlerts(null);

		assertThat(upcoming).extracting(VacunaMascotaResponse::id).contains(applied.id());
		assertThat(alerts).extracting(VacunaMascotaResponse::id).contains(applied.id());
		assertThat(alerts.getFirst().estadoAlerta()).isEqualTo("PROXIMA");
	}

	@Test
	void rejectInactiveVaccineOrMismatchedAppointment() {
		TestData data = createBaseData();
		VacunaResponse vacuna = vacunaService.create(baseVacunaRequest("Moquillo", 365));
		vacunaService.deactivate(vacuna.id());

		assertThatThrownBy(() -> vacunaService.registerForMascota(data.mascota().id(), new VacunaMascotaRequest(
				vacuna.id(),
				data.veterinario().id(),
				null,
				LocalDate.now(),
				null,
				null,
				null
		))).isInstanceOf(IllegalArgumentException.class);
	}

	private TestData createBaseData() {
		DuenioResponse duenio = duenioService.create(new DuenioRequest(
				null,
				"Daniel",
				"Torres",
				"DNI",
				"12345678",
				"999888777",
				"daniel@test.com",
				"Av. Siempre Viva 123"
		));
		MascotaResponse mascota = mascotaService.create(new MascotaRequest(
				duenio.id(),
				"Firulais",
				"Perro",
				"Mestizo",
				SexoMascota.MACHO,
				LocalDate.now().minusYears(2),
				"Marron",
				new BigDecimal("12.50"),
				"Sin observaciones"
		));
		VeterinarioResponse veterinario = veterinarioService.create(new VeterinarioRequest(
				null,
				"Ana",
				"Salas",
				"CMVP-001",
				"Medicina general",
				"999777666",
				"ana.vet@test.com",
				List.of(new HorarioVeterinarioRequest(
						DayOfWeek.MONDAY,
						LocalTime.of(9, 0),
						LocalTime.of(11, 0),
						30
				))
		));
		ServicioResponse consulta = servicioService.create(new ServicioRequest(
				"Consulta general",
				"Evaluacion clinica basica.",
				new BigDecimal("50.00")
		));

		return new TestData(duenio, mascota, veterinario, consulta);
	}

	private CitaRequest baseCitaRequest(TestData data) {
		return new CitaRequest(
				data.duenio().id(),
				data.mascota().id(),
				data.veterinario().id(),
				nextDate(DayOfWeek.MONDAY),
				LocalTime.of(9, 0),
				30,
				"Consulta preventiva",
				List.of(new CostoCitaServicioRequest(data.consulta().id(), 1)),
				BigDecimal.ZERO
		);
	}

	private VacunaRequest baseVacunaRequest(String nombre, Integer intervaloDias) {
		return new VacunaRequest(
				nombre,
				"Descripcion de vacuna " + nombre,
				intervaloDias
		);
	}

	private LocalDate nextDate(DayOfWeek dayOfWeek) {
		LocalDate date = LocalDate.now().plusDays(1);
		while (date.getDayOfWeek() != dayOfWeek) {
			date = date.plusDays(1);
		}
		return date;
	}

	private record TestData(
			DuenioResponse duenio,
			MascotaResponse mascota,
			VeterinarioResponse veterinario,
			ServicioResponse consulta
	) {
	}
}
