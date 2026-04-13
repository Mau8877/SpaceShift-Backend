package com.sw.api.models.Chat;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.util.UUID;

@Getter
@Setter
@EqualsAndHashCode
@Embeddable
public class ParticipanteConversacionId implements Serializable {

    @Column(name = "conversacion_id")
    private UUID conversacionId;

    @Column(name = "usuario_id")
    private UUID usuarioId;

    public ParticipanteConversacionId() {
    }

    public ParticipanteConversacionId(UUID conversacionId, UUID usuarioId) {
        this.conversacionId = conversacionId;
        this.usuarioId = usuarioId;
    }
}
