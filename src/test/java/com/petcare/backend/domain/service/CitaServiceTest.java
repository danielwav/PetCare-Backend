package com.petcare.backend.domain.service;

import com.petcare.backend.domain.dto.request.CitaRequest;
import com.petcare.backend.domain.dto.request.CostoCitaServicioRequest;
import com.petcare.backend.domain.dto.request.DuenioRequest;
import com.petcare.backend.domain.dto.request.HorarioVeterinarioRequest;
import com.petcare.backend.domain.dto.request.MascotaRequest;
import com.petcare.backend.domain.dto.request.RegisterRequest;
import com.petcare.backend.domain.dto.request.ServicioRequest;
import com.petcare.backend.domain.dto.request.VeterinarioRequest;
import com.petcare.backend.domain.dto.response.AuthResponse;
import com.petcare.backend.domain.dto.response.CitaResponse;
import com.petcare.backend.domain.dto.response.DuenioResponse;
import com.petcare.backend.domain.dto.response.MascotaResponse;
import com.petcare.backend.domain.dto.response.ServicioResponse;
import com.petcare.backend.domain.dto.response.VeterinarioResponse;
import com.petcare.backend.domain.repository.UsuarioRepository;
import com.petcare.backend.domain.repository.CitaRepository;
import com.petcare.backend.domain.repository.HorarioVeterinarioRepository;
import com.petcare.backend.persistence.entity.HorarioVeterinario;
import com.petcare.backend.persistence.enums.EstadoCita;
import com.petcare.backend.persistence.enums.SexoMascota;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.MockedStatic;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import com.petcare.backend.web.ApiExceptionHandler;
import com.petcare.backend.web.CitaController;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.CALLS_REAL_METHODS;
import static org.mockito.Mockito.mockStatic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
class CitaServiceTest {

	@Autowired
	private UsuarioRepository usuarioRepository;

	@Autowired
	private CitaService citaService;

	@Autowired
	private ClinicaService clinicaService;

	@Autowired
	private AuthService authService;

	@Autowired
	private DuenioService duenioService;

	@Autowired
	private MascotaService mascotaService;

	@Autowired
	private VeterinarioService veterinarioService;

	@Autowired
	private ServicioService servicioService;

	@Autowired
	private CitaRepository citaRepository;

	@Autowired
	private HorarioVeterinarioRepository horarioVeterinarioRepository;

	@Test
	void createFindUpdateAndCancelCita() {
		TestData data = createBaseData();
		LocalDate nextMonday = nextDate(DayOfWeek.MONDAY);

		CitaResponse created = citaService.create(baseCitaRequest(data, nextMonday, LocalTime.of(9, 0), "Consulta preventiva"), clinicaId());
		CitaResponse found = citaService.findById(created.id(), clinicaId());
		CitaResponse updated = citaService.update(created.id(), baseCitaRequest(
				data,
				nextMonday,
				LocalTime.of(10, 0),
				"Consulta y vacuna"
		), clinicaId());
		CitaResponse canceled = citaService.cancel(created.id(), clinicaId());

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
		citaService.create(baseCitaRequest(data, nextMonday, LocalTime.of(9, 0), "Control general"), clinicaId());

		List<CitaResponse> results = citaService.findAll(
				EstadoCita.PROGRAMADA,
				nextMonday,
				data.duenio().id(),
				data.mascota().id(),
				data.veterinario().id(),
				clinicaId()
		);

		assertThat(results).hasSize(1);
		assertThat(results.getFirst().mascotaNombre()).isEqualTo("Firulais");
	}

	@Test
	void rejectOverlappingAppointment() {
		TestData data = createBaseData();
		LocalDate nextMonday = nextDate(DayOfWeek.MONDAY);
		citaService.create(baseCitaRequest(data, nextMonday, LocalTime.of(9, 0), "Control general"), clinicaId());

		assertThatThrownBy(() -> citaService.create(baseCitaRequest(
				data,
				nextMonday,
				LocalTime.of(9, 15),
				"Cita cruzada"
		), clinicaId())).isInstanceOf(IllegalArgumentException.class);
	}

