package com.petcare.backend.persistence.entity;

import com.petcare.backend.persistence.enums.SexoMascota;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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
@Table(name = "mascotas")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Mascota {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "duenio_id", nullable = false)
	private Duenio duenio;

	@Column(nullable = false, length = 80)
	private String nombre;

	@Column(nullable = false, length = 60)
	private String especie;

	@Column(nullable = false, length = 80)
	private String raza;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private SexoMascota sexo;

	@Column(nullable = false)
	private LocalDate fechaNacimiento;

	@Column(length = 50)
	private String color;

	@Column(precision = 6, scale = 2)
	private BigDecimal pesoKg;

	@Column(length = 300)
	private String observaciones;

	@Column(nullable = false)
	private Boolean active;

	@Column(nullable = false, updatable = false)
	private LocalDateTime createdAt;

	@Column(nullable = false)
	private LocalDateTime updatedAt;
}
