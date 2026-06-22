package com.petcare.backend.domain.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.List;

public record VeterinarioRequest(
		Long usuarioId,
		@Size(max = 80) String nombres,
		@Size(max = 80) String apellidos,
		@NotBlank @Size(max = 30) String numeroColegiatura,
		@NotBlank @Size(max = 100) String especialidad,
		@Size(max = 20) String telefono,
		@Size(max = 120) String email,
		List<@Valid HorarioVeterinarioRequest> horarios
) {
}
