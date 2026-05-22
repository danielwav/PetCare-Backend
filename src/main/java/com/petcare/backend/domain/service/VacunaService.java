package com.petcare.backend.domain.service;

import com.petcare.backend.domain.dto.request.VacunaMascotaRequest;
import com.petcare.backend.domain.dto.request.VacunaRequest;
import com.petcare.backend.domain.dto.response.VacunaMascotaResponse;
import com.petcare.backend.domain.dto.response.VacunaResponse;
import com.petcare.backend.domain.repository.CitaRepository;
import com.petcare.backend.domain.repository.MascotaRepository;
import com.petcare.backend.domain.repository.VacunaMascotaRepository;
import com.petcare.backend.domain.repository.VacunaRepository;
import com.petcare.backend.domain.repository.VeterinarioRepository;
import com.petcare.backend.persistence.entity.Cita;
import com.petcare.backend.persistence.entity.Duenio;
import com.petcare.backend.persistence.entity.Mascota;
import com.petcare.backend.persistence.entity.Vacuna;
import com.petcare.backend.persistence.entity.VacunaMascota;
import com.petcare.backend.persistence.entity.Veterinario;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class VacunaService {

	private static final int DEFAULT_ALERT_DAYS = 30;

	private final VacunaRepository vacunaRepository;
	private final VacunaMascotaRepository vacunaMascotaRepository;
	private final MascotaRepository mascotaRepository;
	private final VeterinarioRepository veterinarioRepository;
	private final CitaRepository citaRepository;
	private final AuthenticatedDuenioService authenticatedDuenioService;

	@Transactional
	public VacunaResponse create(VacunaRequest request) {
		validateUniqueName(request.nombre(), null);

		LocalDateTime now = LocalDateTime.now();
		Vacuna vacuna = Vacuna.builder()
				.nombre(normalizeText(request.nombre()))
				.descripcion(normalizeText(request.descripcion()))
				.intervaloProximaDosisDias(request.intervaloProximaDosisDias())
				.active(true)
				.createdAt(now)
				.updatedAt(now)
				.build();

		return toResponse(vacunaRepository.save(vacuna));
	}

	@Transactional(readOnly = true)
	public List<VacunaResponse> findAll(String search, Boolean active) {
		String normalizedSearch = search == null || search.isBlank() ? null : search.trim();
		Boolean activeFilter = active == null ? true : active;
		return vacunaRepository.search(normalizedSearch, activeFilter).stream()
				.map(this::toResponse)
				.toList();
	}

	@Transactional(readOnly = true)
	public VacunaResponse findById(Long id) {
		return toResponse(findVacuna(id));
	}

	@Transactional
	public VacunaResponse update(Long id, VacunaRequest request) {
		Vacuna vacuna = findVacuna(id);
		validateUniqueName(request.nombre(), id);

		vacuna.setNombre(normalizeText(request.nombre()));
		vacuna.setDescripcion(normalizeText(request.descripcion()));
		vacuna.setIntervaloProximaDosisDias(request.intervaloProximaDosisDias());
		vacuna.setUpdatedAt(LocalDateTime.now());

		return toResponse(vacunaRepository.save(vacuna));
	}

	@Transactional
	public void deactivate(Long id) {
		Vacuna vacuna = findVacuna(id);
		vacuna.setActive(false);
		vacuna.setUpdatedAt(LocalDateTime.now());
		vacunaRepository.save(vacuna);
	}

	@Transactional
	public VacunaResponse activate(Long id) {
		Vacuna vacuna = findVacuna(id);
		vacuna.setActive(true);
		vacuna.setUpdatedAt(LocalDateTime.now());
		return toResponse(vacunaRepository.save(vacuna));
	}

	@Transactional
	public VacunaMascotaResponse registerForMascota(Long mascotaId, VacunaMascotaRequest request) {
		Mascota mascota = findMascota(mascotaId);
		Vacuna vacuna = findVacuna(request.vacunaId());
		Veterinario veterinario = findVeterinario(request.veterinarioId());
		Cita cita = findCitaIfPresent(request.citaId());

		validateApplicationData(mascota, vacuna, veterinario, cita);

		VacunaMascota vacunaMascota = VacunaMascota.builder()
				.mascota(mascota)
				.vacuna(vacuna)
				.veterinario(veterinario)
				.cita(cita)
				.fechaAplicacion(request.fechaAplicacion())
				.lote(normalizeOptionalText(request.lote()))
				.fechaProximaDosis(resolveNextDoseDate(vacuna, request))
				.observaciones(normalizeOptionalText(request.observaciones()))
				.createdAt(LocalDateTime.now())
				.build();

		return toResponse(vacunaMascotaRepository.save(vacunaMascota));
	}

	@Transactional(readOnly = true)
	public List<VacunaMascotaResponse> findByMascota(Long mascotaId) {
		if (!mascotaRepository.existsById(mascotaId)) {
			throw new EntityNotFoundException("Mascota no encontrada.");
		}

		return vacunaMascotaRepository.findByMascotaIdOrderByFechaAplicacionDesc(mascotaId).stream()
				.map(this::toResponse)
				.toList();
	}

	@Transactional(readOnly = true)
	public List<VacunaMascotaResponse> findByMascotaForDuenio(Long mascotaId, String email) {
		Mascota mascota = findMascota(mascotaId);
		validateMascotaBelongsToAuthenticatedDuenio(mascota, email);
		return vacunaMascotaRepository.findByMascotaIdOrderByFechaAplicacionDesc(mascotaId).stream()
				.map(this::toResponse)
				.toList();
	}

	@Transactional(readOnly = true)
	public List<VacunaMascotaResponse> findUpcoming(Integer dias) {
		int days = dias == null ? 60 : dias;
		if (days <= 0) {
			throw new IllegalArgumentException("La cantidad de dias debe ser mayor a cero.");
		}

		LocalDate today = LocalDate.now();
		return vacunaMascotaRepository.findByFechaProximaDosisBetweenOrderByFechaProximaDosisAsc(
						today,
						today.plusDays(days)
				).stream()
				.map(this::toResponse)
				.toList();
	}

	@Transactional(readOnly = true)
	public List<VacunaMascotaResponse> findAlerts(Integer dias) {
		int days = dias == null ? DEFAULT_ALERT_DAYS : dias;
		if (days <= 0) {
			throw new IllegalArgumentException("La ventana de alertas debe ser mayor a cero.");
		}

		return vacunaMascotaRepository.findByFechaProximaDosisLessThanEqualOrderByFechaProximaDosisAsc(
						LocalDate.now().plusDays(days)
				).stream()
				.map(this::toResponse)
				.toList();
	}

	private void validateApplicationData(Mascota mascota, Vacuna vacuna, Veterinario veterinario, Cita cita) {
		if (!mascota.getActive()) {
			throw new IllegalArgumentException("La mascota no esta activa.");
		}
		if (!vacuna.getActive()) {
			throw new IllegalArgumentException("La vacuna no esta activa.");
		}
		if (!veterinario.getActive()) {
			throw new IllegalArgumentException("El veterinario no esta activo.");
		}
		if (cita != null && !cita.getMascota().getId().equals(mascota.getId())) {
			throw new IllegalArgumentException("La cita no pertenece a la mascota indicada.");
		}
		if (cita != null && !cita.getVeterinario().getId().equals(veterinario.getId())) {
			throw new IllegalArgumentException("La cita no corresponde al veterinario indicado.");
		}
	}

	private void validateMascotaBelongsToAuthenticatedDuenio(Mascota mascota, String email) {
		Duenio duenio = authenticatedDuenioService.findByAuthenticatedEmail(email);
		if (!mascota.getDuenio().getId().equals(duenio.getId())) {
			throw new AccessDeniedException("No tienes permiso para consultar vacunas de esta mascota.");
		}
	}

	private LocalDate resolveNextDoseDate(Vacuna vacuna, VacunaMascotaRequest request) {
		if (request.fechaProximaDosis() != null) {
			if (!request.fechaProximaDosis().isAfter(request.fechaAplicacion())) {
				throw new IllegalArgumentException("La proxima dosis debe ser posterior a la fecha de aplicacion.");
			}
			return request.fechaProximaDosis();
		}
		if (vacuna.getIntervaloProximaDosisDias() == null) {
			return null;
		}
		return request.fechaAplicacion().plusDays(vacuna.getIntervaloProximaDosisDias());
	}

	private Cita findCitaIfPresent(Long citaId) {
		if (citaId == null) {
			return null;
		}
		return citaRepository.findById(citaId)
				.orElseThrow(() -> new EntityNotFoundException("Cita no encontrada."));
	}

	private Mascota findMascota(Long id) {
		return mascotaRepository.findById(id)
				.orElseThrow(() -> new EntityNotFoundException("Mascota no encontrada."));
	}

	private Vacuna findVacuna(Long id) {
		return vacunaRepository.findById(id)
				.orElseThrow(() -> new EntityNotFoundException("Vacuna no encontrada."));
	}

	private Veterinario findVeterinario(Long id) {
		return veterinarioRepository.findById(id)
				.orElseThrow(() -> new EntityNotFoundException("Veterinario no encontrado."));
	}

	private void validateUniqueName(String nombre, Long currentId) {
		String normalizedName = normalizeText(nombre);
		vacunaRepository.findByNombreIgnoreCase(normalizedName)
				.filter(vacuna -> currentId == null || !vacuna.getId().equals(currentId))
				.ifPresent(vacuna -> {
					throw new IllegalArgumentException("El nombre de la vacuna ya esta registrado.");
				});
	}

	private VacunaResponse toResponse(Vacuna vacuna) {
		return new VacunaResponse(
				vacuna.getId(),
				vacuna.getNombre(),
				vacuna.getDescripcion(),
				vacuna.getIntervaloProximaDosisDias(),
				vacuna.getActive(),
				vacuna.getCreatedAt(),
				vacuna.getUpdatedAt()
		);
	}

	private VacunaMascotaResponse toResponse(VacunaMascota vacunaMascota) {
		Veterinario veterinario = vacunaMascota.getVeterinario();
		Cita cita = vacunaMascota.getCita();
		return new VacunaMascotaResponse(
				vacunaMascota.getId(),
				vacunaMascota.getMascota().getId(),
				vacunaMascota.getMascota().getNombre(),
				vacunaMascota.getVacuna().getId(),
				vacunaMascota.getVacuna().getNombre(),
				veterinario.getId(),
				fullName(veterinario.getNombres(), veterinario.getApellidos()),
				cita == null ? null : cita.getId(),
				vacunaMascota.getFechaAplicacion(),
				vacunaMascota.getLote(),
				vacunaMascota.getFechaProximaDosis(),
				vacunaMascota.getObservaciones(),
				resolveAlertStatus(vacunaMascota.getFechaProximaDosis()),
				vacunaMascota.getCreatedAt()
		);
	}

	private String resolveAlertStatus(LocalDate nextDoseDate) {
		if (nextDoseDate == null) {
			return "SIN_PROXIMA_DOSIS";
		}
		LocalDate today = LocalDate.now();
		if (nextDoseDate.isBefore(today)) {
			return "VENCIDA";
		}
		if (!nextDoseDate.isAfter(today.plusDays(DEFAULT_ALERT_DAYS))) {
			return "PROXIMA";
		}
		return "PROGRAMADA";
	}

	private String normalizeText(String value) {
		return value.trim();
	}

	private String normalizeOptionalText(String value) {
		return value == null || value.isBlank() ? null : value.trim();
	}

	private String fullName(String nombres, String apellidos) {
		return nombres + " " + apellidos;
	}
}
