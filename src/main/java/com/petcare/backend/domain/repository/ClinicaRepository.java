package com.petcare.backend.domain.repository;

import com.petcare.backend.persistence.entity.Clinica;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ClinicaRepository extends JpaRepository<Clinica, Long> {

	Optional<Clinica> findBySlug(String slug);

	boolean existsBySlug(String slug);
}