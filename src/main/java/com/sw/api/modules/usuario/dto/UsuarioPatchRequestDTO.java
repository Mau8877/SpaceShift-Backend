package com.sw.api.modules.usuario.dto;

import jakarta.validation.constraints.Email;

public record UsuarioPatchRequestDTO(
        @Email(message = "El correo no es valido")
        String correo,
        String nombre,
        String apellido,
        String telefono,
        String descripcion,
        String tipoPerfil) {
}
