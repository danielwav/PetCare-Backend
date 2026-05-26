package com.petcare.backend.domain.dto.response;

import java.time.LocalDate;

public record AlertaVacunaResponse(
		Long vacunaMascotaId,
		Long mascotaId,
		String mascotaNombre,
		Long vacunaId,
		String vacunaNombre,
		LocalDate fechaProximaDosis,
		String estadoAlerta
) {
}
