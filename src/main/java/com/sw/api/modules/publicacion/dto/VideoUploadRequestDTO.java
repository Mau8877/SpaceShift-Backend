package com.sw.api.modules.publicacion.dto;

public record VideoUploadRequestDTO(
    String keyS3,
    String nombreArchivo,
    Long tamanoBytes,
    Integer duracionSegundos
) {}
