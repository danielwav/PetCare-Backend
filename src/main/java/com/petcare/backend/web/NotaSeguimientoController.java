package com.petcare.backend.web;

import com.petcare.backend.domain.dto.request.NotaSeguimientoRequest;
import com.petcare.backend.domain.dto.response.NotaSeguimientoResponse;
import com.petcare.backend.domain.service.NotaSeguimientoService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class NotaSeguimientoController {

    private final NotaSeguimientoService service;

    @PostMapping("/api/notas-seguimiento")
    @ResponseStatus(HttpStatus.CREATED)
    public NotaSeguimientoResponse create(@Valid @RequestBody NotaSeguimientoRequest request, Authentication authentication) {
        String usuario = authentication != null ? authentication.getName() : "sistema";
        return service.create(request, usuario);
    }

    @GetMapping("/api/notas-seguimiento/{citaId}")
    public List<NotaSeguimientoResponse> findByCitaId(@PathVariable Long citaId) {
        return service.findByCitaId(citaId);
    }
}
