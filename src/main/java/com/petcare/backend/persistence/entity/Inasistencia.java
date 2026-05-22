package com.petcare.backend.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "inasistencias")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Inasistencia {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@OneToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "cita_id", nullable = false, unique = true)
	private Cita cita;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "duenio_id", nullable = false)
	private Duenio duenio;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "mascota_id", nullable = false)
	private Mascota mascota;

	@Column(nullable = false, length = 500)
	private String observacion;

	@Column(nullable = false, length = 120)
	private String registradoPor;

	@Column(nullable = false, updatable = false)
	private LocalDateTime fechaRegistro;
}
