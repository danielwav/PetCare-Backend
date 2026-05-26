package com.petcare.backend.domain.repository;

import com.petcare.backend.persistence.entity.Rol;
import com.petcare.backend.persistence.enums.RoleName;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface RolRepository extends JpaRepository<Rol, Long> {

	Optional<Rol> findByName(RoleName name);

	boolean existsByName(RoleName name);
}
