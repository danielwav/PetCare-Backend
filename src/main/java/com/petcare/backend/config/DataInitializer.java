package com.petcare.backend.config;

import com.petcare.backend.domain.repository.RolRepository;
import com.petcare.backend.persistence.entity.Rol;
import com.petcare.backend.persistence.enums.RoleName;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

	private final RolRepository rolRepository;

	@Override
	public void run(String... args) {
		createRoleIfMissing(RoleName.ROLE_ADMIN, "Administrador general del sistema.");
		createRoleIfMissing(RoleName.ROLE_VETERINARIO, "Personal medico veterinario.");
		createRoleIfMissing(RoleName.ROLE_ASISTENTE, "Personal operativo de recepcion y agenda.");
		createRoleIfMissing(RoleName.ROLE_DUENIO, "Cliente o propietario de mascota.");
	}

	private void createRoleIfMissing(RoleName name, String description) {
		if (rolRepository.existsByName(name)) {
			return;
		}

		rolRepository.save(Rol.builder()
				.name(name)
				.description(description)
				.active(true)
				.build());
	}
}
