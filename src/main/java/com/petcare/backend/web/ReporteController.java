package com.petcare.backend.web;

import com.petcare.backend.domain.dto.response.HistoriaClinicaResponse;
import com.petcare.backend.domain.dto.response.InasistenciaResponse;
import com.petcare.backend.domain.dto.response.ReporteCitaResponse;
import com.petcare.backend.domain.dto.response.ReporteCostoCitaResponse;
import com.petcare.backend.domain.dto.response.ReporteServicioResponse;
import com.petcare.backend.domain.dto.response.ServicioSolicitadoResponse;
import com.petcare.backend.domain.dto.response.VacunaMascotaResponse;
import com.petcare.backend.domain.service.ReporteService;
import com.petcare.backend.persistence.enums.EstadoCita;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequiredArgsConstructor
public class ReporteController {

	private final ReporteService reporteService;

	@GetMapping("/api/reportes/citas")
	public List<ReporteCitaResponse> findCitas(
			@RequestParam(required = false) EstadoCita estado,
			@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaInicio,
			@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaFin,
			@RequestParam(required = false) Long veterinarioId,
			@RequestParam(required = false) Long mascotaId,
			@RequestParam(required = false) Long duenioId
	) {
		return reporteService.findCitas(estado, fechaInicio, fechaFin, veterinarioId, mascotaId, duenioId);
	}

	@GetMapping("/api/reportes/inasistencias")
	public List<InasistenciaResponse> findInasistencias(
			@RequestParam(required = false) Long duenioId,
			@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaInicio,
			@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaFin
	) {
		return reporteService.findInasistencias(duenioId, fechaInicio, fechaFin);
	}

	@GetMapping("/api/reportes/vacunas-proximas")
	public List<VacunaMascotaResponse> findVacunasProximas(
			@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaInicio,
			@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaFin
	) {
		return reporteService.findVacunasProximas(fechaInicio, fechaFin);
	}

	@GetMapping("/api/reportes/citas/{id}/costos")
	public ReporteCostoCitaResponse findCostoCita(@PathVariable Long id) {
		return reporteService.findCostoCita(id);
	}

	@GetMapping("/api/reportes/servicios")
	public ReporteServicioResponse findReporteServicios() {
		return reporteService.findReporteServicios();
	}

	@GetMapping("/api/reportes/servicios-mas-solicitados")
	public List<ServicioSolicitadoResponse> findServiciosMasSolicitados(
			@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaInicio,
			@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaFin
	) {
		return reporteService.findServiciosMasSolicitados(fechaInicio, fechaFin);
	}

	@GetMapping("/api/reportes/mascotas/{id}/historia-clinica")
	public HistoriaClinicaResponse findHistoriaClinica(@PathVariable Long id) {
		return reporteService.findHistoriaClinica(id);
	}
}
