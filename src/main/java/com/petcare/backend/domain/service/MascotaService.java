package com.petcare.backend.domain.service;

import com.petcare.backend.domain.dto.request.MascotaRequest;
import com.petcare.backend.domain.dto.response.MascotaResponse;
import com.petcare.backend.domain.repository.CitaRepository;
import com.petcare.backend.domain.repository.DuenioRepository;
import com.petcare.backend.domain.repository.MascotaRepository;
import com.petcare.backend.persistence.entity.Cita;
import com.petcare.backend.persistence.entity.Duenio;
import com.petcare.backend.persistence.entity.Mascota;
import com.petcare.backend.persistence.enums.EstadoCita;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Period;
import java.util.List;

@Service
@RequiredArgsConstructor
public class MascotaService {

	private final MascotaRepository mascotaRepository;
	private final DuenioRepository duenioRepository;
	private final CitaRepository citaRepository;
	private final AuthenticatedDuenioService authenticatedDuenioService;

	@Transactional
	public MascotaResponse create(MascotaRequest request) {
		Duenio duenio = findActiveDuenioById(request.duenioId());
		LocalDateTime now = LocalDateTime.now();

		Mascota mascota = Mascota.builder()
				.duenio(duenio)
				.nombre(normalizeText(request.nombre()))
				.especie(normalizeText(request.especie()))
				.raza(normalizeText(request.raza()))
				.sexo(request.sexo())
				.fechaNacimiento(request.fechaNacimiento())
				.color(normalizeNullableText(request.color()))
				.pesoKg(request.pesoKg())
				.observaciones(normalizeNullableText(request.observaciones()))
				.fotoUrl(request.fotoUrl())
				.active(true)
				.createdAt(now)
				.updatedAt(now)
				.build();

		return toResponse(mascotaRepository.save(mascota));
	}

	@Transactional
	public MascotaResponse createForDuenio(String email, MascotaRequest request) {
		Duenio duenio = authenticatedDuenioService.findByAuthenticatedEmail(email);
		return create(new MascotaRequest(
				duenio.getId(),
				request.nombre(),
				request.especie(),
				request.raza(),
				request.sexo(),
				request.fechaNacimiento(),
				request.color(),
				request.pesoKg(),
				request.observaciones(),
				request.fotoUrl()
		));
	}

	@Transactional(readOnly = true)
	public List<MascotaResponse> findAll(String search, Long duenioId, Boolean active) {
		String normalizedSearch = search == null || search.isBlank() ? null : search.trim();
		return mascotaRepository.search(normalizedSearch, duenioId, active).stream()
				.map(this::toResponse)
				.toList();
	}

	@Transactional(readOnly = true)
	public List<MascotaResponse> findByDuenio(Long duenioId) {
		if (!duenioRepository.existsById(duenioId)) {
			throw new EntityNotFoundException("Duenio no encontrado.");
		}

		return mascotaRepository.findByDuenioIdOrderByNombreAsc(duenioId).stream()
				.map(this::toResponse)
				.toList();
	}

	@Transactional(readOnly = true)
	public List<MascotaResponse> findAllForDuenio(String email, String search, Boolean active) {
		Duenio duenio = authenticatedDuenioService.findByAuthenticatedEmail(email);
		return findAll(search, duenio.getId(), active);
	}

	@Transactional(readOnly = true)
	public List<MascotaResponse> findByDuenioForDuenio(String email, Long duenioId) {
		authenticatedDuenioService.validateOwnDuenio(email, duenioId);
		return findByDuenio(duenioId);
	}

	@Transactional(readOnly = true)
	public MascotaResponse findById(Long id) {
		return toResponse(findEntityById(id));
	}

	@Transactional(readOnly = true)
	public MascotaResponse findByIdForDuenio(Long id, String email) {
		Mascota mascota = findEntityById(id);
		Duenio duenio = authenticatedDuenioService.findByAuthenticatedEmail(email);
		validateOwnedMascota(mascota, duenio);
		return toResponse(mascota);
	}

	@Transactional
	public MascotaResponse update(Long id, MascotaRequest request) {
		Mascota mascota = findEntityById(id);
		Duenio duenio = findActiveDuenioById(request.duenioId());

		mascota.setDuenio(duenio);
		mascota.setNombre(normalizeText(request.nombre()));
		mascota.setEspecie(normalizeText(request.especie()));
		mascota.setRaza(normalizeText(request.raza()));
		mascota.setSexo(request.sexo());
		mascota.setFechaNacimiento(request.fechaNacimiento());
		mascota.setColor(normalizeNullableText(request.color()));
		mascota.setPesoKg(request.pesoKg());
		mascota.setObservaciones(normalizeNullableText(request.observaciones()));
		mascota.setFotoUrl(request.fotoUrl());
		mascota.setUpdatedAt(LocalDateTime.now());

		return toResponse(mascotaRepository.save(mascota));
	}

