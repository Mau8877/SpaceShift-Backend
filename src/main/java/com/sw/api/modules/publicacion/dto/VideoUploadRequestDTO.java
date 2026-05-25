package com.sw.api.modules.publicacion.dto;

public record VideoUploadRequestDTO(
    String urlVideo,
    String nombreArchivo,
    Long tamanoBytes,
    Integer duracionSegundos
) {}
