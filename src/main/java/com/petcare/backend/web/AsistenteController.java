package com.petcare.backend.web;

import com.petcare.backend.domain.dto.request.AsistenteRequest;
import com.petcare.backend.domain.dto.response.AsistenteResponse;
import com.petcare.backend.domain.service.AsistenteService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
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
public class AsistenteController {

	private final AsistenteService asistenteService;

	@PostMapping("/api/asistentes")
	@ResponseStatus(HttpStatus.CREATED)
	public AsistenteResponse create(@Valid @RequestBody AsistenteRequest request) {
		return asistenteService.create(request);
	}

	@GetMapping("/api/asistentes")
	public List<AsistenteResponse> findAll(
			@RequestParam(required = false) String search,
			@RequestParam(required = false) Boolean active
	) {
		return asistenteService.findAll(search, active);
	}

	@GetMapping("/api/asistentes/{id}")
	public AsistenteResponse findById(@PathVariable Long id) {
		return asistenteService.findById(id);
	}

	@PutMapping("/api/asistentes/{id}")
	public AsistenteResponse update(@PathVariable Long id, @Valid @RequestBody AsistenteRequest request) {
		return asistenteService.update(id, request);
	}

	@PatchMapping("/api/asistentes/{id}/activar")
	public AsistenteResponse activate(@PathVariable Long id) {
		return asistenteService.activate(id);
	}

	@DeleteMapping("/api/asistentes/{id}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void deactivate(@PathVariable Long id) {
		asistenteService.deactivate(id);
	}
}
