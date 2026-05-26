package com.petcare.backend.domain.service;

import com.petcare.backend.domain.dto.request.CalculoCostoCitaRequest;
import com.petcare.backend.domain.dto.request.CostoCitaServicioRequest;
import com.petcare.backend.domain.dto.request.ServicioRequest;
import com.petcare.backend.domain.dto.response.CalculoCostoCitaResponse;
import com.petcare.backend.domain.dto.response.ServicioResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
class ServicioServiceTest {

	@Autowired
	private ServicioService servicioService;

	@Test
	void createFindUpdateAndDeactivateServicio() {
		ServicioResponse created = servicioService.create(baseRequest(
				"Consulta general",
				"Evaluacion clinica basica.",
				"50.00"
		));

		ServicioResponse found = servicioService.findById(created.id());
		ServicioResponse updated = servicioService.update(created.id(), baseRequest(
				"Consulta veterinaria",
				"Evaluacion clinica completa.",
				"65.50"
		));

		servicioService.deactivate(created.id());
		ServicioResponse inactive = servicioService.findById(created.id());

		assertThat(found.nombre()).isEqualTo("Consulta general");
		assertThat(found.active()).isTrue();
		assertThat(updated.nombre()).isEqualTo("Consulta veterinaria");
		assertThat(updated.costoBase()).isEqualByComparingTo("65.50");
		assertThat(inactive.active()).isFalse();
	}

	@Test
	void searchActiveServiciosAndRejectDuplicatedName() {
		servicioService.create(baseRequest("Vacunacion", "Aplicacion de vacuna.", "80.00"));
		servicioService.create(baseRequest("Bano medicado", "Servicio dermatologico.", "45.00"));

		List<ServicioResponse> results = servicioService.findAll("vacuna", null);

		assertThat(results).hasSize(1);
		assertThat(results.getFirst().nombre()).isEqualTo("Vacunacion");
		assertThatThrownBy(() -> servicioService.create(baseRequest(
				"vacunacion",
				"Nombre repetido.",
				"90.00"
		))).isInstanceOf(IllegalArgumentException.class);
	}

	@Test
	void calculateAppointmentCostWithDiscount() {
		ServicioResponse consulta = servicioService.create(baseRequest(
				"Consulta general",
				"Evaluacion clinica basica.",
				"50.00"
		));
		ServicioResponse vacuna = servicioService.create(baseRequest(
				"Vacuna rabia",
				"Aplicacion de vacuna antirrabica.",
				"80.00"
		));

		CalculoCostoCitaResponse response = servicioService.calculateCost(new CalculoCostoCitaRequest(
				List.of(
						new CostoCitaServicioRequest(consulta.id(), 1),
						new CostoCitaServicioRequest(vacuna.id(), 2)
				),
				new BigDecimal("10.00")
		));

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
		));

		assertThatThrownBy(() -> servicioService.calculateCost(new CalculoCostoCitaRequest(
				List.of(new CostoCitaServicioRequest(servicio.id(), 1)),
				new BigDecimal("121.00")
		))).isInstanceOf(IllegalArgumentException.class);

		servicioService.deactivate(servicio.id());

		assertThatThrownBy(() -> servicioService.calculateCost(new CalculoCostoCitaRequest(
				List.of(new CostoCitaServicioRequest(servicio.id(), 1)),
				BigDecimal.ZERO
		))).isInstanceOf(IllegalArgumentException.class);
	}

	private ServicioRequest baseRequest(String nombre, String descripcion, String costoBase) {
		return new ServicioRequest(nombre, descripcion, new BigDecimal(costoBase));
	}
}
