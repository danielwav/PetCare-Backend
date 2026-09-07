package com.petcare.backend.domain.service;

import com.petcare.backend.domain.dto.request.AtencionClinicaRequest;
import com.petcare.backend.domain.dto.request.CitaRequest;
import com.petcare.backend.domain.dto.request.CostoCitaServicioRequest;
import com.petcare.backend.domain.dto.request.DuenioRequest;
import com.petcare.backend.domain.dto.request.HorarioVeterinarioRequest;
import com.petcare.backend.domain.dto.request.InasistenciaRequest;
import com.petcare.backend.domain.dto.request.MascotaRequest;
import com.petcare.backend.domain.dto.request.ServicioRequest;
import com.petcare.backend.domain.dto.request.VacunaMascotaRequest;
import com.petcare.backend.domain.dto.request.VacunaRequest;
import com.petcare.backend.domain.dto.request.VeterinarioRequest;
import com.petcare.backend.domain.dto.response.CitaResponse;
import com.petcare.backend.domain.dto.response.DuenioResponse;
import com.petcare.backend.domain.dto.response.HistoriaClinicaResponse;
import com.petcare.backend.domain.dto.response.MascotaResponse;
import com.petcare.backend.domain.dto.response.ReporteCitaResponse;
import com.petcare.backend.domain.dto.response.ReporteCostoCitaResponse;
import com.petcare.backend.domain.dto.response.ServicioResponse;
import com.petcare.backend.domain.dto.response.ServicioSolicitadoResponse;
import com.petcare.backend.domain.dto.response.VacunaMascotaResponse;
import com.petcare.backend.domain.dto.response.VacunaResponse;
import com.petcare.backend.domain.dto.response.VeterinarioResponse;
import com.petcare.backend.domain.repository.CitaRepository;
import com.petcare.backend.domain.repository.InasistenciaRepository;
import com.petcare.backend.persistence.entity.Cita;
import com.petcare.backend.persistence.enums.EstadoCita;
import com.petcare.backend.persistence.enums.EstadoMascota;
import com.petcare.backend.persistence.enums.SexoMascota;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.core.Authentication;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

@SpringBootTest
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
class ReporteServiceTest {

	@Autowired
	private ReporteService reporteService;

	@Autowired
	private ClinicaService clinicaService;

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
	private InasistenciaService inasistenciaService;

	@Autowired
	private AtencionClinicaService atencionClinicaService;

	@Autowired
	private CitaRepository citaRepository;

	@Autowired
	private VacunaService vacunaService;

	private Authentication auth;

	@BeforeEach
	void setUp() {
		auth = mock(Authentication.class);
	}

	private Long clinicaId() {
		return clinicaService.getOrCreateDefaultClinic().getId();
	}

	@Test
	void buildOperationalAndClinicalReports() {
		TestData data = createBaseData();
		LocalDate reportDate = nextDate(DayOfWeek.MONDAY);
		CitaResponse cita = citaService.create(baseCitaRequest(data, reportDate), clinicaId());
		VacunaResponse vacuna = vacunaService.create(new VacunaRequest(
				"Rabia",
				"Proteccion antirrabica anual.",
				null
		));
		VacunaMascotaResponse vacunaMascota = vacunaService.registerForMascota(data.mascota().id(), new VacunaMascotaRequest(
				vacuna.id(),
				data.veterinario().id(),
				null,
				LocalDate.now(),
				null,
				LocalDate.now().plusDays(10),
				"Proxima dosis programada."
		), data.veterinario().id(), clinicaId());

		List<ReporteCitaResponse> citas = reporteService.findCitas(
				EstadoCita.PROGRAMADA,
				reportDate,
				reportDate,
				data.veterinario().id(),
				data.mascota().id(),
				data.duenio().id(),
				clinicaId()
		);
		ReporteCostoCitaResponse costo = reporteService.findCostoCita(cita.id(), clinicaId());
		List<ServicioSolicitadoResponse> servicios = reporteService.findServiciosMasSolicitados(reportDate, reportDate, clinicaId());
		List<VacunaMascotaResponse> vacunas = reporteService.findVacunasProximas(LocalDate.now(), LocalDate.now().plusDays(15), clinicaId());

		assertThat(citas).extracting(ReporteCitaResponse::id).contains(cita.id());
		assertThat(costo.total()).isEqualByComparingTo("120.00");
		assertThat(costo.detalles()).hasSize(2);
		assertThat(servicios).extracting(ServicioSolicitadoResponse::nombreServicio)
				.contains("Consulta general", "Vacuna rabia");
		assertThat(vacunas).extracting(VacunaMascotaResponse::id).contains(vacunaMascota.id());
	}

	@Test
	void reportNoShowsAndClinicalHistory() {
		TestData data = createBaseData();
		CitaResponse noShowCita = citaService.create(baseCitaRequest(data, nextDate(DayOfWeek.MONDAY)), clinicaId());
		moveCitaToPast(noShowCita.id());
		inasistenciaService.register(
				noShowCita.id(),
				new InasistenciaRequest("El duenio no asistio."),
				"asistente@test.com",
				clinicaId()
		);

		CitaResponse clinicalCita = citaService.create(baseCitaRequest(data, nextDate(DayOfWeek.MONDAY)), clinicaId());
		moveCitaToPast(clinicalCita.id());
		atencionClinicaService.register(clinicalCita.id(), new AtencionClinicaRequest(
				"Control por vomitos",
				"Gastritis leve",
				"Dieta blanda",
				"Retornar en 3 dias",
				"Paciente estable",
				null,
				EstadoMascota.PENDIENTE
		), auth);

		HistoriaClinicaResponse historia = reporteService.findHistoriaClinica(data.mascota().id(), clinicaId());

		assertThat(reporteService.findInasistencias(
				data.duenio().id(),
				LocalDate.now().minusDays(1),
				LocalDate.now(),
				clinicaId()
		)).hasSize(1);
		assertThat(historia.atenciones()).hasSize(1);
		assertThat(historia.mascotaId()).isEqualTo(data.mascota().id());
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
		), clinicaId());
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
		), clinicaId());
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
		), clinicaId());
		ServicioResponse consulta = servicioService.create(new ServicioRequest(
				"Consulta general",
				"Evaluacion clinica basica.",
				new BigDecimal("50.00")
		), clinicaId());
		ServicioResponse vacuna = servicioService.create(new ServicioRequest(
				"Vacuna rabia",
				"Aplicacion de vacuna.",
				new BigDecimal("80.00")
		), clinicaId());

		return new TestData(duenio, mascota, veterinario, consulta, vacuna);
	}

	private CitaRequest baseCitaRequest(TestData data, LocalDate fecha) {
		return new CitaRequest(
				data.duenio().id(),
				data.mascota().id(),
				data.veterinario().id(),
				fecha,
				LocalTime.of(9, 0),
				30,
				"Consulta preventiva",
				List.of(
						new CostoCitaServicioRequest(data.consulta().id(), 1),
						new CostoCitaServicioRequest(data.vacuna().id(), 1)
				),
				new BigDecimal("10.00")
		);
	}

	private void moveCitaToPast(Long citaId) {
		Cita cita = citaRepository.findById(citaId).orElseThrow();
		cita.setFecha(LocalDate.now().minusDays(1));
		cita.setHoraInicio(LocalTime.of(9, 0));
		cita.setHoraFin(LocalTime.of(9, 30));
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
			ServicioResponse consulta,
			ServicioResponse vacuna
	) {
	}
}
