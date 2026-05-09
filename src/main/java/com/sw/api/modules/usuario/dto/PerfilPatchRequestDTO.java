package com.sw.api.modules.usuario.dto;

public record PerfilPatchRequestDTO(
                String correo,
                Boolean estadoConexion,
                String tipoPerfil,
                String nombre,
                String apellido,
                String fotoUrl,
                String telefono,
                String descripcion) {
}
