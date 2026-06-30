package com.petcare.backend.domain.service;

import com.petcare.backend.domain.dto.request.HorarioVeterinarioRequest;
import com.petcare.backend.domain.dto.request.VeterinarioRequest;
import com.petcare.backend.domain.dto.response.DisponibilidadVeterinarioResponse;
import com.petcare.backend.domain.dto.response.HorarioVeterinarioResponse;
import com.petcare.backend.domain.dto.response.VeterinarioResponse;
import com.petcare.backend.domain.repository.HorarioVeterinarioRepository;
import com.petcare.backend.domain.repository.UsuarioRepository;
import com.petcare.backend.domain.repository.VeterinarioRepository;
import com.petcare.backend.persistence.entity.HorarioVeterinario;
import com.petcare.backend.persistence.entity.Usuario;
import com.petcare.backend.persistence.entity.Veterinario;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
public class VeterinarioService {

	private final VeterinarioRepository veterinarioRepository;
	private final HorarioVeterinarioRepository horarioVeterinarioRepository;
	private final UsuarioRepository usuarioRepository;

	@Transactional
	public VeterinarioResponse create(VeterinarioRequest request) {
		validateUniqueColegiatura(request.numeroColegiatura(), null);

		Usuario usuario = findUsuarioIfPresent(request.usuarioId());
		if (usuario == null) {
			throw new IllegalArgumentException("Debe seleccionar un usuario veterinario.");
		}
		validateUsuarioAvailable(usuario, null);
		validateHorarios(request.horarios());

		LocalDateTime now = LocalDateTime.now();
		Veterinario veterinario = Veterinario.builder()
				.usuario(usuario)
				.nombres(coalesce(request.nombres(), usuario.getFullName()))
				.apellidos("")
				.numeroColegiatura(normalizeText(request.numeroColegiatura()))
				.especialidad(normalizeText(request.especialidad()))
				.telefono(coalesce(request.telefono(), usuario.getTelefono()))
				.email(coalesce(request.email(), usuario.getEmail()))
				.active(true)
				.createdAt(now)
				.updatedAt(now)
				.build();

		replaceHorarios(veterinario, request.horarios());
		return toResponse(veterinarioRepository.save(veterinario));
	}

	@Transactional
	public VeterinarioResponse update(Long id, VeterinarioRequest request) {
		Veterinario veterinario = findEntityById(id);

		validateUniqueColegiatura(request.numeroColegiatura(), id);

		Usuario usuario = request.usuarioId() != null
				? usuarioRepository.findById(request.usuarioId())
						.orElseThrow(() -> new EntityNotFoundException("Usuario no encontrado."))
				: veterinario.getUsuario();
		validateUsuarioAvailable(usuario, id);
		validateHorarios(request.horarios());

		veterinario.setUsuario(usuario);
		veterinario.setNombres(coalesce(request.nombres(), usuario != null ? usuario.getFullName() : ""));
		veterinario.setApellidos("");
		veterinario.setNumeroColegiatura(normalizeText(request.numeroColegiatura()));
		veterinario.setEspecialidad(normalizeText(request.especialidad()));
		veterinario.setTelefono(coalesce(request.telefono(), usuario != null ? usuario.getTelefono() : ""));
		veterinario.setEmail(coalesce(request.email(), usuario != null ? usuario.getEmail() : ""));
		veterinario.setUpdatedAt(LocalDateTime.now());

		replaceHorarios(veterinario, request.horarios());
		return toResponse(veterinarioRepository.save(veterinario));
	}

	private String coalesce(String value, String fallback) {
		return value != null && !value.isBlank() ? value.trim() : (fallback != null ? fallback : "");
	}

	@Transactional(readOnly = true)
	public List<VeterinarioResponse> findAll(String search, Boolean active) {
		String normalizedSearch = search == null || search.isBlank() ? null : search.trim();
		return veterinarioRepository.search(normalizedSearch, active).stream()
				.filter(v -> v.getUsuario() != null)
				.map(this::toResponse)
				.toList();
	}

	@Transactional(readOnly = true)
	public VeterinarioResponse findById(Long id) {
		return toResponse(findEntityById(id));
	}

	@Transactional
	public void deactivate(Long id) {
		Veterinario veterinario = findEntityById(id);
		veterinario.setActive(false);
		veterinario.setUpdatedAt(LocalDateTime.now());
		veterinario.getHorarios().forEach(horario -> horario.setActive(false));
		veterinarioRepository.save(veterinario);
	}

	@Transactional(readOnly = true)
	public DisponibilidadVeterinarioResponse findDisponibilidad(Long id, LocalDate fecha, Integer duracionMinutos) {
		if (fecha.isBefore(LocalDate.now())) {
			throw new IllegalArgumentException("La fecha de disponibilidad no puede estar en el pasado.");
		}

		Veterinario veterinario = findEntityById(id);
		if (!veterinario.getActive()) {
			throw new IllegalArgumentException("El veterinario no esta activo.");
		}

		List<HorarioVeterinario> horarios = horarioVeterinarioRepository
				.findByVeterinarioIdAndDiaSemanaAndActiveTrueOrderByHoraInicioAsc(id, fecha.getDayOfWeek());
		List<LocalTime> slots = horarios.stream()
				.flatMap(horario -> buildSlots(horario, duracionMinutos).stream())
				.sorted()
				.toList();

		return new DisponibilidadVeterinarioResponse(
				veterinario.getId(),
				fullName(veterinario),
				fecha,
				slots
		);
	}

