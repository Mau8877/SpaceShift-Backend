package com.sw.api.modules.publicacion.dto;

import com.sw.api.modules.publicacion.model.Formato3D;

public record VideoUploadRequestDTO(
    String keyS3,
    String nombreArchivo,
    Long tamanoBytes,
    Integer duracionSegundos,
    Formato3D formato
) {}
