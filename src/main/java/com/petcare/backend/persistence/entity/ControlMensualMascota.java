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

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "controles_mensuales_mascota")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ControlMensualMascota {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "mascota_id", nullable = false)
	private Mascota mascota;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "veterinario_id", nullable = false)
	private Veterinario veterinario;

	@Column(nullable = false)
	private LocalDate fechaControl;

	@Column(nullable = false)
	private Integer anio;

	@Column(nullable = false)
	private Integer mes;

	@Column(precision = 6, scale = 2)
	private BigDecimal pesoKg;

	@Column(length = 500)
	private String alimentacion;

	@Column(length = 500)
	private String observaciones;

	@Column(length = 500)
	private String recomendaciones;

	@Column(nullable = false, updatable = false)
	private LocalDateTime createdAt;

	@Column(nullable = false)
	private LocalDateTime updatedAt;
}
