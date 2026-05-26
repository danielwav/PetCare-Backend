package com.petcare.backend.domain.service;

import com.petcare.backend.domain.dto.request.InasistenciaRequest;
import com.petcare.backend.domain.dto.response.InasistenciaResponse;
import com.petcare.backend.domain.repository.CitaRepository;
import com.petcare.backend.domain.repository.InasistenciaRepository;
import com.petcare.backend.persistence.entity.Cita;
import com.petcare.backend.persistence.entity.Inasistencia;
import com.petcare.backend.persistence.enums.EstadoCita;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class InasistenciaService {

	private final InasistenciaRepository inasistenciaRepository;
	private final CitaRepository citaRepository;

	@Transactional
	public InasistenciaResponse register(Long citaId, InasistenciaRequest request, String registradoPor) {
		Cita cita = citaRepository.findById(citaId)
				.orElseThrow(() -> new EntityNotFoundException("Cita no encontrada."));

		validateCanRegisterNoShow(cita);
		if (inasistenciaRepository.existsByCitaId(citaId)) {
			throw new IllegalArgumentException("La inasistencia ya fue registrada para esta cita.");
		}

		LocalDateTime now = LocalDateTime.now();
		cita.setEstado(EstadoCita.NO_ASISTIO);
		cita.setRequiereConfirmacion(false);
		cita.setUpdatedAt(now);

		Inasistencia inasistencia = Inasistencia.builder()
				.cita(cita)
				.duenio(cita.getDuenio())
				.mascota(cita.getMascota())
				.observacion(normalizeText(request.observacion()))
				.registradoPor(normalizeText(registradoPor))
				.fechaRegistro(now)
				.build();

		return toResponse(inasistenciaRepository.save(inasistencia));
	}

	@Transactional(readOnly = true)
	public List<InasistenciaResponse> findAll(Long duenioId, LocalDate fechaInicio, LocalDate fechaFin) {
		LocalDateTime start = fechaInicio == null ? null : fechaInicio.atStartOfDay();
		LocalDateTime end = fechaFin == null ? null : fechaFin.atTime(LocalTime.MAX);

		if (fechaInicio != null && fechaFin != null && fechaFin.isBefore(fechaInicio)) {
			throw new IllegalArgumentException("La fecha final no puede ser anterior a la fecha inicial.");
		}

		return inasistenciaRepository.search(duenioId, start, end).stream()
				.map(this::toResponse)
				.toList();
	}

	@Transactional(readOnly = true)
	public InasistenciaResponse findById(Long id) {
		return toResponse(inasistenciaRepository.findById(id)
				.orElseThrow(() -> new EntityNotFoundException("Inasistencia no encontrada.")));
	}

	private void validateCanRegisterNoShow(Cita cita) {
		if (cita.getEstado() != EstadoCita.PROGRAMADA && cita.getEstado() != EstadoCita.CONFIRMADA) {
			throw new IllegalArgumentException("Solo se puede registrar inasistencia en citas programadas o confirmadas.");
		}

		LocalDateTime scheduledAt = LocalDateTime.of(cita.getFecha(), cita.getHoraInicio());
		if (scheduledAt.isAfter(LocalDateTime.now())) {
			throw new IllegalArgumentException("No se puede registrar inasistencia antes de la fecha y hora de la cita.");
		}
	}

	private InasistenciaResponse toResponse(Inasistencia inasistencia) {
		Cita cita = inasistencia.getCita();
		return new InasistenciaResponse(
				inasistencia.getId(),
				cita.getId(),
				inasistencia.getDuenio().getId(),
				fullName(inasistencia.getDuenio().getNombres(), inasistencia.getDuenio().getApellidos()),
				inasistencia.getMascota().getId(),
				inasistencia.getMascota().getNombre(),
				cita.getFecha(),
				cita.getHoraInicio(),
				inasistencia.getObservacion(),
				inasistencia.getRegistradoPor(),
				inasistencia.getFechaRegistro()
		);
	}

	private String normalizeText(String value) {
		return value.trim();
	}

	private String fullName(String nombres, String apellidos) {
		return nombres + " " + apellidos;
	}
}
