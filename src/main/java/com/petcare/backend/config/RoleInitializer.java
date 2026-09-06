package com.petcare.backend.config;

import com.petcare.backend.domain.repository.RolRepository;
import com.petcare.backend.persistence.entity.Rol;
import com.petcare.backend.persistence.enums.RoleName;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class RoleInitializer implements CommandLineRunner {

    private final RolRepository rolRepository;

    @Override
    public void run(String... args) {
        createIfMissing(RoleName.ROLE_ADMIN, "Administrador general del sistema.");
        createIfMissing(RoleName.ROLE_VETERINARIO, "Personal medico veterinario.");
        createIfMissing(RoleName.ROLE_ASISTENTE, "Personal operativo de recepcion y agenda.");
        createIfMissing(RoleName.ROLE_DUENIO, "Cliente o propietario de mascota.");
    }

    private void createIfMissing(RoleName name, String description) {
        if (!rolRepository.existsByName(name)) {
            rolRepository.save(Rol.builder()
                    .name(name)
                    .description(description)
                    .active(true)
                    .build());
        }
    }
}
