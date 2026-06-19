package com.petcare.backend.domain.repository;

import com.petcare.backend.persistence.entity.NotaSeguimiento;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface NotaSeguimientoRepository extends JpaRepository<NotaSeguimiento, Long> {
    List<NotaSeguimiento> findByCitaIdOrderByCreatedAtDesc(Long citaId);
}
