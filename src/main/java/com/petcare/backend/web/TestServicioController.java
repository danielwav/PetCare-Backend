package com.petcare.backend.web;

import com.petcare.backend.domain.service.ServicioService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class TestServicioController {

    private final ServicioService servicioService;

    @GetMapping("/api/test-servicio")
    public String test() {
        try {
            var count = servicioService.findAll(null, null).size();
            return "ServicioService works. Count: " + count;
        } catch (Exception e) {
            return "Error: " + e.getClass().getName() + ": " + e.getMessage();
        }
    }
}
