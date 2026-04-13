package com.sw.api.models.Chat;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

import com.sw.api.models.Usuario;

@Getter
@Setter
@Entity
@Table(name = "mensaje")
public class Mensaje {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "conversacion_id", nullable = false)
    private Conversacion conversacion;

    @ManyToOne
    @JoinColumn(name = "remitente_id", nullable = false)
    private Usuario remitente;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String contenido;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado", nullable = false)
    private EstadoMensaje estado = EstadoMensaje.ENVIADO;

    @Column(name = "creado_en", nullable = false, updatable = false)
    private LocalDateTime creadoEn = LocalDateTime.now();

    @PrePersist
    protected void onCreate() {
        creadoEn = LocalDateTime.now();
    }
}
