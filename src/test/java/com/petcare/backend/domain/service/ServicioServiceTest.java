package com.petcare.backend.domain.service;

import com.petcare.backend.domain.dto.request.CalculoCostoCitaRequest;
import com.petcare.backend.domain.dto.request.CostoCitaServicioRequest;
import com.petcare.backend.domain.dto.request.ServicioRequest;
import com.petcare.backend.domain.dto.response.CalculoCostoCitaResponse;
import com.petcare.backend.domain.dto.response.ServicioResponse;
import com.petcare.backend.domain.repository.ClinicaRepository;
import com.petcare.backend.persistence.entity.Clinica;
import com.petcare.backend.persistence.enums.EstadoClinica;
import com.petcare.backend.persistence.enums.PlanClinica;
import org.springframework.security.access.AccessDeniedException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
class ServicioServiceTest {

	@Autowired
	private ServicioService servicioService;

	@Autowired
	private ClinicaService clinicaService;

	@Autowired
	private ClinicaRepository clinicaRepository;

	private Long clinicaId() {
		return clinicaService.getOrCreateDefaultClinic().getId();
	}

	@Test
	void createFindUpdateAndDeactivateServicio() {
		ServicioResponse created = servicioService.create(baseRequest(
				"Consulta general",
				"Evaluacion clinica basica.",
				"50.00"
		), clinicaId());

		ServicioResponse found = servicioService.findById(created.id(), clinicaId());
		ServicioResponse updated = servicioService.update(created.id(), baseRequest(
				"Consulta veterinaria",
				"Evaluacion clinica completa.",
				"65.50"
		), clinicaId());

		servicioService.deactivate(created.id(), clinicaId());
		ServicioResponse inactive = servicioService.findById(created.id(), clinicaId());

		assertThat(found.nombre()).isEqualTo("Consulta general");
		assertThat(found.active()).isTrue();
		assertThat(updated.nombre()).isEqualTo("Consulta veterinaria");
		assertThat(updated.costoBase()).isEqualByComparingTo("65.50");
		assertThat(inactive.active()).isFalse();
	}

	@Test
	void searchActiveServiciosAndRejectDuplicatedName() {
		servicioService.create(baseRequest("Vacunacion", "Aplicacion de vacuna.", "80.00"), clinicaId());
		servicioService.create(baseRequest("Bano medicado", "Servicio dermatologico.", "45.00"), clinicaId());

		List<ServicioResponse> results = servicioService.findAll("vacuna", null, clinicaId());

		assertThat(results).hasSize(1);
		assertThat(results.getFirst().nombre()).isEqualTo("Vacunacion");
		assertThatThrownBy(() -> servicioService.create(baseRequest(
				"vacunacion",
				"Nombre repetido.",
				"90.00"
		), clinicaId())).isInstanceOf(IllegalArgumentException.class);
	}

	@Test
	void calculateAppointmentCostWithDiscount() {
		ServicioResponse consulta = servicioService.create(baseRequest(
				"Consulta general",
				"Evaluacion clinica basica.",
				"50.00"
		), clinicaId());
		ServicioResponse vacuna = servicioService.create(baseRequest(
				"Vacuna rabia",
				"Aplicacion de vacuna antirrabica.",
				"80.00"
		), clinicaId());

		CalculoCostoCitaResponse response = servicioService.calculateCost(new CalculoCostoCitaRequest(
				List.of(
						new CostoCitaServicioRequest(consulta.id(), 1),
						new CostoCitaServicioRequest(vacuna.id(), 2)
				),
				new BigDecimal("10.00")
		), clinicaId());

		assertThat(response.detalles()).hasSize(2);
		assertThat(response.subtotal()).isEqualByComparingTo("210.00");
		assertThat(response.descuento()).isEqualByComparingTo("10.00");
		assertThat(response.total()).isEqualByComparingTo("200.00");
	}

	@Test
	void rejectCostCalculationWithInactiveServiceOrInvalidDiscount() {
		ServicioResponse servicio = servicioService.create(baseRequest(
				"Emergencia",
				"Atencion prioritaria.",
				"120.00"
		), clinicaId());

		assertThatThrownBy(() -> servicioService.calculateCost(new CalculoCostoCitaRequest(
				List.of(new CostoCitaServicioRequest(servicio.id(), 1)),
				new BigDecimal("121.00")
		), clinicaId())).isInstanceOf(IllegalArgumentException.class);

		servicioService.deactivate(servicio.id(), clinicaId());

		assertThatThrownBy(() -> servicioService.calculateCost(new CalculoCostoCitaRequest(
				List.of(new CostoCitaServicioRequest(servicio.id(), 1)),
				BigDecimal.ZERO
		), clinicaId())).isInstanceOf(IllegalArgumentException.class);
	}

	@Test
	void isolateCatalogAndAllowSameNameAcrossClinics() {
		Long firstClinic = clinicaId();
		LocalDateTime now = LocalDateTime.now();
		Long secondClinic = clinicaRepository.save(Clinica.builder()
				.nombre("Segunda clinica").slug("segunda").plan(PlanClinica.TRIAL)
				.estado(EstadoClinica.ACTIVA).createdAt(now).updatedAt(now).build()).getId();
		ServicioRequest request = baseRequest("Consulta", "Consulta veterinaria", "50.00");
		ServicioResponse first = servicioService.create(request, firstClinic);
		ServicioResponse second = servicioService.create(request, secondClinic);

		assertThat(servicioService.findAll(null, null, firstClinic))
				.extracting(ServicioResponse::id).containsExactly(first.id());
		assertThat(servicioService.findAll(null, null, null)).isEmpty();
		assertThatThrownBy(() -> servicioService.findById(second.id(), firstClinic))
				.isInstanceOf(AccessDeniedException.class);
		assertThatThrownBy(() -> servicioService.update(second.id(), request, firstClinic))
				.isInstanceOf(AccessDeniedException.class);
		assertThatThrownBy(() -> servicioService.deactivate(second.id(), firstClinic))
				.isInstanceOf(AccessDeniedException.class);
		assertThatThrownBy(() -> servicioService.activate(second.id(), firstClinic))
				.isInstanceOf(AccessDeniedException.class);
		assertThatThrownBy(() -> servicioService.calculateCost(new CalculoCostoCitaRequest(
				List.of(new CostoCitaServicioRequest(second.id(), 1)), BigDecimal.ZERO), firstClinic))
				.isInstanceOf(AccessDeniedException.class);
	}

	private ServicioRequest baseRequest(String nombre, String descripcion, String costoBase) {
		return new ServicioRequest(nombre, descripcion, new BigDecimal(costoBase));
	}
}
