package com.petcare.backend.domain.dto.response;

public record ControlMensualPendienteResponse(
		Long mascotaId,
		String mascotaNombre,
		Long duenioId,
		String duenioNombreCompleto,
		Integer anio,
		Integer mes
) {
}
