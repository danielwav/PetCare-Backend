package com.petcare.backend.web;

import com.petcare.backend.domain.dto.request.DuenioRequest;
import com.petcare.backend.domain.dto.response.DuenioResponse;
import com.petcare.backend.domain.service.DuenioService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
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
	@PreAuthorize("hasAnyRole('ADMIN', 'ASISTENTE', 'VETERINARIO')")
	public List<DuenioResponse> findAll(
			@RequestParam(required = false) String search,
			@RequestParam(required = false) Boolean active
	) {
		return duenioService.findAll(search, active);
	}

	@GetMapping("/{id}")
	@PreAuthorize("hasAnyRole('ADMIN', 'ASISTENTE', 'VETERINARIO')")
	public DuenioResponse findById(@PathVariable Long id) {
		return duenioService.findById(id);
	}

	@PutMapping("/{id}")
	@PreAuthorize("hasAnyRole('ADMIN', 'ASISTENTE')")
	public DuenioResponse update(@PathVariable Long id, @Valid @RequestBody DuenioRequest request) {
		return duenioService.update(id, request);
	}

	@DeleteMapping("/{id}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	@PreAuthorize("hasAnyRole('ADMIN', 'ASISTENTE')")
	public void deactivate(@PathVariable Long id) {
		duenioService.deactivate(id);
	}
}
