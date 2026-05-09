package com.sw.api.modules.auth.dto;

public record RegisterRequest(
    String correo,
    String password,
    String nombre,
    String apellido,
    String fotoUrl,
    String tipoPerfil
) {}
