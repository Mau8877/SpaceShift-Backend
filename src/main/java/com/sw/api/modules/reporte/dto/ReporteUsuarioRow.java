package com.sw.api.modules.reporte.dto;

import java.time.LocalDateTime;
import java.util.UUID;

public record ReporteUsuarioRow(
        UUID id,
        String correo,
        String nombre,
        String apellido,
        String telefono,
        String rol,
        String tipoPerfil,
        boolean activo,
        boolean enLinea,
        LocalDateTime fechaRegistro,
        LocalDateTime ultimaConexion,
        long totalPublicaciones
) {}
