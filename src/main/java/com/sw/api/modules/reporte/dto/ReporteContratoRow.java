package com.sw.api.modules.reporte.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

public record ReporteContratoRow(
        UUID id,
        String tipoContrato,
        String estadoContrato,
        BigDecimal montoAcordado,
        String moneda,
        LocalDate fechaInicio,
        LocalDate fechaFin,
        Integer noches,
        Integer cantidadHuespedes,
        LocalDateTime fechaCreacion,
        String correosPropietario,
        String nombrePropietario,
        String correoCliente,
        String nombreCliente,
        String tituloPublicacion,
        String tipoInmueble,
        String ciudadInmueble
) {}
