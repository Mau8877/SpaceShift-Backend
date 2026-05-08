package com.sw.api.modules.publicacion.dto;

import java.util.UUID;

public record ImagenPublicacionDTO(
    UUID id,
    String urlImage,
    Boolean esPortada
) {}