	@Test
	void availabilityExcludesOverlapsAllowsAdjacencyAndCancellationFreesSlots() {
		TestData data = createBaseData();
		LocalDate date = nextDate(DayOfWeek.MONDAY);
		CitaResponse booked = citaService.create(baseCitaRequest(data, date, LocalTime.of(9, 30), "Reservada"), clinicaId());

		for (EstadoCita estado : List.of(EstadoCita.PROGRAMADA, EstadoCita.CONFIRMADA, EstadoCita.ATENDIDA, EstadoCita.NO_ASISTIO)) {
			var entity = citaRepository.findById(booked.id()).orElseThrow();
			entity.setEstado(estado);
			citaRepository.save(entity);
			assertThat(veterinarioService.findDisponibilidad(data.veterinario().id(), date, 30, clinicaId())
					.horariosDisponibles()).containsExactly(LocalTime.of(9, 0), LocalTime.of(10, 0), LocalTime.of(10, 30));
		}
		var activeBooking = citaRepository.findById(booked.id()).orElseThrow();
		activeBooking.setEstado(EstadoCita.PROGRAMADA);
		citaRepository.save(activeBooking);
		assertThat(veterinarioService.findDisponibilidad(data.veterinario().id(), date, 60, clinicaId())
				.horariosDisponibles()).containsExactly(LocalTime.of(10, 0));
		assertThat(veterinarioService.findDisponibilidad(data.veterinario().id(), date, 45, clinicaId())
				.horariosDisponibles()).containsExactly(LocalTime.of(10, 0));
		assertThat(veterinarioService.findDisponibilidad(data.veterinario().id(), date, 15, clinicaId())
				.horariosDisponibles()).containsExactly(LocalTime.of(9, 0), LocalTime.of(10, 0), LocalTime.of(10, 30));

		CitaResponse before = citaService.create(baseCitaRequest(data, date, LocalTime.of(9, 0), "Adyacente anterior"), clinicaId());
		CitaResponse after = citaService.create(baseCitaRequest(data, date, LocalTime.of(10, 0), "Adyacente posterior"), clinicaId());
		citaService.cancel(booked.id(), clinicaId());
		assertThat(veterinarioService.findDisponibilidad(data.veterinario().id(), date, null, clinicaId())
				.horariosDisponibles()).containsExactly(LocalTime.of(9, 30), LocalTime.of(10, 30));
		CitaResponse replacement = citaService.create(baseCitaRequest(data, date, LocalTime.of(9, 30), "Reemplazo"), clinicaId());
		assertThat(replacement.horaFin()).isEqualTo(LocalTime.of(10, 0));
		assertThat(before.horaFin()).isEqualTo(booked.horaInicio());
		assertThat(after.horaInicio()).isEqualTo(booked.horaFin());
	}

	@Test
	void availabilityAndCreationDenyVeterinariansFromAnotherClinic() {
		TestData data = createBaseData();
		Long otherClinicId = clinicaService.createClinic("Otra clinica", "otra-clinica").getId();
		VeterinarioResponse foreign = veterinarioService.create(new VeterinarioRequest(
				ServiceTestFixtures.veterinarioUser(authService, usuarioRepository, otherClinicId, "foreign.vet@test.com"),
				"Otra", "Veterinaria", "CMVP-FOREIGN", "Medicina general", "999777666", "foreign.vet@test.com",
				List.of(new HorarioVeterinarioRequest(DayOfWeek.MONDAY, LocalTime.of(9, 0), LocalTime.of(11, 0), 30))
		), otherClinicId);
		LocalDate date = nextDate(DayOfWeek.MONDAY);
		CitaRequest request = new CitaRequest(data.duenio().id(), data.mascota().id(), foreign.id(), date,
				LocalTime.of(9, 0), 30, "Otra clinica", List.of(new CostoCitaServicioRequest(data.consulta().id(), 1)), BigDecimal.ZERO);

		assertThatThrownBy(() -> veterinarioService.findDisponibilidad(foreign.id(), date, 30, clinicaId()))
				.isInstanceOf(AccessDeniedException.class);
		assertThatThrownBy(() -> veterinarioService.findDisponibilidad(data.veterinario().id(), date, 30, otherClinicId))
				.isInstanceOf(AccessDeniedException.class);
		assertThatThrownBy(() -> citaService.create(request, clinicaId())).isInstanceOf(AccessDeniedException.class);
		assertThat(citaRepository.count()).isZero();
	}

