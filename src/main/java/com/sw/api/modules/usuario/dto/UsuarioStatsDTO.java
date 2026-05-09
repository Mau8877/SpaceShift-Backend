package com.sw.api.modules.usuario.dto;

public record UsuarioStatsDTO(
        long totalUsuarios,
        long usuariosActivos,
        long usuariosInactivos,
        long totalPublicaciones) {
}
