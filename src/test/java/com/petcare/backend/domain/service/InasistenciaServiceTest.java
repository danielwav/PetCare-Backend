package com.petcare.backend.domain.service;

import com.petcare.backend.domain.dto.request.CitaRequest;
import com.petcare.backend.domain.dto.request.CostoCitaServicioRequest;
import com.petcare.backend.domain.dto.request.DuenioRequest;
import com.petcare.backend.domain.dto.request.HorarioVeterinarioRequest;
import com.petcare.backend.domain.dto.request.InasistenciaRequest;
import com.petcare.backend.domain.dto.request.MascotaRequest;
import com.petcare.backend.domain.dto.request.ServicioRequest;
import com.petcare.backend.domain.dto.request.VeterinarioRequest;
import com.petcare.backend.domain.dto.response.CitaResponse;
import com.petcare.backend.domain.dto.response.DuenioResponse;
import com.petcare.backend.domain.dto.response.InasistenciaResponse;
import com.petcare.backend.domain.dto.response.MascotaResponse;
import com.petcare.backend.domain.dto.response.ServicioResponse;
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
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
class InasistenciaServiceTest {

	@Autowired
	private InasistenciaService inasistenciaService;

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
	private CitaRepository citaRepository;

	@Test
	void registerNoShowAndSearchByDuenioAndDateRange() {
		TestData data = createBaseData();
		CitaResponse cita = citaService.create(baseCitaRequest(data, nextDate(DayOfWeek.MONDAY)));
		moveCitaToPast(cita.id());

		InasistenciaResponse registered = inasistenciaService.register(
				cita.id(),
				new InasistenciaRequest("El duenio no se presento a la cita."),
				"asistente@test.com"
		);
		CitaResponse updatedCita = citaService.findById(cita.id());
		List<InasistenciaResponse> results = inasistenciaService.findAll(
				data.duenio().id(),
				LocalDate.now().minusDays(2),
				LocalDate.now()
		);

		assertThat(registered.citaId()).isEqualTo(cita.id());
		assertThat(registered.duenioId()).isEqualTo(data.duenio().id());
		assertThat(registered.mascotaNombre()).isEqualTo("Firulais");
		assertThat(registered.registradoPor()).isEqualTo("asistente@test.com");
		assertThat(updatedCita.estado()).isEqualTo(EstadoCita.NO_ASISTIO);
		assertThat(updatedCita.requiereConfirmacion()).isFalse();
		assertThat(results).extracting(InasistenciaResponse::id).contains(registered.id());
	}

	@Test
	void rejectDuplicateNoShowForSameCita() {
		TestData data = createBaseData();
		CitaResponse cita = citaService.create(baseCitaRequest(data, nextDate(DayOfWeek.MONDAY)));
		moveCitaToPast(cita.id());
		inasistenciaService.register(
				cita.id(),
				new InasistenciaRequest("Primera marca de inasistencia."),
				"asistente@test.com"
		);

		assertThatThrownBy(() -> inasistenciaService.register(
				cita.id(),
				new InasistenciaRequest("Registro duplicado."),
				"asistente@test.com"
		)).isInstanceOf(IllegalArgumentException.class);
	}

	@Test
	void rejectNoShowBeforeAppointmentTime() {
		TestData data = createBaseData();
		CitaResponse cita = citaService.create(baseCitaRequest(data, nextDate(DayOfWeek.MONDAY)));

		assertThatThrownBy(() -> inasistenciaService.register(
				cita.id(),
				new InasistenciaRequest("Aun no llega la hora de la cita."),
				"asistente@test.com"
		)).isInstanceOf(IllegalArgumentException.class);
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

	private CitaRequest baseCitaRequest(TestData data, LocalDate fecha) {
		return new CitaRequest(
				data.duenio().id(),
				data.mascota().id(),
				data.veterinario().id(),
				fecha,
				LocalTime.of(9, 0),
				30,
				"Consulta preventiva",
				List.of(new CostoCitaServicioRequest(data.consulta().id(), 1)),
				BigDecimal.ZERO
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
			ServicioResponse consulta
	) {
	}
}
