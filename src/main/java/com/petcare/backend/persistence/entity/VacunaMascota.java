package com.petcare.backend.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "vacunas_mascota")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VacunaMascota {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "mascota_id", nullable = false)
	private Mascota mascota;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "vacuna_id", nullable = false)
	private Vacuna vacuna;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "veterinario_id", nullable = false)
	private Veterinario veterinario;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "cita_id")
	private Cita cita;

	@Column(nullable = false)
	private LocalDate fechaAplicacion;

	@Column(length = 80)
	private String lote;

	private LocalDate fechaProximaDosis;

	@Column(length = 500)
	private String observaciones;

	@Column(nullable = false, updatable = false)
	private LocalDateTime createdAt;
}
