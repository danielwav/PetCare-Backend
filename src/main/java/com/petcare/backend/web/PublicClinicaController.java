package com.petcare.backend.web;

import com.petcare.backend.domain.dto.response.ClinicaPublicResponse;
import com.petcare.backend.domain.dto.response.ClinicaDirectoryResponse;
import com.petcare.backend.domain.dto.response.ClinicDirectoryPageResponse;
import com.petcare.backend.domain.dto.response.ServicioPublicResponse;
import com.petcare.backend.domain.repository.ClinicaRepository;
import com.petcare.backend.domain.repository.ServicioRepository;
import com.petcare.backend.domain.service.ClinicaService;
import com.petcare.backend.persistence.entity.Clinica;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/public/clinicas")
@RequiredArgsConstructor
public class PublicClinicaController {

	private final ClinicaService clinicaService;
	private final ServicioRepository servicioRepository;
	private final ClinicaRepository clinicaRepository;

	@GetMapping
	public ClinicDirectoryPageResponse directory(
			@RequestParam(defaultValue = "") String q,
			@RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "12") int size) {
		if (page < 0 || size < 1 || size > 24 || q.length() > 100) {
			throw new IllegalArgumentException("page must be >= 0, size must be 1-24, and q must be at most 100 characters");
		}
		String search = q.trim().toLowerCase(Locale.ROOT)
				.replace('\u00e1', 'a').replace('\u00e9', 'e').replace('\u00ed', 'i')
				.replace('\u00f3', 'o').replace('\u00fa', 'u').replace('\u00fc', 'u');
		String pattern = search.isEmpty() ? null : "%" + search
				.replace("!", "!!").replace("%", "!%").replace("_", "!_") + "%";
		var clinics = clinicaRepository.findDirectory(pattern,
				PageRequest.of(page, size, Sort.by("nombre", "id")));
		List<Long> ids = clinics.getContent().stream().map(Clinica::getId).toList();
		Map<Long, List<String>> services = ids.isEmpty() ? Map.of()
				: servicioRepository.findActiveDirectoryNames(ids).stream().collect(Collectors.groupingBy(
						ServicioRepository.DirectoryServiceName::getClinicaId,
						Collectors.mapping(ServicioRepository.DirectoryServiceName::getNombre, Collectors.toList())));
		List<ClinicaDirectoryResponse> items = clinics.getContent().stream()
				.map(c -> new ClinicaDirectoryResponse(c.getId(), c.getNombre(), c.getSlug(),
						c.getDireccion(), c.getTelefono(), c.getHorarioAtencion(), c.getDescripcion(),
						c.getLogoUrl(), services.getOrDefault(c.getId(), List.of())))
				.toList();
		return new ClinicDirectoryPageResponse(items, clinics.getNumber(), clinics.getTotalPages(), clinics.getTotalElements());
	}

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
