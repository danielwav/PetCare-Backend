package com.petcare.backend.domain.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record ChangePasswordRequest(
    @NotBlank @Size(min = 8, message = "La contraseña debe tener al menos 8 caracteres")
    @Pattern(regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[^a-zA-Z\\d]).+$",
             message = "La contraseña debe tener al menos una mayúscula, una minúscula, un número y un carácter especial")
    String currentPassword,
    @NotBlank @Size(min = 8, message = "La contraseña debe tener al menos 8 caracteres")
    @Pattern(regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[^a-zA-Z\\d]).+$",
             message = "La contraseña debe tener al menos una mayúscula, una minúscula, un número y un carácter especial")
    String newPassword
) {}
