package com.petcare.backend.domain.repository;

import com.petcare.backend.persistence.entity.Veterinario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface VeterinarioRepository extends JpaRepository<Veterinario, Long> {

	Optional<Veterinario> findByEmail(String email);

	Optional<Veterinario> findByNumeroColegiatura(String numeroColegiatura);

	Optional<Veterinario> findByUsuarioId(Long usuarioId);

	@Query(value = "select distinct v.* from veterinarios v left join horarios_veterinarios h on h.veterinario_id = v.id where (:active is null or v.active = :active) and (:search is null or upper(v.nombres) like upper('%' || :search || '%') or upper(v.apellidos) like upper('%' || :search || '%') or upper(v.email) like upper('%' || :search || '%') or upper(v.especialidad) like upper('%' || :search || '%') or v.numero_colegiatura like ('%' || :search || '%')) order by v.apellidos asc, v.nombres asc", nativeQuery = true)
	List<Veterinario> search(@Param("search") String search, @Param("active") Boolean active);
}
