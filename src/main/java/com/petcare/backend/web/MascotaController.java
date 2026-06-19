package com.petcare.backend.web;

import com.petcare.backend.domain.dto.request.MascotaRequest;
import com.petcare.backend.domain.dto.response.MascotaResponse;
import com.petcare.backend.domain.service.MascotaService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class MascotaController {

	private final MascotaService mascotaService;

	@PostMapping("/api/mascotas")
	@ResponseStatus(HttpStatus.CREATED)
	public MascotaResponse create(@Valid @RequestBody MascotaRequest request, Authentication authentication) {
		if (isDuenioOnly(authentication)) {
			return mascotaService.createForDuenio(authentication.getName(), request);
		}
		return mascotaService.create(request);
	}

	@GetMapping("/api/mascotas")
	public List<MascotaResponse> findAll(
			@RequestParam(required = false) String search,
			@RequestParam(required = false) Long duenioId,
			@RequestParam(required = false) Boolean active,
			Authentication authentication
	) {
		if (isDuenioOnly(authentication)) {
			return mascotaService.findAllForDuenio(authentication.getName(), search, active);
		}
		return mascotaService.findAll(search, duenioId, active);
	}

	@GetMapping("/api/duenios/{duenioId}/mascotas")
	public List<MascotaResponse> findByDuenio(@PathVariable Long duenioId, Authentication authentication) {
		if (isDuenioOnly(authentication)) {
			return mascotaService.findByDuenioForDuenio(authentication.getName(), duenioId);
		}
		return mascotaService.findByDuenio(duenioId);
	}

	@GetMapping("/api/mascotas/{id}")
	public MascotaResponse findById(@PathVariable Long id, Authentication authentication) {
		if (isDuenioOnly(authentication)) {
			return mascotaService.findByIdForDuenio(id, authentication.getName());
		}
		return mascotaService.findById(id);
	}

	@PutMapping("/api/mascotas/{id}")
	public MascotaResponse update(@PathVariable Long id, @Valid @RequestBody MascotaRequest request, Authentication authentication) {
		if (isDuenioOnly(authentication)) {
			return mascotaService.updateForDuenio(id, request, authentication.getName());
		}
		return mascotaService.update(id, request);
	}

	@DeleteMapping("/api/mascotas/{id}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void deactivate(@PathVariable Long id, Authentication authentication) {
		if (isDuenioOnly(authentication)) {
			mascotaService.deactivateForDuenio(id, authentication.getName());
		} else {
			mascotaService.deactivate(id);
		}
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
