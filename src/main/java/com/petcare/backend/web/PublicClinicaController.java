package com.petcare.backend.web;

import com.petcare.backend.domain.dto.response.ClinicaPublicResponse;
import com.petcare.backend.domain.dto.response.ServicioPublicResponse;
import com.petcare.backend.domain.repository.ServicioRepository;
import com.petcare.backend.domain.service.ClinicaService;
import com.petcare.backend.persistence.entity.Clinica;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/public/clinicas")
@RequiredArgsConstructor
public class PublicClinicaController {

	private final ClinicaService clinicaService;
	private final ServicioRepository servicioRepository;

	@GetMapping("/{slug}")
	public ClinicaPublicResponse bySlug(@PathVariable String slug) {
		Clinica clinica = clinicaService.findPublicBySlug(slug);
		return new ClinicaPublicResponse(
				clinica.getId(),
				clinica.getNombre(),
				clinica.getSlug(),
				clinica.getDireccion(),
				clinica.getTelefono(),
				clinica.getHorarioAtencion(),
				clinica.getDescripcion(),
				clinica.getLogoUrl()
		);
	}

	@GetMapping("/{slug}/servicios")
	public List<ServicioPublicResponse> servicios(@PathVariable String slug) {
		Clinica clinica = clinicaService.findPublicBySlug(slug);
		return servicioRepository.findAllByClinicaIdOrderByNombreAsc(clinica.getId()).stream()
				.filter(s -> Boolean.TRUE.equals(s.getActive()))
				.map(s -> new ServicioPublicResponse(
						s.getId(),
						s.getNombre(),
						s.getDescripcion(),
						s.getCostoBase()
				))
				.toList();
	}
}