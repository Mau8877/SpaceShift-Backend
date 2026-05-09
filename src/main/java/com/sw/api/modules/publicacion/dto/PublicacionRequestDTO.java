package com.sw.api.modules.publicacion.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record PublicacionRequestDTO(
    UUID idUsuario,
    UUID idInmueble,
    String titulo,
    String descripcionGeneral,
    String tipoTransaccion,
    BigDecimal precio,
    String moneda,
    String estadoPublicacion,
    java.util.List<String> imagenesUrls
) {}
