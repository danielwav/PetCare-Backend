package com.petcare.backend.domain.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateInternalUserRequest(
        @NotBlank @Size(max = 60) String nombres,
        @NotBlank @Size(max = 60) String apellidos,
        @NotBlank @Email @Size(max = 120) String email,
        @NotBlank String rol
) {
}
