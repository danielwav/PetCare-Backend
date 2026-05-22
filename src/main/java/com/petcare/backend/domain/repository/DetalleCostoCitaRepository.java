package com.petcare.backend.domain.repository;

import com.petcare.backend.persistence.entity.DetalleCostoCita;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DetalleCostoCitaRepository extends JpaRepository<DetalleCostoCita, Long> {

	List<DetalleCostoCita> findByCitaId(Long citaId);
}
