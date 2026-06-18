package com.petcare.backend.persistence.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "horarios_semanales")
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class HorarioSemanal {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long usuarioId;

    @Column(nullable = false)
    private LocalDate fechaSemana;

    @Column(length = 20)
    private String lunes;

    @Column(length = 20)
    private String martes;

    @Column(length = 20)
    private String miercoles;

    @Column(length = 20)
    private String jueves;

    @Column(length = 20)
    private String viernes;

    @Column(length = 20)
    private String sabado;

    @Column(length = 20)
    private String domingo;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;
}
