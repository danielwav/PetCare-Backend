package com.petcare.backend.domain.service;

import com.petcare.backend.domain.dto.request.UpdateUserRequest;
import com.petcare.backend.domain.dto.request.UpdateUserRolesRequest;
import com.petcare.backend.domain.dto.response.UserResponse;
import com.petcare.backend.domain.repository.RolRepository;
import com.petcare.backend.domain.repository.UsuarioRepository;
import com.petcare.backend.persistence.entity.Rol;
import com.petcare.backend.persistence.entity.Usuario;
import com.petcare.backend.persistence.enums.RoleName;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UsuarioService {

    private final UsuarioRepository usuarioRepository;
    private final RolRepository rolRepository;

    public List<UserResponse> findAll() {
        return usuarioRepository.findAll().stream()
                .map(this::toUserResponse)
                .toList();
    }

    public UserResponse findById(Long id) {
        return toUserResponse(findUsuario(id));
    }

    @Transactional
    public UserResponse update(Long id, UpdateUserRequest request) {
        Usuario usuario = findUsuario(id);
        String email = request.email().toLowerCase();
        if (!usuario.getEmail().equals(email) && usuarioRepository.existsByEmail(email)) {
            throw new IllegalArgumentException("El correo ya esta registrado.");
        }
        usuario.setFullName(request.fullName());
        usuario.setEmail(email);
        usuario.setTelefono(request.telefono());
        return toUserResponse(usuarioRepository.save(usuario));
    }

    @Transactional
    public UserResponse deactivate(Long id) {
        Usuario usuario = findUsuario(id);
        usuario.setActive(false);
        return toUserResponse(usuarioRepository.save(usuario));
    }

    @Transactional
    public UserResponse toggleActive(Long id) {
        Usuario usuario = findUsuario(id);
        usuario.setActive(!usuario.getActive());
        return toUserResponse(usuarioRepository.save(usuario));
    }

    @Transactional
    public UserResponse updateRoles(Long id, UpdateUserRolesRequest request) {
        Usuario usuario = findUsuario(id);
        Set<Rol> roles = request.roles().stream()
                .map(roleName -> {
                    try {
                        RoleName rn = RoleName.valueOf(roleName);
                        return rolRepository.findByName(rn)
                                .orElseThrow(() -> new IllegalArgumentException("Rol no encontrado: " + roleName));
                    } catch (IllegalArgumentException e) {
                        throw new IllegalArgumentException("Rol invalido: " + roleName);
                    }
                })
                .collect(Collectors.toSet());
        if (roles.isEmpty()) {
            throw new IllegalArgumentException("Debe asignar al menos un rol.");
        }
        usuario.setRoles(roles);
        return toUserResponse(usuarioRepository.save(usuario));
    }

    public Usuario findUsuario(Long id) {
        return usuarioRepository.findById(id)
                .orElseThrow(() -> new UsernameNotFoundException("Usuario no encontrado con id: " + id));
    }

    private UserResponse toUserResponse(Usuario usuario) {
        Set<String> roles = usuario.getRoles().stream()
                .map(role -> role.getName().name())
                .collect(Collectors.toSet());
        return new UserResponse(
                usuario.getId(),
                usuario.getFullName(),
                usuario.getEmail(),
                usuario.getTelefono(),
                usuario.getActive(),
                roles
        );
    }
}
