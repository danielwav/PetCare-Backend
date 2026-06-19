package com.petcare.backend.domain.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record SetPasswordRequest(
    @NotBlank String token,
    @NotBlank @Size(min = 8) @Pattern(regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d).+$", message = "La contraseña debe tener al menos 8 caracteres, una mayúscula, una minúscula y un número")
    String password
) {}
