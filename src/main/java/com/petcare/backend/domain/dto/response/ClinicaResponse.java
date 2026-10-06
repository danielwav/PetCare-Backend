package com.petcare.backend.domain.dto.response;

import java.time.LocalDateTime;

public record ClinicaResponse(
		Long id,
		String nombre,
		String slug,
		String plan,
		String estado,
		String direccion,
		String telefono,
		String horarioAtencion,
		String descripcion,
		String logoUrl,
		LocalDateTime createdAt,
		LocalDateTime trialStartedAt,
		LocalDateTime trialEndsAt,
		boolean readOnly
) {
}
