package com.petcare.backend.domain.dto.response;

import java.time.LocalDate;
import java.util.List;

public record PanelAlertasDiaResponse(
		LocalDate fecha,
		Integer totalCitasProgramadasHoy,
		Integer totalCitasSinConfirmar,
		Integer totalCitasConfirmadasPendientesAtencion,
		Integer totalCitasNoAsistidasHoy,
		Integer totalVacunasProximas,
		Integer totalVacunasVencidas,
		Integer totalControlesMensualesPendientes,
		List<AlertaCitaResponse> citasProgramadasHoy,
		List<AlertaCitaResponse> citasSinConfirmar,
		List<AlertaCitaResponse> citasConfirmadasPendientesAtencion,
		List<AlertaCitaResponse> citasNoAsistidasHoy,
		List<AlertaVacunaResponse> vacunasProximas,
		List<AlertaVacunaResponse> vacunasVencidas,
		List<ControlMensualPendienteResponse> controlesMensualesPendientes
) {
}
