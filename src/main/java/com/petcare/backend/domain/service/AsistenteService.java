package com.petcare.backend.domain.service;

import com.petcare.backend.domain.dto.request.AsistenteRequest;
import com.petcare.backend.domain.dto.response.AsistenteResponse;
import com.petcare.backend.domain.repository.AsistenteRepository;
import com.petcare.backend.domain.repository.RolRepository;
import com.petcare.backend.domain.repository.UsuarioRepository;
import com.petcare.backend.persistence.entity.Asistente;
import com.petcare.backend.persistence.entity.Rol;
import com.petcare.backend.persistence.entity.Usuario;
import com.petcare.backend.persistence.enums.RoleName;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AsistenteService {

	private final AsistenteRepository asistenteRepository;
	private final UsuarioRepository usuarioRepository;
	private final RolRepository rolRepository;
	private final PasswordEncoder passwordEncoder;

	@Transactional
	public AsistenteResponse create(AsistenteRequest request) {
		validateUniqueEmail(request.email(), null);
		validateUniqueDocument(request.numeroDocumento(), null);

		Usuario usuario = resolveUsuarioForCreate(request);
		validateUsuarioAvailable(usuario, null);
		ensureAsistenteRole(usuario);
		usuario = usuarioRepository.save(usuario);

		LocalDateTime now = LocalDateTime.now();
		Asistente asistente = Asistente.builder()
				.usuario(usuario)
				.nombres(normalizeText(request.nombres()))
				.apellidos(normalizeText(request.apellidos()))
				.tipoDocumento(normalizeText(request.tipoDocumento()))
				.numeroDocumento(normalizeText(request.numeroDocumento()))
				.telefono(request.telefono().trim())
				.email(normalizeEmail(request.email()))
				.funciones(normalizeText(request.funciones()))
				.active(true)
				.createdAt(now)
				.updatedAt(now)
				.build();

		return toResponse(asistenteRepository.save(asistente));
	}

	@Transactional(readOnly = true)
	public List<AsistenteResponse> findAll(String search, Boolean active) {
		String normalizedSearch = search == null || search.isBlank() ? null : search.trim();
		return asistenteRepository.search(normalizedSearch, active).stream()
				.map(this::toResponse)
				.toList();
	}

	@Transactional(readOnly = true)
	public AsistenteResponse findById(Long id) {
		return toResponse(findEntityById(id));
	}

	@Transactional
	public AsistenteResponse update(Long id, AsistenteRequest request) {
		Asistente asistente = findEntityById(id);
		validateUniqueEmail(request.email(), id);
		validateUniqueDocument(request.numeroDocumento(), id);

		Usuario usuario = resolveUsuarioForUpdate(request, asistente);
		validateUsuarioAvailable(usuario, id);
		ensureAsistenteRole(usuario);
		usuario = usuarioRepository.save(usuario);

		asistente.setUsuario(usuario);
		asistente.setNombres(normalizeText(request.nombres()));
		asistente.setApellidos(normalizeText(request.apellidos()));
		asistente.setTipoDocumento(normalizeText(request.tipoDocumento()));
		asistente.setNumeroDocumento(normalizeText(request.numeroDocumento()));
		asistente.setTelefono(request.telefono().trim());
		asistente.setEmail(normalizeEmail(request.email()));
		asistente.setFunciones(normalizeText(request.funciones()));
		asistente.setUpdatedAt(LocalDateTime.now());

		return toResponse(asistenteRepository.save(asistente));
	}

	@Transactional
	public AsistenteResponse activate(Long id) {
		Asistente asistente = findEntityById(id);
		asistente.setActive(true);
		asistente.setUpdatedAt(LocalDateTime.now());
		if (asistente.getUsuario() != null) {
			asistente.getUsuario().setActive(true);
		}
		return toResponse(asistenteRepository.save(asistente));
	}

	@Transactional
	public void deactivate(Long id) {
		Asistente asistente = findEntityById(id);
		asistente.setActive(false);
		asistente.setUpdatedAt(LocalDateTime.now());
		if (asistente.getUsuario() != null) {
			asistente.getUsuario().setActive(false);
		}
		asistenteRepository.save(asistente);
	}

	private Usuario resolveUsuarioForCreate(AsistenteRequest request) {
		if (request.usuarioId() != null) {
			return findUsuario(request.usuarioId());
		}
		if (request.password() == null || request.password().isBlank()) {
			throw new IllegalArgumentException("La contrasena es obligatoria al crear un asistente sin usuario existente.");
		}
		String email = normalizeEmail(request.email());
		if (usuarioRepository.existsByEmail(email)) {
			throw new IllegalArgumentException("El correo ya esta registrado para un usuario.");
		}
		return Usuario.builder()
				.fullName(fullName(request.nombres(), request.apellidos()))
				.email(email)
				.password(passwordEncoder.encode(request.password()))
				.active(true)
				.createdAt(LocalDateTime.now())
				.roles(new HashSet<>())
				.build();
	}

	private Usuario resolveUsuarioForUpdate(AsistenteRequest request, Asistente asistente) {
		if (request.usuarioId() != null) {
			return findUsuario(request.usuarioId());
		}
		Usuario usuario = asistente.getUsuario();
		if (usuario == null) {
			return resolveUsuarioForCreate(request);
		}
		usuario.setFullName(fullName(request.nombres(), request.apellidos()));
		usuario.setEmail(normalizeEmail(request.email()));
		if (request.password() != null && !request.password().isBlank()) {
			usuario.setPassword(passwordEncoder.encode(request.password()));
		}
		return usuario;
	}

	private void ensureAsistenteRole(Usuario usuario) {
		Rol role = rolRepository.findByName(RoleName.ROLE_ASISTENTE)
				.orElseThrow(() -> new IllegalStateException("Rol base no encontrado: " + RoleName.ROLE_ASISTENTE));
		usuario.getRoles().add(role);
	}

	private Usuario findUsuario(Long id) {
		return usuarioRepository.findById(id)
				.orElseThrow(() -> new EntityNotFoundException("Usuario no encontrado."));
	}

	private Asistente findEntityById(Long id) {
		return asistenteRepository.findById(id)
				.orElseThrow(() -> new EntityNotFoundException("Asistente no encontrado."));
	}

	private void validateUsuarioAvailable(Usuario usuario, Long currentAsistenteId) {
		if (usuario == null) {
			return;
		}
		asistenteRepository.findByUsuarioId(usuario.getId())
				.filter(existing -> currentAsistenteId == null || !existing.getId().equals(currentAsistenteId))
				.ifPresent(existing -> {
					throw new IllegalArgumentException("El usuario ya esta relacionado a otro asistente.");
				});
	}

	private void validateUniqueEmail(String email, Long currentId) {
		String normalizedEmail = normalizeEmail(email);
		asistenteRepository.findByEmail(normalizedEmail)
				.filter(asistente -> currentId == null || !asistente.getId().equals(currentId))
				.ifPresent(asistente -> {
					throw new IllegalArgumentException("El correo ya esta registrado para otro asistente.");
				});
	}

	private void validateUniqueDocument(String numeroDocumento, Long currentId) {
		String normalizedDocument = normalizeText(numeroDocumento);
		asistenteRepository.findByNumeroDocumento(normalizedDocument)
				.filter(asistente -> currentId == null || !asistente.getId().equals(currentId))
				.ifPresent(asistente -> {
					throw new IllegalArgumentException("El documento ya esta registrado para otro asistente.");
				});
	}

	private AsistenteResponse toResponse(Asistente asistente) {
		Long usuarioId = asistente.getUsuario() == null ? null : asistente.getUsuario().getId();
		return new AsistenteResponse(
				asistente.getId(),
				usuarioId,
				asistente.getNombres(),
				asistente.getApellidos(),
				asistente.getTipoDocumento(),
				asistente.getNumeroDocumento(),
				asistente.getTelefono(),
				asistente.getEmail(),
				asistente.getFunciones(),
				asistente.getActive(),
				asistente.getCreatedAt(),
				asistente.getUpdatedAt()
		);
	}

	private String normalizeEmail(String value) {
		return value.trim().toLowerCase();
	}

	private String normalizeText(String value) {
		return value.trim();
	}

	private String fullName(String nombres, String apellidos) {
		return normalizeText(nombres) + " " + normalizeText(apellidos);
	}
}
