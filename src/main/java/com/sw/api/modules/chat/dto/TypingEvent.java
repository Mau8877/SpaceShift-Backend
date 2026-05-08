package com.sw.api.modules.chat.dto;

import java.util.UUID;

public record TypingEvent(
    String tipo,
    UUID conversacionId,
    UUID usuarioId,
    boolean escribiendo
) {}
