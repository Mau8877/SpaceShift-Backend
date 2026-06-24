package com.sw.api.modules.iot.dto;

public record TuyaDeviceScanResultDTO(
        String tuyaDeviceId,
        String name,
        boolean online,
        boolean alreadyRegistered) {
}
