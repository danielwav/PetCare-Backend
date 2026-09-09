package com.petcare.backend.domain.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record UpdateClinicaRequest(
		@NotBlank @Size(max = 120) String nombre,
		@NotBlank @Size(max = 80) @Pattern(regexp = "^[a-z0-9-]+$", message = "El slug solo admite minusculas, numeros y guiones") String slug,
		@Size(max = 250) String direccion,
		@Size(max = 20) @Pattern(regexp = "^[+\\d\\s-]*$", message = "Número de teléfono inválido") String telefono,
		@Size(max = 250) String horarioAtencion,
		@Size(max = 1000) String descripcion,
		@Size(max = 500) String logoUrl
) {
}