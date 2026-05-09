package com.sw.api.modules.usuario.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record UsuarioRequestDTO(
        @NotBlank(message = "El correo es obligatorio")
        @Email(message = "El correo no es valido")
        String correo,

        @NotBlank(message = "La password es obligatoria")
        String password,

        @NotBlank(message = "El rol es obligatorio")
        String rol,

        @NotBlank(message = "El tipoPerfil es obligatorio")
        String tipoPerfil,

        @NotBlank(message = "El nombre es obligatorio")
        String nombre,

        String apellido,
        String telefono,
        String descripcion) {
}
