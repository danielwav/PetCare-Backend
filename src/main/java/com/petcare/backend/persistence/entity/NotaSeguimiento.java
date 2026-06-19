package com.petcare.backend.persistence.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "notas_seguimiento")
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class NotaSeguimiento {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long citaId;

    @Column(nullable = false, length = 500)
    private String observacion;

    @Column(nullable = false)
    private String registradoPor;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
