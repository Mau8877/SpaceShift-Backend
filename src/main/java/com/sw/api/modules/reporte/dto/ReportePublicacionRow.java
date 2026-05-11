package com.sw.api.modules.reporte.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record ReportePublicacionRow(
        UUID id,
        String titulo,
        String tipoTransaccion,
        BigDecimal precio,
        String moneda,
        String estadoPublicacion,
        LocalDateTime fechaPublicacion,
        String tipoInmueble,
        BigDecimal areaTerreno,
        BigDecimal areaConstruida,
        Integer habitaciones,
        Integer banos,
        Integer garajes,
        String ciudad,
        String zonaBarrios,
        String direccionExacta,
        String correoPublicador,
        String nombrePublicador,
        String apellidoPublicador
) {}
