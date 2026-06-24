package com.sw.api.modules.iot.dto;

import java.util.UUID;

public record AssignPlugRequestDTO(UUID inmuebleId, String dispositivoId) {
}
