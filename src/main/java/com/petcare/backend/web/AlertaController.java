package com.petcare.backend.web;

import com.petcare.backend.domain.dto.response.PanelAlertasDiaResponse;
import com.petcare.backend.domain.service.AlertaService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@RestController
@RequiredArgsConstructor
public class AlertaController {

	private final AlertaService alertaService;

	@GetMapping("/api/alertas/dia")
	@PreAuthorize("hasAnyRole('ADMIN', 'ASISTENTE', 'VETERINARIO')")
	public PanelAlertasDiaResponse getDailyPanel(
			@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha,
			@RequestParam(required = false) Integer diasVacunas,
			Authentication authentication
	) {
		return alertaService.getDailyPanel(fecha, diasVacunas);
	}
}
