package com.sw.api.modules.iot.dto;

import com.sw.api.modules.iot.model.InstallationTicketStatus;

import java.time.LocalDateTime;
import java.util.UUID;

public record InstallationTicketDTO(
        UUID id,
        UUID propertyId,
        String propertyName,
        String dispositivoId,
        String dispositivoNombre,
        UUID publicacionId,
        InstallationTicketStatus status,
        LocalDateTime requestedAt,
        LocalDateTime scheduledAt) {
}
