package com.petcare.backend.domain.dto.response;

import java.util.List;

public record ClinicaDirectoryResponse(
		Long id,
		String nombre,
		String slug,
		String direccion,
		String telefono,
		String horarioAtencion,
		String descripcion,
		String logoUrl,
		List<String> servicios
) {
}
