package com.petcare.backend.domain.repository;

import com.petcare.backend.persistence.entity.Servicio;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ServicioRepository extends JpaRepository<Servicio, Long> {

	Optional<Servicio> findByNombreIgnoreCase(String nombre);

	Optional<Servicio> findByClinicaIdAndNombreIgnoreCase(Long clinicaId, String nombre);

	List<Servicio> findAllByClinicaIdOrderByNombreAsc(Long clinicaId);

	@Query(value = "select * from servicios where (:clinicaId is null or clinica_id = :clinicaId) and (:active is null or active = :active) and (:search is null or upper(nombre) like upper('%' || :search || '%') or upper(descripcion) like upper('%' || :search || '%')) order by nombre asc", nativeQuery = true)
	List<Servicio> search(@Param("clinicaId") Long clinicaId, @Param("search") String search, @Param("active") Boolean active);
}
