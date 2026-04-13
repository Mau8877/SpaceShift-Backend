package com.sw.api.dtos;

import java.util.UUID;

public record TypingRequest(
    UUID conversacionId,
    boolean escribiendo
) {}
