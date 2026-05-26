package com.petcare.backend.web;

import com.petcare.backend.domain.dto.request.DuenioRequest;
import com.petcare.backend.domain.dto.response.DuenioResponse;
import com.petcare.backend.domain.service.DuenioService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
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
@RequestMapping("/api/duenios")
@RequiredArgsConstructor
public class DuenioController {

	private final DuenioService duenioService;

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	@PreAuthorize("hasAnyRole('ADMIN', 'ASISTENTE')")
	public DuenioResponse create(@Valid @RequestBody DuenioRequest request) {
		return duenioService.create(request);
	}

	@GetMapping
	@PreAuthorize("hasAnyRole('ADMIN', 'ASISTENTE', 'VETERINARIO', 'DUENIO')")
	public List<DuenioResponse> findAll(
			@RequestParam(required = false) String search,
			@RequestParam(required = false) Boolean active,
			Authentication authentication
	) {
		if (isDuenioOnly(authentication)) {
			return List.of(duenioService.findOwn(authentication.getName()));
		}
		return duenioService.findAll(search, active);
	}

	@GetMapping("/me")
	@PreAuthorize("hasRole('DUENIO')")
	public DuenioResponse findMe(Authentication authentication) {
		return duenioService.findOwn(authentication.getName());
	}

	@GetMapping("/{id}")
	@PreAuthorize("hasAnyRole('ADMIN', 'ASISTENTE', 'VETERINARIO', 'DUENIO')")
	public DuenioResponse findById(@PathVariable Long id, Authentication authentication) {
		if (isDuenioOnly(authentication)) {
			return duenioService.findOwnById(id, authentication.getName());
		}
		return duenioService.findById(id);
	}

	@PutMapping("/{id}")
	@PreAuthorize("hasAnyRole('ADMIN', 'ASISTENTE', 'DUENIO')")
	public DuenioResponse update(@PathVariable Long id, @Valid @RequestBody DuenioRequest request, Authentication authentication) {
		if (isDuenioOnly(authentication)) {
			return duenioService.updateOwn(id, request, authentication.getName());
		}
		return duenioService.update(id, request);
	}

	@DeleteMapping("/{id}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	@PreAuthorize("hasAnyRole('ADMIN', 'ASISTENTE')")
	public void deactivate(@PathVariable Long id) {
		duenioService.deactivate(id);
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
