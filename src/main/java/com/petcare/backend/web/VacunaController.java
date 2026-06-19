package com.petcare.backend.web;

import com.petcare.backend.domain.dto.request.VacunaMascotaRequest;
import com.petcare.backend.domain.dto.request.VacunaRequest;
import com.petcare.backend.domain.dto.response.VacunaMascotaResponse;
import com.petcare.backend.domain.dto.response.VacunaResponse;
import com.petcare.backend.domain.service.VacunaService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class VacunaController {

	private final VacunaService vacunaService;

	@PostMapping("/api/vacunas")
	@ResponseStatus(HttpStatus.CREATED)
	public VacunaResponse create(@Valid @RequestBody VacunaRequest request) {
		return vacunaService.create(request);
	}

	@GetMapping("/api/vacunas")
	public List<VacunaResponse> findAll(
			@RequestParam(required = false) String search,
			@RequestParam(required = false) Boolean active
	) {
		return vacunaService.findAll(search, active);
	}

	@GetMapping("/api/vacunas/{id}")
	public VacunaResponse findById(@PathVariable Long id) {
		return vacunaService.findById(id);
	}

	@PutMapping("/api/vacunas/{id}")
	public VacunaResponse update(@PathVariable Long id, @Valid @RequestBody VacunaRequest request) {
		return vacunaService.update(id, request);
	}

	@PatchMapping("/api/vacunas/{id}/activar")
	public VacunaResponse activate(@PathVariable Long id) {
		return vacunaService.activate(id);
	}

	@DeleteMapping("/api/vacunas/{id}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void deactivate(@PathVariable Long id) {
		vacunaService.deactivate(id);
	}

	@PostMapping("/api/mascotas/{id}/vacunas")
	@ResponseStatus(HttpStatus.CREATED)
	public VacunaMascotaResponse registerForMascota(
			@PathVariable Long id,
			@Valid @RequestBody VacunaMascotaRequest request,
			Authentication authentication
	) {
		return vacunaService.registerForMascota(id, request, authentication.getName());
	}

	@GetMapping("/api/mascotas/{id}/vacunas")
	public List<VacunaMascotaResponse> findByMascota(@PathVariable Long id, Authentication authentication) {
		if (isDuenioOnly(authentication)) {
			return vacunaService.findByMascotaForDuenio(id, authentication.getName());
		}
		return vacunaService.findByMascota(id);
	}

	@GetMapping("/api/vacunas/proximas")
	public List<VacunaMascotaResponse> findUpcoming(@RequestParam(required = false) Integer dias) {
		return vacunaService.findUpcoming(dias);
	}

	@GetMapping("/api/alertas/vacunas")
	public List<VacunaMascotaResponse> findAlerts(@RequestParam(required = false) Integer dias, Authentication authentication) {
		if (isDuenioOnly(authentication)) {
			return vacunaService.findAlertsForDuenio(dias, authentication.getName());
		}
		return vacunaService.findAlerts(dias);
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
