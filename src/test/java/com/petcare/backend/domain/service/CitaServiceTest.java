package com.petcare.backend.domain.service;

import com.petcare.backend.domain.dto.request.CitaRequest;
import com.petcare.backend.domain.dto.request.CostoCitaServicioRequest;
import com.petcare.backend.domain.dto.request.DuenioRequest;
import com.petcare.backend.domain.dto.request.HorarioVeterinarioRequest;
import com.petcare.backend.domain.dto.request.MascotaRequest;
import com.petcare.backend.domain.dto.request.ServicioRequest;
import com.petcare.backend.domain.dto.request.VeterinarioRequest;
import com.petcare.backend.domain.dto.response.CitaResponse;
import com.petcare.backend.domain.dto.response.DuenioResponse;
import com.petcare.backend.domain.dto.response.MascotaResponse;
import com.petcare.backend.domain.dto.response.ServicioResponse;
import com.petcare.backend.domain.dto.response.VeterinarioResponse;
import com.petcare.backend.persistence.enums.EstadoCita;
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
class CitaServiceTest {

	@Autowired
	private CitaService citaService;

	@Autowired
	private DuenioService duenioService;

	@Autowired
	private MascotaService mascotaService;

	@Autowired
	private VeterinarioService veterinarioService;

	@Autowired
	private ServicioService servicioService;

	@Test
	void createFindUpdateAndCancelCita() {
		TestData data = createBaseData();
		LocalDate nextMonday = nextDate(DayOfWeek.MONDAY);

		CitaResponse created = citaService.create(baseCitaRequest(data, nextMonday, LocalTime.of(9, 0), "Consulta preventiva"));
		CitaResponse found = citaService.findById(created.id());
		CitaResponse updated = citaService.update(created.id(), baseCitaRequest(
				data,
				nextMonday,
				LocalTime.of(10, 0),
				"Consulta y vacuna"
		));
		CitaResponse canceled = citaService.cancel(created.id());

		assertThat(found.estado()).isEqualTo(EstadoCita.PROGRAMADA);
		assertThat(found.subtotal()).isEqualByComparingTo("130.00");
		assertThat(found.descuento()).isEqualByComparingTo("10.00");
		assertThat(found.total()).isEqualByComparingTo("120.00");
		assertThat(found.requiereConfirmacion()).isTrue();
		assertThat(found.fechaConfirmacion()).isNull();
		assertThat(found.detallesCosto()).hasSize(2);
		assertThat(updated.horaInicio()).isEqualTo(LocalTime.of(10, 0));
		assertThat(updated.horaFin()).isEqualTo(LocalTime.of(10, 30));
		assertThat(canceled.estado()).isEqualTo(EstadoCita.CANCELADA);
	}

	@Test
	void searchCitasByFilters() {
		TestData data = createBaseData();
		LocalDate nextMonday = nextDate(DayOfWeek.MONDAY);
		citaService.create(baseCitaRequest(data, nextMonday, LocalTime.of(9, 0), "Control general"));

		List<CitaResponse> results = citaService.findAll(
				EstadoCita.PROGRAMADA,
				nextMonday,
				data.duenio().id(),
				data.mascota().id(),
				data.veterinario().id()
		);

		assertThat(results).hasSize(1);
		assertThat(results.getFirst().mascotaNombre()).isEqualTo("Firulais");
	}

	@Test
	void rejectOverlappingAppointment() {
		TestData data = createBaseData();
		LocalDate nextMonday = nextDate(DayOfWeek.MONDAY);
		citaService.create(baseCitaRequest(data, nextMonday, LocalTime.of(9, 0), "Control general"));

		assertThatThrownBy(() -> citaService.create(baseCitaRequest(
				data,
				nextMonday,
				LocalTime.of(9, 15),
				"Cita cruzada"
		))).isInstanceOf(IllegalArgumentException.class);
	}

	@Test
	void rejectAppointmentOutsideVeterinarioSchedule() {
		TestData data = createBaseData();
		LocalDate nextMonday = nextDate(DayOfWeek.MONDAY);

		assertThatThrownBy(() -> citaService.create(baseCitaRequest(
				data,
				nextMonday,
				LocalTime.of(12, 0),
				"Fuera de horario"
		))).isInstanceOf(IllegalArgumentException.class);
	}

	@Test
	void confirmCitaAndRemoveFromConfirmationAlerts() {
		TestData data = createBaseData();
		LocalDate nextMonday = nextDate(DayOfWeek.MONDAY);
		CitaResponse created = citaService.create(baseCitaRequest(data, nextMonday, LocalTime.of(9, 0), "Control general"));

		List<CitaResponse> alertsBefore = citaService.findConfirmationAlerts(24 * 8);
		CitaResponse confirmed = citaService.confirm(created.id(), "asistente@test.com");
		List<CitaResponse> alertsAfter = citaService.findConfirmationAlerts(24 * 8);

		assertThat(alertsBefore).extracting(CitaResponse::id).contains(created.id());
		assertThat(confirmed.estado()).isEqualTo(EstadoCita.CONFIRMADA);
		assertThat(confirmed.requiereConfirmacion()).isFalse();
		assertThat(confirmed.fechaConfirmacion()).isNotNull();
		assertThat(confirmed.confirmadaPor()).isEqualTo("asistente@test.com");
		assertThat(alertsAfter).extracting(CitaResponse::id).doesNotContain(created.id());
	}

	@Test
	void rejectConfirmationForCanceledCita() {
		TestData data = createBaseData();
		LocalDate nextMonday = nextDate(DayOfWeek.MONDAY);
		CitaResponse created = citaService.create(baseCitaRequest(data, nextMonday, LocalTime.of(9, 0), "Control general"));
		citaService.cancel(created.id());

		assertThatThrownBy(() -> citaService.confirm(created.id(), "asistente@test.com"))
				.isInstanceOf(IllegalArgumentException.class);
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
		ServicioResponse vacuna = servicioService.create(new ServicioRequest(
				"Vacuna rabia",
				"Aplicacion de vacuna antirrabica.",
				new BigDecimal("80.00")
		));

		return new TestData(duenio, mascota, veterinario, consulta, vacuna);
	}

	private CitaRequest baseCitaRequest(TestData data, LocalDate fecha, LocalTime horaInicio, String motivo) {
		return new CitaRequest(
				data.duenio().id(),
				data.mascota().id(),
				data.veterinario().id(),
				fecha,
				horaInicio,
				30,
				motivo,
				List.of(
						new CostoCitaServicioRequest(data.consulta().id(), 1),
						new CostoCitaServicioRequest(data.vacuna().id(), 1)
				),
				new BigDecimal("10.00")
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
			ServicioResponse consulta,
			ServicioResponse vacuna
	) {
	}
}
