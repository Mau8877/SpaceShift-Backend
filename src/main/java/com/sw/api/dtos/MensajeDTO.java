package com.sw.api.dtos;

import com.sw.api.models.Chat.EstadoMensaje;

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
