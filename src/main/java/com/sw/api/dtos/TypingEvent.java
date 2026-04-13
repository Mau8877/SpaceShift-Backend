package com.sw.api.dtos;

import java.util.UUID;

public record TypingEvent(
    String tipo,
    UUID conversacionId,
    UUID usuarioId,
    boolean escribiendo
) {}
