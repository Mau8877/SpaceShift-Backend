package com.sw.api.dtos;

public record PerfilPatchRequestDTO(
        String correo,
        Boolean estadoConexion,
        String tipoPerfil,
        String nombre,
        String apellido,
        String fotoUrl) {
}
