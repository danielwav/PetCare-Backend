package com.petcare.backend.web;

import com.petcare.backend.domain.dto.request.VeterinarioRequest;
import com.petcare.backend.domain.dto.response.DisponibilidadVeterinarioResponse;
import com.petcare.backend.domain.dto.response.VeterinarioResponse;
import com.petcare.backend.domain.service.ClinicaService;
import com.petcare.backend.domain.service.VeterinarioService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequiredArgsConstructor
public class VeterinarioController {

	private final VeterinarioService veterinarioService;
	private final ClinicaService clinicaService;

	@PostMapping("/api/veterinarios")
	@ResponseStatus(HttpStatus.CREATED)
	public VeterinarioResponse create(@Valid @RequestBody VeterinarioRequest request, Authentication authentication) {
		return veterinarioService.create(request, clinicaService.resolveClinicaId(authentication.getName()));
	}

	@GetMapping("/api/veterinarios")
	public List<VeterinarioResponse> findAll(
			@RequestParam(required = false) String search,
			@RequestParam(required = false) Boolean active,
			Authentication authentication
	) {
		return veterinarioService.findAll(search, active, clinicaService.resolveClinicaId(authentication.getName()));
	}

	@GetMapping("/api/veterinarios/{id}")
	public VeterinarioResponse findById(@PathVariable Long id, Authentication authentication) {
		return veterinarioService.findById(id, clinicaService.resolveClinicaId(authentication.getName()));
	}

	@GetMapping("/api/veterinarios/{id}/disponibilidad")
	public DisponibilidadVeterinarioResponse findDisponibilidad(
			@PathVariable Long id,
			@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha,
			@RequestParam(required = false) Integer duracionMinutos,
			Authentication authentication
	) {
		return veterinarioService.findDisponibilidad(id, fecha, duracionMinutos, clinicaService.resolveClinicaId(authentication.getName()));
	}

	@PutMapping("/api/veterinarios/{id}")
	public VeterinarioResponse update(@PathVariable Long id, @Valid @RequestBody VeterinarioRequest request, Authentication authentication) {
		return veterinarioService.update(id, request, clinicaService.resolveClinicaId(authentication.getName()));
	}

	@DeleteMapping("/api/veterinarios/{id}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void deactivate(@PathVariable Long id, Authentication authentication) {
		veterinarioService.deactivate(id, clinicaService.resolveClinicaId(authentication.getName()));
	}
}
