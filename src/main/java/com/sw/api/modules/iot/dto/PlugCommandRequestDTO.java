package com.sw.api.modules.iot.dto;

public record PlugCommandRequestDTO(String action) {
    public boolean isOn() {
        return "ON".equalsIgnoreCase(action);
    }
}
