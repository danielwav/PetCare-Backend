package com.petcare.backend.domain.repository;

import com.petcare.backend.persistence.entity.AtencionClinica;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface AtencionClinicaRepository extends JpaRepository<AtencionClinica, Long> {

	boolean existsByCitaId(Long citaId);

	Optional<AtencionClinica> findByCitaId(Long citaId);

	List<AtencionClinica> findByMascotaIdOrderByFechaRegistroDesc(Long mascotaId);
}
