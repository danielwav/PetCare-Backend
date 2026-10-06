package com.petcare.backend.domain.dto.response;

import java.util.List;

public record ClinicDirectoryPageResponse(
		List<ClinicaDirectoryResponse> items,
		int page,
		int totalPages,
		long totalElements
) {
}
