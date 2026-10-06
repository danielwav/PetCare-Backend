package com.petcare.backend.domain.repository;

import com.petcare.backend.persistence.entity.Vacuna;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface VacunaRepository extends JpaRepository<Vacuna, Long> {

	Optional<Vacuna> findByNombreIgnoreCaseAndClinicaId(String nombre, Long clinicaId);

	@Query(value = "select v.* from vacunas v where v.clinica_id = :clinicaId and (:active is null or v.active = :active) and (:search is null or upper(v.nombre) like upper('%' || :search || '%') or upper(v.descripcion) like upper('%' || :search || '%')) order by v.nombre asc", nativeQuery = true)
	List<Vacuna> search(@Param("clinicaId") Long clinicaId, @Param("search") String search, @Param("active") Boolean active);
}
