package com.sw.api.modules.usuario.dto;

public record PerfilResponseDTO(
        String correo,
        boolean estadoConexion,
        String tipoPerfil,
        String nombre,
        String apellido,
        String fotoUrl) {
}
