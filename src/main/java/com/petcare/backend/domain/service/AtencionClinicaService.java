package com.petcare.backend.domain.service;

import com.petcare.backend.domain.dto.request.AtencionClinicaRequest;
import com.petcare.backend.domain.dto.response.AtencionClinicaResponse;
import com.petcare.backend.domain.dto.response.ControlMensualMascotaResponse;
import com.petcare.backend.domain.dto.response.HistoriaClinicaResponse;
import com.petcare.backend.domain.repository.AtencionClinicaRepository;
import com.petcare.backend.domain.repository.CitaRepository;
import com.petcare.backend.domain.repository.ControlMensualMascotaRepository;
import com.petcare.backend.domain.repository.MascotaRepository;
import com.petcare.backend.persistence.entity.AtencionClinica;
import com.petcare.backend.persistence.entity.Cita;
import com.petcare.backend.persistence.entity.ControlMensualMascota;
import com.petcare.backend.persistence.entity.Duenio;
import com.petcare.backend.persistence.entity.Mascota;
import com.petcare.backend.persistence.entity.Veterinario;
import com.petcare.backend.persistence.enums.EstadoCita;
import com.petcare.backend.persistence.enums.EstadoMascota;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AtencionClinicaService {

	private final AtencionClinicaRepository atencionClinicaRepository;
	private final ControlMensualMascotaRepository controlMensualMascotaRepository;
	private final CitaRepository citaRepository;
	private final MascotaRepository mascotaRepository;
	private final AuthenticatedDuenioService authenticatedDuenioService;

	@Transactional
	public AtencionClinicaResponse register(Long citaId, AtencionClinicaRequest request, Long clinicaId) {
		Cita cita = citaRepository.findById(citaId)
				.orElseThrow(() -> new EntityNotFoundException("Cita no encontrada."));

		validateCitaBelongsToClinic(cita, clinicaId);
		validateCanRegisterAttention(cita);
		if (atencionClinicaRepository.existsByCitaId(citaId)) {
			throw new IllegalArgumentException("La atencion clinica ya fue registrada para esta cita.");
		}

		AtencionClinica atencion = AtencionClinica.builder()
				.cita(cita)
				.mascota(cita.getMascota())
				.veterinario(cita.getVeterinario())
				.motivo(normalizeText(request.motivo()))
				.diagnostico(normalizeText(request.diagnostico()))
				.tratamiento(normalizeText(request.tratamiento()))
				.recomendaciones(normalizeOptionalText(request.recomendaciones()))
				.observacionesClinicas(normalizeOptionalText(request.observacionesClinicas()))
				.notasInternas(normalizeOptionalText(request.notasInternas()))
				.fechaRegistro(LocalDateTime.now())
				.build();

		cita.setEstado(EstadoCita.ATENDIDA);
		cita.setRequiereConfirmacion(false);
		cita.setUpdatedAt(LocalDateTime.now());

		Mascota mascota = cita.getMascota();
		mascota.setEstado(request.estadoMascota());
		mascota.setFechaEstado(LocalDateTime.now());
		mascota.setVeterinarioEstado(cita.getVeterinario().getNombres() + " " + cita.getVeterinario().getApellidos());

		return toResponse(atencionClinicaRepository.save(atencion));
	}

	@Transactional(readOnly = true)
	public AtencionClinicaResponse findById(Long id) {
		return toResponse(findAtencion(id));
	}

	@Transactional(readOnly = true)
	public AtencionClinicaResponse findByIdScoped(Long id, Long clinicaId) {
		AtencionClinica atencion = findAtencion(id);
		validateMascotaBelongsToClinic(atencion.getMascota(), clinicaId);
		return toResponse(atencion);
	}

	@Transactional(readOnly = true)
	public AtencionClinicaResponse findByIdForDuenio(Long id, String email) {
		AtencionClinica atencion = findAtencion(id);
		validateMascotaBelongsToAuthenticatedDuenio(atencion.getMascota(), email);
		return toResponse(atencion);
	}

	@Transactional(readOnly = true)
	public HistoriaClinicaResponse findHistoriaClinicaByMascotaScoped(Long mascotaId, Long clinicaId) {
		Mascota mascota = findMascota(mascotaId);
		validateMascotaBelongsToClinic(mascota, clinicaId);
		return findHistoriaClinicaByMascota(mascotaId);
	}

	@Transactional(readOnly = true)
	public HistoriaClinicaResponse findHistoriaClinicaByMascotaForDuenio(Long mascotaId, String email) {
		Mascota mascota = findMascota(mascotaId);
		validateMascotaBelongsToAuthenticatedDuenio(mascota, email);
		return findHistoriaClinicaByMascota(mascotaId);
	}

	@Transactional(readOnly = true)
	public HistoriaClinicaResponse findHistoriaClinicaByMascota(Long mascotaId) {
		Mascota mascota = mascotaRepository.findById(mascotaId)
				.orElseThrow(() -> new EntityNotFoundException("Mascota no encontrada."));
		List<AtencionClinicaResponse> atenciones = atencionClinicaRepository.findByMascotaIdOrderByFechaRegistroDesc(mascotaId)
				.stream()
				.map(this::toResponse)
				.toList();
		List<ControlMensualMascotaResponse> controles = controlMensualMascotaRepository
				.findByMascotaIdOrderByFechaControlDesc(mascotaId)
				.stream()
				.map(this::toControlResponse)
				.toList();

		return new HistoriaClinicaResponse(
				mascota.getId(),
				mascota.getNombre(),
				mascota.getDuenio().getId(),
				fullName(mascota.getDuenio().getNombres(), mascota.getDuenio().getApellidos()),
				mascota.getEstado(),
				mascota.getFechaEstado(),
				mascota.getVeterinarioEstado(),
				atenciones,
				controles
		);
	}

	private AtencionClinica findAtencion(Long id) {
		return atencionClinicaRepository.findById(id)
				.orElseThrow(() -> new EntityNotFoundException("Atencion clinica no encontrada."));
	}

	private Mascota findMascota(Long id) {
		return mascotaRepository.findById(id)
				.orElseThrow(() -> new EntityNotFoundException("Mascota no encontrada."));
	}

	private void validateMascotaBelongsToClinic(Mascota mascota, Long clinicaId) {
		if (mascota.getClinica() == null || !mascota.getClinica().getId().equals(clinicaId)) {
			throw new AccessDeniedException("La mascota indicada no pertenece a tu clinica.");
		}
	}

private void validateMascotaBelongsToAuthenticatedDuenio(Mascota mascota, String email) {
		Duenio duenio = authenticatedDuenioService.findByAuthenticatedEmail(email);
		if (mascota.getDuenio() == null || !mascota.getDuenio().getId().equals(duenio.getId())) {
			throw new AccessDeniedException("No tienes permiso para consultar la historia clinica de esta mascota.");
		}
	}

	private void validateCitaBelongsToClinic(Cita cita, Long clinicaId) {
		if (cita.getClinica() == null || !cita.getClinica().getId().equals(clinicaId)) {
			throw new AccessDeniedException("La cita indicada no pertenece a tu clinica.");
		}
	}

	private void validateCanRegisterAttention(Cita cita) {
		if (cita.getEstado() != EstadoCita.PROGRAMADA && cita.getEstado() != EstadoCita.CONFIRMADA) {
			throw new IllegalArgumentException("Solo se puede registrar atencion en citas programadas o confirmadas.");
		}
		if (LocalDateTime.of(cita.getFecha(), cita.getHoraInicio()).isAfter(LocalDateTime.now())) {
			throw new IllegalArgumentException("No se puede registrar atencion antes de la fecha y hora de la cita.");
		}
	}

	private AtencionClinicaResponse toResponse(AtencionClinica atencion) {
		Veterinario veterinario = atencion.getVeterinario();
		return new AtencionClinicaResponse(
				atencion.getId(),
				atencion.getCita().getId(),
				atencion.getMascota().getId(),
				atencion.getMascota().getNombre(),
				veterinario.getId(),
				fullName(veterinario.getNombres(), veterinario.getApellidos()),
				atencion.getMotivo(),
				atencion.getDiagnostico(),
				atencion.getTratamiento(),
				atencion.getRecomendaciones(),
				atencion.getObservacionesClinicas(),
				atencion.getNotasInternas(),
				atencion.getFechaRegistro()
		);
	}

	private ControlMensualMascotaResponse toControlResponse(ControlMensualMascota control) {
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
