package com.sw.api.modules.iot.dto;

import com.sw.api.modules.iot.model.TipoIncumplimiento;

import java.time.LocalDateTime;
import java.util.UUID;

public record DeviceViolationDTO(UUID id, TipoIncumplimiento tipo, LocalDateTime detectedAt, String detalle) {
}