	private List<LocalTime> buildSlots(HorarioVeterinario horario, Integer requestedDuration) {
		int duration = requestedDuration == null ? horario.getDuracionBloqueMinutos() : requestedDuration;
		if (duration <= 0) {
			throw new IllegalArgumentException("La duracion debe ser mayor a cero.");
		}

		List<LocalTime> slots = new ArrayList<>();
		LocalTime cursor = horario.getHoraInicio();
		while (!cursor.plusMinutes(duration).isAfter(horario.getHoraFin())) {
			slots.add(cursor);
			cursor = cursor.plusMinutes(horario.getDuracionBloqueMinutos());
		}
		return slots;
	}

	private Veterinario findEntityById(Long id) {
		return veterinarioRepository.findById(id)
				.orElseThrow(() -> new EntityNotFoundException("Veterinario no encontrado."));
	}

	private Usuario findUsuarioIfPresent(Long usuarioId) {
		if (usuarioId == null) {
			return null;
		}

		return usuarioRepository.findById(usuarioId)
				.orElseThrow(() -> new EntityNotFoundException("Usuario no encontrado."));
	}

	private void validateUsuarioAvailable(Usuario usuario, Long currentVeterinarioId) {
		if (usuario == null) {
			return;
		}

		veterinarioRepository.findByUsuarioId(usuario.getId())
				.filter(existing -> currentVeterinarioId == null || !existing.getId().equals(currentVeterinarioId))
				.ifPresent(existing -> {
					throw new IllegalArgumentException("El usuario ya esta relacionado a otro veterinario.");
				});
	}

	private void validateUniqueEmail(String email, Long currentId) {
		if (email == null || email.isBlank()) return;
		String normalizedEmail = normalizeEmail(email);
		veterinarioRepository.findByEmail(normalizedEmail)
				.filter(veterinario -> currentId == null || !veterinario.getId().equals(currentId))
				.ifPresent(veterinario -> {
					throw new IllegalArgumentException("El correo ya esta registrado para otro veterinario.");
				});
	}

	private void validateUniqueColegiatura(String numeroColegiatura, Long currentId) {
		String normalizedColegiatura = normalizeText(numeroColegiatura);
		veterinarioRepository.findByNumeroColegiatura(normalizedColegiatura)
				.filter(veterinario -> currentId == null || !veterinario.getId().equals(currentId))
				.ifPresent(veterinario -> {
					throw new IllegalArgumentException("La colegiatura ya esta registrada para otro veterinario.");
				});
	}

	private void validateHorarios(List<HorarioVeterinarioRequest> horarios) {
		if (horarios == null || horarios.isEmpty()) {
			return;
		}

		for (HorarioVeterinarioRequest horario : horarios) {
			if (!horario.horaInicio().isBefore(horario.horaFin())) {
				throw new IllegalArgumentException("La hora de inicio debe ser menor que la hora de fin.");
			}
		}
	}

	private void replaceHorarios(Veterinario veterinario, List<HorarioVeterinarioRequest> horarios) {
		veterinario.getHorarios().clear();

		if (horarios == null) {
			return;
		}

		horarios.forEach(request -> veterinario.getHorarios().add(HorarioVeterinario.builder()
				.veterinario(veterinario)
				.diaSemana(request.diaSemana())
				.horaInicio(request.horaInicio())
				.horaFin(request.horaFin())
				.duracionBloqueMinutos(request.duracionBloqueMinutos())
				.active(true)
				.build()));
	}

	private VeterinarioResponse toResponse(Veterinario veterinario) {
		List<HorarioVeterinarioResponse> horarios = veterinario.getHorarios().stream()
				.sorted(Comparator.comparing(HorarioVeterinario::getDiaSemana)
						.thenComparing(HorarioVeterinario::getHoraInicio))
				.map(this::toHorarioResponse)
				.toList();
		Long usuarioId = veterinario.getUsuario() == null ? null : veterinario.getUsuario().getId();

		return new VeterinarioResponse(
				veterinario.getId(),
				usuarioId,
				veterinario.getNombres(),
				veterinario.getApellidos(),
				veterinario.getNumeroColegiatura(),
				veterinario.getEspecialidad(),
				veterinario.getTelefono(),
				veterinario.getEmail(),
				veterinario.getActive(),
				horarios,
				veterinario.getCreatedAt(),
				veterinario.getUpdatedAt()
		);
	}

	private HorarioVeterinarioResponse toHorarioResponse(HorarioVeterinario horario) {
		return new HorarioVeterinarioResponse(
				horario.getId(),
				horario.getDiaSemana(),
				horario.getHoraInicio(),
				horario.getHoraFin(),
				horario.getDuracionBloqueMinutos(),
				horario.getActive()
		);
	}

	private String fullName(Veterinario veterinario) {
		return veterinario.getNombres() + " " + veterinario.getApellidos();
	}

	private String normalizeEmail(String value) {
		return value == null ? null : value.trim().toLowerCase();
	}

	private String normalizeText(String value) {
		return value.trim();
	}

	private String normalizeNullableText(String value) {
		return value == null || value.isBlank() ? null : value.trim();
	}
}
