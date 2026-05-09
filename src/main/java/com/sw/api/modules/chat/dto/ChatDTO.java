package com.sw.api.modules.chat.dto;

import java.time.LocalDateTime;
import java.util.UUID;

public record ChatDTO(
    UUID conversacionId,
    String tituloPropiedad,
    UUID otroUsuarioId,
    String nombreOtroUsuario,
    String fotoOtroUsuario,
    LocalDateTime ultimoMensajeFecha
) {}
