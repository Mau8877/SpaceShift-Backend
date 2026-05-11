package com.sw.api.modules.reporte.dto;

import java.math.BigDecimal;

public record ReporteResumenDTO(
        long totalUsuarios,
        long usuariosActivos,
        long usuariosInactivos,
        long usuariosRoleUser,
        long usuariosRoleAdmin,
        long totalPublicaciones,
        long publicacionesDisponibles,
        long publicacionesEnVenta,
        long publicacionesEnAlquiler,
        long totalContratos,
        long contratosActivos,
        long contratosCerrados,
        long contratosCancelados,
        long contratosCompraventa,
        long contratosAlquiler,
        long contratosHospedaje,
        BigDecimal montoTotalContratos,
        BigDecimal montoContratosActivos
) {}
