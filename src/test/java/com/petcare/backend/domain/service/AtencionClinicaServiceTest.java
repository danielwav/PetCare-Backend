package com.petcare.backend.domain.service;

import com.petcare.backend.domain.dto.request.AtencionClinicaRequest;
import com.petcare.backend.domain.dto.request.CitaRequest;
import com.petcare.backend.domain.dto.request.ControlMensualMascotaRequest;
import com.petcare.backend.domain.dto.request.CostoCitaServicioRequest;
import com.petcare.backend.domain.dto.request.DuenioRequest;
import com.petcare.backend.domain.dto.request.HorarioVeterinarioRequest;
import com.petcare.backend.domain.dto.request.MascotaRequest;
import com.petcare.backend.domain.dto.request.ServicioRequest;
import com.petcare.backend.domain.dto.request.VeterinarioRequest;
import com.petcare.backend.domain.dto.response.AtencionClinicaResponse;
import com.petcare.backend.domain.dto.response.CitaResponse;
import com.petcare.backend.domain.dto.response.ControlMensualMascotaResponse;
import com.petcare.backend.domain.dto.response.DuenioResponse;
import com.petcare.backend.domain.dto.response.HistoriaClinicaResponse;
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
class AtencionClinicaServiceTest {

	@Autowired
	private AtencionClinicaService atencionClinicaService;

	@Autowired
	private ControlMensualMascotaService controlMensualMascotaService;

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
	void registerClinicalAttentionAndAddToHistory() {
		TestData data = createBaseData();
		CitaResponse cita = citaService.create(baseCitaRequest(data, nextDate(DayOfWeek.MONDAY)));
		moveCitaToPast(cita.id());

		AtencionClinicaResponse atencion = atencionClinicaService.register(cita.id(), baseAtencionRequest());
		CitaResponse updatedCita = citaService.findById(cita.id());
		HistoriaClinicaResponse historia = atencionClinicaService.findHistoriaClinicaByMascota(data.mascota().id());

		assertThat(atencion.citaId()).isEqualTo(cita.id());
		assertThat(atencion.mascotaId()).isEqualTo(data.mascota().id());
		assertThat(atencion.diagnostico()).isEqualTo("Gastritis leve");
		assertThat(updatedCita.estado()).isEqualTo(EstadoCita.ATENDIDA);
		assertThat(updatedCita.requiereConfirmacion()).isFalse();
		assertThat(historia.atenciones()).extracting(AtencionClinicaResponse::id).contains(atencion.id());
	}

	@Test
	void rejectDuplicateOrFutureClinicalAttention() {
		TestData data = createBaseData();
		CitaResponse cita = citaService.create(baseCitaRequest(data, nextDate(DayOfWeek.MONDAY)));

		assertThatThrownBy(() -> atencionClinicaService.register(cita.id(), baseAtencionRequest()))
				.isInstanceOf(IllegalArgumentException.class);

		moveCitaToPast(cita.id());
		atencionClinicaService.register(cita.id(), baseAtencionRequest());

		assertThatThrownBy(() -> atencionClinicaService.register(cita.id(), baseAtencionRequest()))
				.isInstanceOf(IllegalArgumentException.class);
	}

	@Test
	void createUpdateAndListMonthlyControls() {
		TestData data = createBaseData();
		LocalDate controlDate = LocalDate.now().withDayOfMonth(1);

		ControlMensualMascotaResponse created = controlMensualMascotaService.create(
				data.mascota().id(),
				baseControlRequest(data.veterinario().id(), controlDate, "12.80")
		);
		ControlMensualMascotaResponse updated = controlMensualMascotaService.update(
				created.id(),
				baseControlRequest(data.veterinario().id(), controlDate, "13.10")
		);
		List<ControlMensualMascotaResponse> controls = controlMensualMascotaService.findByMascota(data.mascota().id());
		HistoriaClinicaResponse historia = atencionClinicaService.findHistoriaClinicaByMascota(data.mascota().id());

		assertThat(created.mes()).isEqualTo(controlDate.getMonthValue());
		assertThat(updated.pesoKg()).isEqualByComparingTo("13.10");
		assertThat(controls).extracting(ControlMensualMascotaResponse::id).contains(created.id());
		assertThat(historia.controlesMensuales()).extracting(ControlMensualMascotaResponse::id).contains(created.id());
	}

	@Test
	void rejectDuplicateMonthlyControlForSamePetAndMonth() {
		TestData data = createBaseData();
		LocalDate controlDate = LocalDate.now().withDayOfMonth(1);
		controlMensualMascotaService.create(
				data.mascota().id(),
				baseControlRequest(data.veterinario().id(), controlDate, "12.80")
		);

		assertThatThrownBy(() -> controlMensualMascotaService.create(
				data.mascota().id(),
				baseControlRequest(data.veterinario().id(), controlDate.plusDays(5), "13.00")
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

	private AtencionClinicaRequest baseAtencionRequest() {
		return new AtencionClinicaRequest(
				"Vomitos y falta de apetito",
				"Gastritis leve",
				"Dieta blanda y medicacion por 3 dias",
				"Retornar si los sintomas continuan",
				"Paciente estable",
				"Seguimiento telefonico recomendado"
		);
	}

	private ControlMensualMascotaRequest baseControlRequest(Long veterinarioId, LocalDate fecha, String pesoKg) {
		return new ControlMensualMascotaRequest(
				veterinarioId,
				fecha,
				new BigDecimal(pesoKg),
				"Alimentacion balanceada",
				"Buen estado general",
				"Mantener rutina de actividad"
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
