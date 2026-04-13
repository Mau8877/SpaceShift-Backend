package com.sw.api.models.Chat;

import com.sw.api.models.Usuario;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "participante_conversacion")
public class ParticipanteConversacion {

    @EmbeddedId
    private ParticipanteConversacionId id;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("conversacionId")
    @JoinColumn(name = "conversacion_id")
    private Conversacion conversacion;

    @ManyToOne
    @MapsId("usuarioId")
    @JoinColumn(name = "usuario_id")
    private Usuario usuario;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ultimo_mensaje_leido_id")
    private Mensaje ultimoMensajeLeido;

    @Enumerated(EnumType.STRING)
    @Column(name = "rol", nullable = false)
    private RolParticipante rol;
}
