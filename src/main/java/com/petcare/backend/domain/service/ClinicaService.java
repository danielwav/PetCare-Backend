package com.petcare.backend.domain.service;

import com.petcare.backend.domain.dto.request.UpdateClinicaRequest;
import com.petcare.backend.domain.dto.response.ClinicaResponse;
import com.petcare.backend.domain.repository.ClinicaRepository;
import com.petcare.backend.domain.repository.UsuarioRepository;
import com.petcare.backend.persistence.entity.Clinica;
import com.petcare.backend.persistence.entity.Usuario;
import com.petcare.backend.persistence.enums.EstadoClinica;
import com.petcare.backend.persistence.enums.PlanClinica;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.Normalizer;
import java.time.LocalDateTime;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class ClinicaService {

	private static final String DEFAULT_CLINICA_SLUG = "demo";

	private final ClinicaRepository clinicaRepository;
	private final UsuarioRepository usuarioRepository;

	@Transactional
	public Clinica createClinic(String nombre, String slug) {
		String normalizedSlug = slug != null && !slug.isBlank() ? slug : slugify(nombre);
		if (clinicaRepository.existsBySlug(normalizedSlug)) {
			throw new IllegalArgumentException("Ya existe una clinica con ese slug.");
		}
		LocalDateTime now = LocalDateTime.now();
		return clinicaRepository.save(Clinica.builder()
				.nombre(nombre)
				.slug(normalizedSlug)
				.plan(PlanClinica.TRIAL)
				.estado(EstadoClinica.ACTIVA)
				.createdAt(now)
				.updatedAt(now)
				.build());
	}

	@Transactional
	public Clinica getOrCreateDefaultClinic() {
		return clinicaRepository.findBySlug(DEFAULT_CLINICA_SLUG)
				.orElseGet(() -> createClinic("Clínica Demo", DEFAULT_CLINICA_SLUG));
	}

	@Transactional(readOnly = true)
	public ClinicaResponse getMyClinic(String email) {
		return toResponse(findMyClinic(email));
	}

	@Transactional(readOnly = true)
	public Long resolveClinicaId(String email) {
		Clinica clinica = findMyClinic(email.toLowerCase(Locale.ROOT));
		if (clinica.getEstado() != EstadoClinica.ACTIVA) {
			throw new AccessDeniedException("La clinica no esta activa.");
		}
		return clinica.getId();
	}

	@Transactional
	public ClinicaResponse updateMyClinic(String email, UpdateClinicaRequest request) {
		Clinica clinica = findMyClinic(email);
		String slug = request.slug().toLowerCase(Locale.ROOT);
		if (!slug.equals(clinica.getSlug()) && clinicaRepository.existsBySlug(slug)) {
			throw new IllegalArgumentException("Ya existe una clinica con ese slug.");
		}
		clinica.setNombre(request.nombre());
		clinica.setSlug(slug);
		clinica.setUpdatedAt(LocalDateTime.now());
		return toResponse(clinicaRepository.save(clinica));
	}

	private Clinica findMyClinic(String email) {
		Usuario usuario = usuarioRepository.findByEmail(email.toLowerCase(Locale.ROOT))
				.orElseThrow(() -> new EntityNotFoundException("Usuario no encontrado."));
		if (usuario.getClinica() == null) {
			throw new EntityNotFoundException("El usuario no tiene una clinica asignada.");
		}
		return usuario.getClinica();
	}

	public static ClinicaResponse toResponse(Clinica clinica) {
		return new ClinicaResponse(
				clinica.getId(),
				clinica.getNombre(),
				clinica.getSlug(),
				clinica.getPlan().name(),
				clinica.getEstado().name(),
				clinica.getCreatedAt()
		);
	}

	public static String slugify(String value) {
		if (value == null) {
			return "";
		}
		String normalized = Normalizer.normalize(value, Normalizer.Form.NFD)
				.replaceAll("\\p{M}", "");
		return normalized.toLowerCase(Locale.ROOT)
				.replaceAll("[^a-z0-9]+", "-")
				.replaceAll("(^-|-$)", "");
	}
}