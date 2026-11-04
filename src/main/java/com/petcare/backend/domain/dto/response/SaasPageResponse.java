package com.petcare.backend.domain.dto.response;

import org.springframework.data.domain.Page;

import java.util.List;

public record SaasPageResponse<T>(List<T> items, int page, int totalPages, long totalElements) {
	public static <T> SaasPageResponse<T> from(Page<T> page) {
		return new SaasPageResponse<>(page.getContent(), page.getNumber(), page.getTotalPages(), page.getTotalElements());
	}
}
