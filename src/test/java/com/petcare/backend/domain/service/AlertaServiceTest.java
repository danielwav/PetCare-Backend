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
import com.petcare.backend.domain.dto.response.PanelAlertasDiaResponse;
import com.petcare.backend.domain.dto.response.ServicioResponse;
import com.petcare.backend.domain.dto.response.VacunaResponse;
import com.petcare.backend.domain.dto.response.VeterinarioResponse;
import com.petcare.backend.domain.repository.CitaRepository;
import com.petcare.backend.persistence.entity.Cita;
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

@SpringBootTest
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
class AlertaServiceTest {

	@Autowired
	private AlertaService alertaService;

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

	@Autowired
	private VacunaService vacunaService;

	@Autowired
	private CitaRepository citaRepository;

	@Test
	void buildDailyPanelWithAppointmentsVaccinesAndMonthlyControls() {
		TestData data = createBaseData();
		CitaResponse scheduled = citaService.create(baseCitaRequest(data, nextDate(DayOfWeek.MONDAY), LocalTime.of(9, 0)));
		CitaResponse confirmed = citaService.create(baseCitaRequest(data, nextDate(DayOfWeek.MONDAY), LocalTime.of(9, 30)));
		CitaResponse noShow = citaService.create(baseCitaRequest(data, nextDate(DayOfWeek.MONDAY), LocalTime.of(10, 0)));
		moveCitaToToday(scheduled.id(), EstadoCita.PROGRAMADA, true);
		moveCitaToToday(confirmed.id(), EstadoCita.CONFIRMADA, false);
		moveCitaToToday(noShow.id(), EstadoCita.NO_ASISTIO, false);
		createVaccineAlerts(data);

		PanelAlertasDiaResponse panel = alertaService.getDailyPanel(LocalDate.now(), 30);

		assertThat(panel.totalCitasProgramadasHoy()).isEqualTo(2);
		assertThat(panel.totalCitasSinConfirmar()).isEqualTo(1);
		assertThat(panel.totalCitasConfirmadasPendientesAtencion()).isEqualTo(1);
		assertThat(panel.totalCitasNoAsistidasHoy()).isEqualTo(1);
		assertThat(panel.totalVacunasProximas()).isEqualTo(1);
		assertThat(panel.totalVacunasVencidas()).isEqualTo(1);
		assertThat(panel.totalControlesMensualesPendientes()).isEqualTo(1);
		assertThat(panel.citasSinConfirmar()).extracting(alert -> alert.citaId()).contains(scheduled.id());
		assertThat(panel.citasConfirmadasPendientesAtencion()).extracting(alert -> alert.citaId()).contains(confirmed.id());
		assertThat(panel.citasNoAsistidasHoy()).extracting(alert -> alert.citaId()).contains(noShow.id());
		assertThat(panel.controlesMensualesPendientes()).extracting(control -> control.mascotaId()).contains(data.mascota().id());
	}

	private void createVaccineAlerts(TestData data) {
		VacunaResponse rabia = vacunaService.create(new VacunaRequest(
				"Rabia",
				"Proteccion antirrabica.",
				null
		));
		VacunaResponse moquillo = vacunaService.create(new VacunaRequest(
				"Moquillo",
				"Proteccion contra moquillo.",
				null
		));

		vacunaService.registerForMascota(data.mascota().id(), new VacunaMascotaRequest(
				rabia.id(),
				data.veterinario().id(),
				null,
				LocalDate.now(),
				null,
				LocalDate.now().plusDays(15),
				"Proxima dosis dentro de la ventana."
		));
		vacunaService.registerForMascota(data.mascota().id(), new VacunaMascotaRequest(
				moquillo.id(),
				data.veterinario().id(),
				null,
				LocalDate.now().minusDays(40),
				null,
				LocalDate.now().minusDays(10),
				"Dosis vencida."
		));
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
				"Sin observaciones",
				null
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

	private CitaRequest baseCitaRequest(TestData data, LocalDate fecha, LocalTime horaInicio) {
		return new CitaRequest(
				data.duenio().id(),
				data.mascota().id(),
				data.veterinario().id(),
				fecha,
				horaInicio,
				30,
				"Consulta preventiva",
				List.of(new CostoCitaServicioRequest(data.consulta().id(), 1)),
				BigDecimal.ZERO
		);
	}

	private void moveCitaToToday(Long citaId, EstadoCita estado, Boolean requiereConfirmacion) {
		Cita cita = citaRepository.findById(citaId).orElseThrow();
		cita.setFecha(LocalDate.now());
		cita.setEstado(estado);
		cita.setRequiereConfirmacion(requiereConfirmacion);
		citaRepository.save(cita);
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
