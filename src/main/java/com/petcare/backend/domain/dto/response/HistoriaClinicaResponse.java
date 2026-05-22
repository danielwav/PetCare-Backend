package com.petcare.backend.domain.dto.response;

import java.util.List;

public record HistoriaClinicaResponse(
		Long mascotaId,
		String mascotaNombre,
		Long duenioId,
		String duenioNombreCompleto,
		List<AtencionClinicaResponse> atenciones,
		List<ControlMensualMascotaResponse> controlesMensuales
) {
}
