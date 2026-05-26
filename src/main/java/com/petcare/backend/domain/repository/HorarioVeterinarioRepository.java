package com.petcare.backend.domain.repository;

import com.petcare.backend.persistence.entity.HorarioVeterinario;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.DayOfWeek;
import java.util.List;

public interface HorarioVeterinarioRepository extends JpaRepository<HorarioVeterinario, Long> {

	List<HorarioVeterinario> findByVeterinarioIdAndDiaSemanaAndActiveTrueOrderByHoraInicioAsc(
			Long veterinarioId,
			DayOfWeek diaSemana
	);
}
