package com.petcare.backend.domain.dto.request;

import jakarta.validation.constraints.NotEmpty;
import java.util.Set;

public record UpdateUserRolesRequest(
        @NotEmpty Set<String> roles
) {
}
