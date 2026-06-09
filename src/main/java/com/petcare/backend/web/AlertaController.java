package com.petcare.backend.web;

import com.petcare.backend.domain.dto.response.PanelAlertasDiaResponse;
import com.petcare.backend.domain.service.AlertaService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class AlertaController {

	private final AlertaService alertaService;

	@GetMapping("/api/alertas/dia")
	@PreAuthorize("hasAnyRole('ADMIN', 'ASISTENTE', 'VETERINARIO')")
	public PanelAlertasDiaResponse getDailyPanel() {
		return alertaService.getDailyPanel(null, null);
	}
}
