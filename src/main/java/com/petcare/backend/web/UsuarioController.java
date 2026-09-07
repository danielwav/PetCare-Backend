package com.petcare.backend.web;

import com.petcare.backend.domain.dto.request.CreateInternalUserRequest;
import com.petcare.backend.domain.dto.request.UpdateUserRequest;
import com.petcare.backend.domain.dto.request.UpdateUserRolesRequest;
import com.petcare.backend.domain.dto.response.UserResponse;
import com.petcare.backend.domain.service.AuthService;
import com.petcare.backend.domain.service.ClinicaService;
import com.petcare.backend.domain.service.UsuarioService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/usuarios")
@RequiredArgsConstructor
public class UsuarioController {

    private final UsuarioService usuarioService;
    private final AuthService authService;
    private final ClinicaService clinicaService;

    @PostMapping("/internal")
    @ResponseStatus(HttpStatus.CREATED)
    public Map<String, String> createInternal(@Valid @RequestBody CreateInternalUserRequest request, Authentication authentication) {
        String token = authService.createInternalUser(request, clinicaService.resolveClinicaId(authentication.getName()));
        return Map.of("message", "Usuario creado exitosamente.", "activationToken", token);
    }

    @GetMapping
    public List<UserResponse> findAll(Authentication authentication) {
        return usuarioService.findAll(clinicaService.resolveClinicaId(authentication.getName()));
    }

    @GetMapping("/{id}")
    public UserResponse findById(@PathVariable Long id, Authentication authentication) {
        return usuarioService.findById(id, clinicaService.resolveClinicaId(authentication.getName()));
    }

    @PutMapping("/{id}")
    public UserResponse update(@PathVariable Long id, @Valid @RequestBody UpdateUserRequest request, Authentication authentication) {
        return usuarioService.update(id, request, clinicaService.resolveClinicaId(authentication.getName()));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id, Authentication authentication) {
        usuarioService.hardDelete(id, clinicaService.resolveClinicaId(authentication.getName()));
    }

    @PatchMapping("/{id}/activate")
    public UserResponse toggleActive(@PathVariable Long id, Authentication authentication) {
        return usuarioService.toggleActive(id, clinicaService.resolveClinicaId(authentication.getName()));
    }

    @PutMapping("/{id}/roles")
    public UserResponse updateRoles(@PathVariable Long id, @Valid @RequestBody UpdateUserRolesRequest request, Authentication authentication) {
        return usuarioService.updateRoles(id, request, clinicaService.resolveClinicaId(authentication.getName()));
    }
}
