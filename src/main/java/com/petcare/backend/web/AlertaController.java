package com.petcare.backend.web;

import com.petcare.backend.domain.dto.response.PanelAlertasDiaResponse;
import com.petcare.backend.domain.service.AlertaService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequiredArgsConstructor
public class AlertaController {

	private final AlertaService alertaService;

	@GetMapping("/api/alertas/dia")
	@PreAuthorize("hasAnyRole('ADMIN', 'ASISTENTE', 'VETERINARIO')")
	public PanelAlertasDiaResponse getDailyPanel() {
		return alertaService.getDailyPanel(null, null);
	}

	@GetMapping("/api/alertas/debug")
	public Map<String, Object> debug(Authentication authentication) {
		if (authentication == null) {
			return Map.of("authenticated", false, "message", "No authentication found");
		}
		return Map.of(
			"authenticated", authentication.isAuthenticated(),
			"name", authentication.getName(),
			"authorities", authentication.getAuthorities().stream()
				.map(GrantedAuthority::getAuthority)
				.toList()
		);
	}
}
