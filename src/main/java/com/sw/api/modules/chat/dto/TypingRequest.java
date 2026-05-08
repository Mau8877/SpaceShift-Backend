package com.sw.api.modules.chat.dto;

import java.util.UUID;

public record TypingRequest(
    UUID conversacionId,
    boolean escribiendo
) {}
