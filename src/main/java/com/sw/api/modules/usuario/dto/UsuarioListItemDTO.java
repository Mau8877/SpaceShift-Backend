package com.sw.api.modules.usuario.dto;

import java.util.UUID;

public record UsuarioListItemDTO(
        UUID id,
        String correo,
        String nombre,
        String apellido,
        String telefono,
        boolean estado,
        boolean estadoConexion,
        String rol,
        String tipoPerfil,
        long totalPublicaciones) {
}
