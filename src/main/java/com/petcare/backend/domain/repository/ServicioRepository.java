package com.petcare.backend.domain.repository;

import com.petcare.backend.persistence.entity.Servicio;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ServicioRepository extends JpaRepository<Servicio, Long> {

	Optional<Servicio> findByNombreIgnoreCase(String nombre);

	@Query(value = "select s from Servicio s where (:active is null or s.active = :active) and (:search is null or upper(s.nombre) like upper(concat('%', :search, '%')) or upper(s.descripcion) like upper(concat('%', :search, '%'))) order by s.nombre asc")
	List<Servicio> search(@Param("search") String search, @Param("active") Boolean active);
}
