package com.petcare.backend.domain.service;

import com.petcare.backend.domain.dto.request.DuenioRequest;
import com.petcare.backend.domain.dto.response.DuenioResponse;
import com.petcare.backend.domain.repository.DuenioRepository;
import com.petcare.backend.domain.repository.UsuarioRepository;
import com.petcare.backend.persistence.entity.Duenio;
import com.petcare.backend.persistence.entity.Usuario;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class DuenioService {

	private final DuenioRepository duenioRepository;
	private final UsuarioRepository usuarioRepository;

	@Transactional
	public DuenioResponse create(DuenioRequest request) {
		validateUniqueEmail(request.email(), null);
		validateUniqueDocument(request.numeroDocumento(), null);

		Usuario usuario = findUsuarioIfPresent(request.usuarioId());
		if (usuario != null && duenioRepository.findByUsuarioId(usuario.getId()).isPresent()) {
			throw new IllegalArgumentException("El usuario ya esta relacionado a un duenio.");
		}

		LocalDateTime now = LocalDateTime.now();
		Duenio duenio = Duenio.builder()
				.usuario(usuario)
				.nombres(normalizeText(request.nombres()))
				.apellidos(normalizeText(request.apellidos()))
				.tipoDocumento(normalizeText(request.tipoDocumento()))
				.numeroDocumento(request.numeroDocumento().trim())
				.telefono(request.telefono().trim())
				.email(normalizeEmail(request.email()))
				.direccion(normalizeNullableText(request.direccion()))
				.active(true)
				.createdAt(now)
				.updatedAt(now)
				.build();

		return toResponse(duenioRepository.save(duenio));
	}

	@Transactional(readOnly = true)
	public List<DuenioResponse> findAll(String search, Boolean active) {
		String normalizedSearch = search == null || search.isBlank() ? null : search.trim();
		return duenioRepository.search(normalizedSearch, active).stream()
				.map(this::toResponse)
				.toList();
	}

	@Transactional(readOnly = true)
	public DuenioResponse findById(Long id) {
		return toResponse(findEntityById(id));
	}

	@Transactional
	public DuenioResponse update(Long id, DuenioRequest request) {
		Duenio duenio = findEntityById(id);

		validateUniqueEmail(request.email(), id);
		validateUniqueDocument(request.numeroDocumento(), id);

		Usuario usuario = findUsuarioIfPresent(request.usuarioId());
		if (usuario != null) {
			duenioRepository.findByUsuarioId(usuario.getId())
					.filter(existing -> !existing.getId().equals(id))
					.ifPresent(existing -> {
						throw new IllegalArgumentException("El usuario ya esta relacionado a otro duenio.");
					});
		}

		duenio.setUsuario(usuario);
		duenio.setNombres(normalizeText(request.nombres()));
		duenio.setApellidos(normalizeText(request.apellidos()));
		duenio.setTipoDocumento(normalizeText(request.tipoDocumento()));
		duenio.setNumeroDocumento(request.numeroDocumento().trim());
		duenio.setTelefono(request.telefono().trim());
		duenio.setEmail(normalizeEmail(request.email()));
		duenio.setDireccion(normalizeNullableText(request.direccion()));
		duenio.setUpdatedAt(LocalDateTime.now());

		return toResponse(duenioRepository.save(duenio));
	}

	@Transactional
	public void deactivate(Long id) {
		Duenio duenio = findEntityById(id);
		duenio.setActive(false);
		duenio.setUpdatedAt(LocalDateTime.now());
		duenioRepository.save(duenio);
	}

	private Duenio findEntityById(Long id) {
		return duenioRepository.findById(id)
				.orElseThrow(() -> new EntityNotFoundException("Duenio no encontrado."));
	}

	private Usuario findUsuarioIfPresent(Long usuarioId) {
		if (usuarioId == null) {
			return null;
		}

		return usuarioRepository.findById(usuarioId)
				.orElseThrow(() -> new EntityNotFoundException("Usuario no encontrado."));
	}

	private void validateUniqueEmail(String email, Long currentId) {
		String normalizedEmail = normalizeEmail(email);
		duenioRepository.findByEmail(normalizedEmail)
				.filter(duenio -> currentId == null || !duenio.getId().equals(currentId))
				.ifPresent(duenio -> {
					throw new IllegalArgumentException("El correo ya esta registrado para otro duenio.");
				});
	}

	private void validateUniqueDocument(String numeroDocumento, Long currentId) {
		String normalizedDocument = numeroDocumento.trim();
		duenioRepository.findByNumeroDocumento(normalizedDocument)
				.filter(duenio -> currentId == null || !duenio.getId().equals(currentId))
				.ifPresent(duenio -> {
					throw new IllegalArgumentException("El documento ya esta registrado para otro duenio.");
				});
	}

	private DuenioResponse toResponse(Duenio duenio) {
		Long usuarioId = duenio.getUsuario() == null ? null : duenio.getUsuario().getId();
		return new DuenioResponse(
				duenio.getId(),
				usuarioId,
				duenio.getNombres(),
				duenio.getApellidos(),
				duenio.getTipoDocumento(),
				duenio.getNumeroDocumento(),
				duenio.getTelefono(),
				duenio.getEmail(),
				duenio.getDireccion(),
				duenio.getActive(),
				duenio.getCreatedAt(),
				duenio.getUpdatedAt()
		);
	}

	private String normalizeEmail(String value) {
		return value.trim().toLowerCase();
	}

	private String normalizeText(String value) {
		return value.trim();
	}

	private String normalizeNullableText(String value) {
		return value == null || value.isBlank() ? null : value.trim();
	}
}
