package com.sw.api.modules.iot.dto;

import com.sw.api.modules.iot.model.PlugStatus;

import java.time.LocalDateTime;
import java.util.UUID;

public record SmartPlugDTO(
        UUID id,
        String tuyaDeviceId,
        String alias,
        PlugStatus status,
        String notes,
        CurrentAssignmentDTO currentAssignment) {

    public record CurrentAssignmentDTO(
            UUID applianceId,
            String applianceName,
            String propertyName,
            LocalDateTime assignedAt) {
    }
}
