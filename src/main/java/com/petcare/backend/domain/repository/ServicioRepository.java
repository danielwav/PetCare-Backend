package com.petcare.backend.domain.repository;

import com.petcare.backend.persistence.entity.Servicio;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ServicioRepository extends JpaRepository<Servicio, Long> {

	Optional<Servicio> findByNombreIgnoreCase(String nombre);

	List<Servicio> findByActiveTrueOrderByNombreAsc();
}
