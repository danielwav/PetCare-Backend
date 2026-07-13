package com.petcare.backend.web;

import com.petcare.backend.domain.dto.request.CreateInternalUserRequest;
import com.petcare.backend.domain.dto.request.UpdateUserRequest;
import com.petcare.backend.domain.dto.request.UpdateUserRolesRequest;
import com.petcare.backend.domain.dto.response.UserResponse;
import com.petcare.backend.domain.service.AuthService;
import com.petcare.backend.domain.service.UsuarioService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/usuarios")
@RequiredArgsConstructor
public class UsuarioController {

    private final UsuarioService usuarioService;
    private final AuthService authService;

    @PostMapping("/internal")
    @ResponseStatus(HttpStatus.CREATED)
    public Map<String, String> createInternal(@Valid @RequestBody CreateInternalUserRequest request) {
        return authService.createInternalUser(request);
    }

    @GetMapping
    public List<UserResponse> findAll() {
        return usuarioService.findAll();
    }

    @GetMapping("/{id}")
    public UserResponse findById(@PathVariable Long id) {
        return usuarioService.findById(id);
    }

    @PutMapping("/{id}")
    public UserResponse update(@PathVariable Long id, @Valid @RequestBody UpdateUserRequest request) {
        return usuarioService.update(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        usuarioService.hardDelete(id);
    }

    @PatchMapping("/{id}/activate")
    public UserResponse toggleActive(@PathVariable Long id) {
        return usuarioService.toggleActive(id);
    }

    @PutMapping("/{id}/roles")
    public UserResponse updateRoles(@PathVariable Long id, @Valid @RequestBody UpdateUserRolesRequest request) {
        return usuarioService.updateRoles(id, request);
    }
}
