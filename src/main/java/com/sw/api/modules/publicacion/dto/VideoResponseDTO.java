package com.sw.api.modules.publicacion.dto;

import com.sw.api.modules.publicacion.model.EstadoProcesamiento;
import java.time.LocalDateTime;
import java.util.UUID;

public record VideoResponseDTO(
    UUID id,
    UUID idPublicacion,
    String urlVideo,
    String urlModelo3D,
    String urlSplat,
    String urlJsonModelo,
    String urlPreviewWebp,
    Integer duracionSegundos,
    Integer creditosConsumidos,
    EstadoProcesamiento estadoProcesamiento,
    String nombreArchivo,
    Long tamanoBytes,
    String errorMensaje,
    LocalDateTime fechaCreacion
) {}
