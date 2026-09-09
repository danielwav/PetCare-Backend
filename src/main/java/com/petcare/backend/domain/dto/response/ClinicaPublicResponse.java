package com.petcare.backend.domain.dto.response;

public record ClinicaPublicResponse(
		Long id,
		String nombre,
		String slug,
		String direccion,
		String telefono,
		String horarioAtencion,
		String descripcion,
		String logoUrl
) {
}