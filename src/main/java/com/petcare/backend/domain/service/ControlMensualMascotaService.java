package com.petcare.backend.domain.service;

import com.petcare.backend.domain.dto.request.ControlMensualMascotaRequest;
import com.petcare.backend.domain.dto.response.ControlMensualMascotaResponse;
import com.petcare.backend.domain.repository.ControlMensualMascotaRepository;
import com.petcare.backend.domain.repository.MascotaRepository;
import com.petcare.backend.domain.repository.VeterinarioRepository;
import com.petcare.backend.persistence.entity.ControlMensualMascota;
import com.petcare.backend.persistence.entity.Mascota;
import com.petcare.backend.persistence.entity.Veterinario;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ControlMensualMascotaService {

	private final ControlMensualMascotaRepository controlRepository;
	private final MascotaRepository mascotaRepository;
	private final VeterinarioRepository veterinarioRepository;

	@Transactional
	public ControlMensualMascotaResponse create(Long mascotaId, ControlMensualMascotaRequest request, Long clinicaId) {
		Mascota mascota = findMascota(mascotaId);
		validateMascotaBelongsToClinic(mascota, clinicaId);
		Veterinario veterinario = findVeterinario(request.veterinarioId());
		validateVeterinarioBelongsToClinic(veterinario, clinicaId);
		validateBaseData(mascota, veterinario);
		validateUniqueMonth(mascotaId, request.fechaControl().getYear(), request.fechaControl().getMonthValue(), null);

		LocalDateTime now = LocalDateTime.now();
		ControlMensualMascota control = ControlMensualMascota.builder()
				.mascota(mascota)
				.veterinario(veterinario)
				.fechaControl(request.fechaControl())
				.anio(request.fechaControl().getYear())
				.mes(request.fechaControl().getMonthValue())
				.pesoKg(request.pesoKg())
				.alimentacion(normalizeOptionalText(request.alimentacion()))
				.observaciones(normalizeOptionalText(request.observaciones()))
				.recomendaciones(normalizeOptionalText(request.recomendaciones()))
				.createdAt(now)
				.updatedAt(now)
				.build();

		return toResponse(controlRepository.save(control));
	}

	@Transactional(readOnly = true)
	public List<ControlMensualMascotaResponse> findByMascota(Long mascotaId, Long clinicaId) {
		Mascota mascota = findMascota(mascotaId);
		validateMascotaBelongsToClinic(mascota, clinicaId);

		return controlRepository.findByMascotaIdOrderByFechaControlDesc(mascotaId).stream()
				.map(this::toResponse)
				.toList();
	}

	@Transactional(readOnly = true)
	public ControlMensualMascotaResponse findById(Long id, Long clinicaId) {
		ControlMensualMascota control = findEntityById(id);
		validateMascotaBelongsToClinic(control.getMascota(), clinicaId);
		return toResponse(control);
	}

	@Transactional
	public ControlMensualMascotaResponse update(Long id, ControlMensualMascotaRequest request, Long clinicaId) {
		ControlMensualMascota control = findEntityById(id);
		validateMascotaBelongsToClinic(control.getMascota(), clinicaId);
		Veterinario veterinario = findVeterinario(request.veterinarioId());
		validateVeterinarioBelongsToClinic(veterinario, clinicaId);
		validateBaseData(control.getMascota(), veterinario);
		validateUniqueMonth(
				control.getMascota().getId(),
				request.fechaControl().getYear(),
				request.fechaControl().getMonthValue(),
				id
		);

		control.setVeterinario(veterinario);
		control.setFechaControl(request.fechaControl());
		control.setAnio(request.fechaControl().getYear());
		control.setMes(request.fechaControl().getMonthValue());
		control.setPesoKg(request.pesoKg());
		control.setAlimentacion(normalizeOptionalText(request.alimentacion()));
		control.setObservaciones(normalizeOptionalText(request.observaciones()));
		control.setRecomendaciones(normalizeOptionalText(request.recomendaciones()));
		control.setUpdatedAt(LocalDateTime.now());

		return toResponse(controlRepository.save(control));
	}

	private void validateUniqueMonth(Long mascotaId, Integer anio, Integer mes, Long currentId) {
		controlRepository.findByMascotaIdAndAnioAndMes(mascotaId, anio, mes)
				.filter(existing -> currentId == null || !existing.getId().equals(currentId))
				.ifPresent(existing -> {
					throw new IllegalArgumentException("Ya existe un control mensual para esta mascota en el mes indicado.");
				});
	}

	private void validateBaseData(Mascota mascota, Veterinario veterinario) {
		if (!mascota.getActive()) {
			throw new IllegalArgumentException("La mascota no esta activa.");
		}
		if (!veterinario.getActive()) {
			throw new IllegalArgumentException("El veterinario no esta activo.");
		}
	}

	private void validateMascotaBelongsToClinic(Mascota mascota, Long clinicaId) {
		if (mascota.getClinica() == null || !mascota.getClinica().getId().equals(clinicaId)) {
			throw new AccessDeniedException("La mascota indicada no pertenece a tu clinica.");
		}
	}

	private void validateVeterinarioBelongsToClinic(Veterinario veterinario, Long clinicaId) {
		if (veterinario.getClinica() == null || !veterinario.getClinica().getId().equals(clinicaId)) {
			throw new AccessDeniedException("El veterinario indicado no pertenece a tu clinica.");
		}
	}

	private Mascota findMascota(Long id) {
		return mascotaRepository.findById(id)
				.orElseThrow(() -> new EntityNotFoundException("Mascota no encontrada."));
	}

	private Veterinario findVeterinario(Long id) {
		return veterinarioRepository.findById(id)
				.orElseThrow(() -> new EntityNotFoundException("Veterinario no encontrado."));
	}

	private ControlMensualMascota findEntityById(Long id) {
		return controlRepository.findById(id)
				.orElseThrow(() -> new EntityNotFoundException("Control mensual no encontrado."));
	}

	private ControlMensualMascotaResponse toResponse(ControlMensualMascota control) {
		Veterinario veterinario = control.getVeterinario();
		return new ControlMensualMascotaResponse(
				control.getId(),
				control.getMascota().getId(),
				control.getMascota().getNombre(),
				veterinario.getId(),
				fullName(veterinario.getNombres(), veterinario.getApellidos()),
				control.getFechaControl(),
				control.getAnio(),
				control.getMes(),
				control.getPesoKg(),
				control.getAlimentacion(),
				control.getObservaciones(),
				control.getRecomendaciones(),
				control.getCreatedAt(),
				control.getUpdatedAt()
		);
	}

	private String normalizeOptionalText(String value) {
		return value == null || value.isBlank() ? null : value.trim();
	}

	private String fullName(String nombres, String apellidos) {
		return nombres + " " + apellidos;
	}
}
