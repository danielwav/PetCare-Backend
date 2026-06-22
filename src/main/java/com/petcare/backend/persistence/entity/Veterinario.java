package com.petcare.backend.persistence.entity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "veterinarios")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Veterinario {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@OneToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "usuario_id", unique = true)
	private Usuario usuario;

	@Column(length = 80)
	private String nombres;

	@Column(length = 80)
	private String apellidos;

	@Column(nullable = false, unique = true, length = 30)
	private String numeroColegiatura;

	@Column(nullable = false, length = 100)
	private String especialidad;

	@Column(length = 20)
	private String telefono;

	@Column(unique = true, length = 120)
	private String email;

	@Column(nullable = false)
	private Boolean active;

	@Column(nullable = false, updatable = false)
	private LocalDateTime createdAt;

	@Column(nullable = false)
	private LocalDateTime updatedAt;

	@Builder.Default
	@OneToMany(mappedBy = "veterinario", cascade = CascadeType.ALL, orphanRemoval = true)
	private List<HorarioVeterinario> horarios = new ArrayList<>();
}
