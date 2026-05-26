package com.petcare.backend.domain.repository;

import com.petcare.backend.persistence.entity.ControlMensualMascota;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ControlMensualMascotaRepository extends JpaRepository<ControlMensualMascota, Long> {

	boolean existsByMascotaIdAndAnioAndMes(Long mascotaId, Integer anio, Integer mes);

	Optional<ControlMensualMascota> findByMascotaIdAndAnioAndMes(Long mascotaId, Integer anio, Integer mes);

	List<ControlMensualMascota> findByMascotaIdOrderByFechaControlDesc(Long mascotaId);
}
