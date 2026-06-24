package com.sw.api.modules.iot.dto;

import java.time.LocalDateTime;

public record PlugPowerReadingDTO(LocalDateTime recordedAt, Integer curPower, boolean online) {
}
