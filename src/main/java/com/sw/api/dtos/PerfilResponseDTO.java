package com.sw.api.dtos;

public record PerfilResponseDTO(
        String correo,
        boolean estadoConexion,
        String tipoPerfil,
        String nombre,
        String apellido,
        String fotoUrl) {
}
