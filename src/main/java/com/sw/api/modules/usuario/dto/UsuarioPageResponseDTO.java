package com.sw.api.modules.usuario.dto;

import java.util.List;

public record UsuarioPageResponseDTO(
        List<UsuarioListItemDTO> content,
        int page,
        int size,
        long totalElements,
        int totalPages,
        UsuarioStatsDTO stats) {
}
