package com.petcare.backend.web;

import com.petcare.backend.domain.dto.request.AtencionClinicaRequest;
import com.petcare.backend.domain.dto.response.AtencionClinicaResponse;
import com.petcare.backend.domain.dto.response.HistoriaClinicaResponse;
import com.petcare.backend.domain.service.AtencionClinicaService;
import com.petcare.backend.domain.service.ClinicaService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class AtencionClinicaController {

	private final AtencionClinicaService atencionClinicaService;
	private final ClinicaService clinicaService;

	@PostMapping("/api/citas/{id}/atencion")
	@ResponseStatus(HttpStatus.CREATED)
	public AtencionClinicaResponse register(
			@PathVariable Long id,
			@Valid @RequestBody AtencionClinicaRequest request,
			Authentication authentication
	) {
		return atencionClinicaService.register(id, request, clinicaService.resolveClinicaId(authentication.getName()));
	}

	@GetMapping("/api/mascotas/{id}/historia-clinica")
	public HistoriaClinicaResponse findHistoriaClinicaByMascota(@PathVariable Long id, Authentication authentication) {
		if (isDuenioOnly(authentication)) {
			return atencionClinicaService.findHistoriaClinicaByMascotaForDuenio(id, authentication.getName());
		}
		return atencionClinicaService.findHistoriaClinicaByMascotaScoped(id, clinicaService.resolveClinicaId(authentication.getName()));
	}

	@GetMapping("/api/atenciones/{id}")
	public AtencionClinicaResponse findById(@PathVariable Long id, Authentication authentication) {
		if (isDuenioOnly(authentication)) {
			return atencionClinicaService.findByIdForDuenio(id, authentication.getName());
		}
		return atencionClinicaService.findByIdScoped(id, clinicaService.resolveClinicaId(authentication.getName()));
	}

	private boolean isDuenioOnly(Authentication authentication) {
		return hasRole(authentication, "ROLE_DUENIO")
				&& !hasRole(authentication, "ROLE_ADMIN")
				&& !hasRole(authentication, "ROLE_ASISTENTE")
				&& !hasRole(authentication, "ROLE_VETERINARIO");
	}

	private boolean hasRole(Authentication authentication, String role) {
		return authentication != null && authentication.getAuthorities().stream()
				.anyMatch(authority -> authority.getAuthority().equals(role));
	}
}
