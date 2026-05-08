package com.sw.api.modules.chat.dto;

import com.sw.api.modules.chat.model.EstadoMensaje;
import java.time.LocalDateTime;
import java.util.UUID;

public record MensajeDTO(
    UUID id,
    UUID conversacionId,
    UUID remitenteId,
    String contenido,
    EstadoMensaje estado,
    LocalDateTime creadoEn
) {}