	@Test
	void rejectElapsedTimeTodayButAllowFutureSameDayAppointment() {
		TestData data = createBaseData();
		LocalDate today = LocalDate.now();
		HorarioVeterinario horario = horarioVeterinarioRepository
				.findByVeterinarioIdAndDiaSemanaAndActiveTrueOrderByHoraInicioAsc(data.veterinario().id(), DayOfWeek.MONDAY)
				.getFirst();
		horario.setDiaSemana(today.getDayOfWeek());
		horarioVeterinarioRepository.save(horario);
		LocalDateTime now = today.atTime(9, 15);

		try (MockedStatic<LocalDateTime> time = mockStatic(LocalDateTime.class, CALLS_REAL_METHODS)) {
			time.when(LocalDateTime::now).thenReturn(now);
			assertThatThrownBy(() -> citaService.create(baseCitaRequest(data, today, LocalTime.of(9, 0), "Vencida"), clinicaId()))
					.isInstanceOf(IllegalArgumentException.class).hasMessageContaining("futura");
			assertThat(citaService.create(baseCitaRequest(data, today, LocalTime.of(9, 30), "Futura"), clinicaId())
					.horaFin()).isEqualTo(LocalTime.of(10, 0));
		}
	}

	@ParameterizedTest
	@CsvSource({"23:45,15", "23:30,60", "09:00,1440", "09:00,1470", "23:30,2147483647", "09:00,14"})
	void rejectMidnightAndOversizedDurationsOnCreateUpdateAndHttp(LocalTime start, int duration) throws Exception {
		TestData data = createBaseData();
		LocalDate date = nextDate(DayOfWeek.MONDAY);
		CitaResponse existing = citaService.create(baseCitaRequest(data, date, LocalTime.of(10, 0), "Valida"), clinicaId());
		CitaRequest invalid = new CitaRequest(data.duenio().id(), data.mascota().id(), data.veterinario().id(),
				date, start, duration, "Duracion invalida", List.of(new CostoCitaServicioRequest(data.consulta().id(), 1)), BigDecimal.ZERO);

		assertThatThrownBy(() -> citaService.create(invalid, clinicaId())).isInstanceOf(IllegalArgumentException.class);
		assertThatThrownBy(() -> citaService.update(existing.id(), invalid, clinicaId())).isInstanceOf(IllegalArgumentException.class);
		assertThat(citaService.findById(existing.id(), clinicaId()).horaInicio()).isEqualTo(LocalTime.of(10, 0));

		MockMvc mvc = MockMvcBuilders.standaloneSetup(new CitaController(citaService, clinicaService))
				.setControllerAdvice(new ApiExceptionHandler()).build();
		mvc.perform(post("/api/citas")
				.principal(new UsernamePasswordAuthenticationToken("ana.vet@test.com", "unused",
						List.of(new SimpleGrantedAuthority("ROLE_VETERINARIO"))))
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{"duenioId":%d,"mascotaId":%d,"veterinarioId":%d,"fecha":"%s",
						"horaInicio":"%s","duracionMinutos":%d,"motivo":"Duracion invalida",
						"servicios":[{"servicioId":%d,"cantidad":1}],"descuento":0}
						""".formatted(data.duenio().id(), data.mascota().id(), data.veterinario().id(),
						date, start, duration, data.consulta().id())))
				.andExpect(status().isBadRequest());
		assertThat(citaRepository.count()).isEqualTo(1);
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
		), clinicaId())).isInstanceOf(IllegalArgumentException.class);
	}

	@Test
	void confirmCitaAndRemoveFromConfirmationAlerts() {
		TestData data = createBaseData();
		LocalDate nextMonday = nextDate(DayOfWeek.MONDAY);
		CitaResponse created = citaService.create(baseCitaRequest(data, nextMonday, LocalTime.of(9, 0), "Control general"), clinicaId());

		List<CitaResponse> alertsBefore = citaService.findConfirmationAlerts(24 * 8, clinicaId());
		CitaResponse confirmed = citaService.confirm(created.id(), "asistente@test.com", clinicaId());
		List<CitaResponse> alertsAfter = citaService.findConfirmationAlerts(24 * 8, clinicaId());

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
		CitaResponse created = citaService.create(baseCitaRequest(data, nextMonday, LocalTime.of(9, 0), "Control general"), clinicaId());
		citaService.cancel(created.id(), clinicaId());

		assertThatThrownBy(() -> citaService.confirm(created.id(), "asistente@test.com", clinicaId()))
				.isInstanceOf(IllegalArgumentException.class);
	}

	@Test
	void duenioCanOnlyConsultAndConfirmOwnCitas() {
		AuthResponse ownerUser = authService.register(new RegisterRequest("Owner", "owner.cita@test.com", "secret123", "000000000"));
		AuthResponse otherUser = authService.register(new RegisterRequest("Other", "other.cita@test.com", "secret123", "000000000"));
		TestData ownerData = createBaseData("12345670", "owner.cita.profile@test.com", ownerUser.user().id(), "Lola", "CMVP-010", "vet10@test.com", "Consulta owner");
		TestData otherData = createBaseData("12345671", "other.cita.profile@test.com", otherUser.user().id(), "Toby", "CMVP-011", "vet11@test.com", "Consulta other");
		LocalDate nextMonday = nextDate(DayOfWeek.MONDAY);
		CitaResponse ownCita = citaService.createAsDuenio(
				baseCitaRequest(ownerData, nextMonday, LocalTime.of(9, 0), "Control propio"),
				ownerUser.user().email()
		);
		CitaResponse cancelableCita = citaService.createAsDuenio(
				baseCitaRequest(ownerData, nextMonday, LocalTime.of(9, 30), "Control cancelable"),
				ownerUser.user().email()
		);
		CitaResponse otherCita = citaService.create(baseCitaRequest(otherData, nextMonday, LocalTime.of(9, 0), "Control ajeno"), clinicaId());

		List<CitaResponse> ownCitas = citaService.findAllForDuenio(ownerUser.user().email(), null, nextMonday, null, null);
		CitaResponse confirmed = citaService.confirmAsDuenio(ownCita.id(), ownerUser.user().email());
		CitaResponse canceled = citaService.cancelAsDuenio(cancelableCita.id(), ownerUser.user().email());

		assertThat(ownCitas).extracting(CitaResponse::id).containsExactly(ownCita.id(), cancelableCita.id());
		assertThat(confirmed.estado()).isEqualTo(EstadoCita.CONFIRMADA);
		assertThat(canceled.estado()).isEqualTo(EstadoCita.CANCELADA);
		assertThatThrownBy(() -> citaService.createAsDuenio(
				baseCitaRequest(otherData, nextMonday, LocalTime.of(9, 30), "Control ajeno"),
				ownerUser.user().email()
		)).isInstanceOf(AccessDeniedException.class);
		assertThatThrownBy(() -> citaService.findByIdForDuenio(otherCita.id(), ownerUser.user().email()))
				.isInstanceOf(AccessDeniedException.class);
		assertThatThrownBy(() -> citaService.cancelAsDuenio(otherCita.id(), ownerUser.user().email()))
				.isInstanceOf(AccessDeniedException.class);
		assertThatThrownBy(() -> citaService.confirmAsDuenio(otherCita.id(), ownerUser.user().email()))
				.isInstanceOf(AccessDeniedException.class);
	}

	private TestData createBaseData() {
		return createBaseData("12345678", "daniel@test.com", null, "Firulais", "CMVP-001", "ana.vet@test.com", "Consulta general");
	}

	private Long clinicaId() {
		return clinicaService.getOrCreateDefaultClinic().getId();
	}

	private TestData createBaseData(
			String documento,
			String duenioEmail,
			Long usuarioId,
			String mascotaNombre,
			String numeroColegiatura,
			String veterinarioEmail,
			String servicioNombre
	) {
		DuenioRequest duenioRequest = new DuenioRequest(
				usuarioId,
				"Daniel",
				"Torres",
				"DNI",
				documento,
				"999888777",
				duenioEmail,
				"Av. Siempre Viva 123"
		);
		DuenioResponse duenio = usuarioId == null
				? duenioService.create(duenioRequest, clinicaId())
				: duenioService.update(duenioService.findOwn(authService.meById(usuarioId).email()).id(),
						duenioRequest, clinicaId());
		MascotaResponse mascota = mascotaService.create(new MascotaRequest(
				duenio.id(),
				mascotaNombre,
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
				ServiceTestFixtures.veterinarioUser(authService, usuarioRepository, clinicaId(), veterinarioEmail),
				"Ana",
				"Salas",
				numeroColegiatura,
				"Medicina general",
				"999777666",
				veterinarioEmail,
				List.of(new HorarioVeterinarioRequest(
						DayOfWeek.MONDAY,
						LocalTime.of(9, 0),
						LocalTime.of(11, 0),
						30
				))
		), clinicaId());
		ServicioResponse consulta = servicioService.create(new ServicioRequest(
				servicioNombre,
				"Evaluacion clinica basica.",
				new BigDecimal("50.00")
		), clinicaId());
		ServicioResponse vacuna = servicioService.create(new ServicioRequest(
				servicioNombre + " vacuna",
				"Aplicacion de vacuna antirrabica.",
				new BigDecimal("80.00")
		), clinicaId());

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
