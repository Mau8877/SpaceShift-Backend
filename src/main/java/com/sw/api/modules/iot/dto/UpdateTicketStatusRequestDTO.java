package com.sw.api.modules.iot.dto;

import com.sw.api.modules.iot.model.InstallationTicketStatus;

import java.time.LocalDateTime;

public record UpdateTicketStatusRequestDTO(InstallationTicketStatus status, LocalDateTime scheduledAt) {
}
