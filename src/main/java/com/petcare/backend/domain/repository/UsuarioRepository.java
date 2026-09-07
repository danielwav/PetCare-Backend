package com.petcare.backend.domain.repository;

import com.petcare.backend.persistence.entity.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UsuarioRepository extends JpaRepository<Usuario, Long> {

	Optional<Usuario> findByEmail(String email);

	boolean existsByEmail(String email);

	Optional<Usuario> findByActivationToken(String activationToken);

	List<Usuario> findAllByClinicaId(Long clinicaId);
}