	@Transactional
	public MascotaResponse updateForDuenio(Long id, MascotaRequest request, String email) {
		Mascota mascota = findEntityById(id);
		Duenio duenio = authenticatedDuenioService.findByAuthenticatedEmail(email);
		validateOwnedMascota(mascota, duenio);
		mascota.setNombre(normalizeText(request.nombre()));
		mascota.setEspecie(normalizeText(request.especie()));
		mascota.setRaza(normalizeText(request.raza()));
		mascota.setSexo(request.sexo());
		mascota.setFechaNacimiento(request.fechaNacimiento());
		mascota.setColor(normalizeNullableText(request.color()));
		mascota.setPesoKg(request.pesoKg());
		mascota.setObservaciones(normalizeNullableText(request.observaciones()));
		mascota.setFotoUrl(request.fotoUrl());
		mascota.setUpdatedAt(LocalDateTime.now());
		return toResponse(mascotaRepository.save(mascota));
	}

	@Transactional
	public void deactivateForDuenio(Long id, String email) {
		Mascota mascota = findEntityById(id);
		Duenio duenio = authenticatedDuenioService.findByAuthenticatedEmail(email);
		validateOwnedMascota(mascota, duenio);
		cancelFutureCitas(id);
		mascota.setActive(false);
		mascota.setUpdatedAt(LocalDateTime.now());
		mascotaRepository.save(mascota);
	}

	@Transactional
	public void deactivate(Long id) {
		Mascota mascota = findEntityById(id);
		cancelFutureCitas(id);
		mascota.setActive(false);
		mascota.setUpdatedAt(LocalDateTime.now());
		mascotaRepository.save(mascota);
	}

	private void cancelFutureCitas(Long mascotaId) {
		List<Cita> futureCitas = citaRepository.findByMascotaIdAndFechaGreaterThanEqualAndEstadoIn(
				mascotaId, LocalDate.now(), List.of(EstadoCita.PROGRAMADA, EstadoCita.CONFIRMADA));
		for (Cita cita : futureCitas) {
			cita.setEstado(EstadoCita.CANCELADA);
		}
		citaRepository.saveAll(futureCitas);
	}

	private Mascota findEntityById(Long id) {
		return mascotaRepository.findById(id)
				.orElseThrow(() -> new EntityNotFoundException("Mascota no encontrada."));
	}

	private Duenio findActiveDuenioById(Long duenioId) {
		Duenio duenio = duenioRepository.findById(duenioId)
				.orElseThrow(() -> new EntityNotFoundException("Duenio no encontrado."));

		if (!duenio.getActive()) {
			throw new IllegalArgumentException("No se puede registrar una mascota para un duenio inactivo.");
		}

		return duenio;
	}

	private void validateOwnedMascota(Mascota mascota, Duenio duenio) {
		if (!mascota.getDuenio().getId().equals(duenio.getId())) {
			throw new AccessDeniedException("No tienes permiso para consultar esta mascota.");
		}
	}

	private MascotaResponse toResponse(Mascota mascota) {
		Duenio duenio = mascota.getDuenio();
		String duenioNombreCompleto = duenio.getNombres() + " " + duenio.getApellidos();

		return new MascotaResponse(
				mascota.getId(),
				duenio.getId(),
				duenioNombreCompleto,
				mascota.getNombre(),
				mascota.getEspecie(),
				mascota.getRaza(),
				mascota.getSexo(),
				mascota.getFechaNacimiento(),
				calculateAgeYears(mascota.getFechaNacimiento()),
				mascota.getColor(),
				mascota.getPesoKg(),
				mascota.getObservaciones(),
				mascota.getFotoUrl(),
				mascota.getActive(),
				mascota.getCreatedAt(),
				mascota.getUpdatedAt()
		);
	}

	private Integer calculateAgeYears(LocalDate birthDate) {
		return Period.between(birthDate, LocalDate.now()).getYears();
	}

	private String normalizeText(String value) {
		return value.trim();
	}

	private String normalizeNullableText(String value) {
		return value == null || value.isBlank() ? null : value.trim();
	}
}
