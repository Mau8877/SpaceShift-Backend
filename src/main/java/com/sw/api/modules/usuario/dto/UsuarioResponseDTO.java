package com.sw.api.modules.usuario.dto;

import java.time.LocalDateTime;
import java.util.UUID;

public record UsuarioResponseDTO(
        UUID id,
        String correo,
        boolean estado,
        boolean estadoConexion,
        LocalDateTime ultimaConexion,
        String rol,
        LocalDateTime createdDate,
        LocalDateTime lastModifiedDate,
        String nombre,
        String apellido,
        String fotoUrl,
        String telefono,
        String descripcion,
        String tipoPerfil,
        long totalPublicaciones) {
}
