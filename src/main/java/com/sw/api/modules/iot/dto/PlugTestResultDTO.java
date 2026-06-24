package com.sw.api.modules.iot.dto;

public record PlugTestResultDTO(boolean online, boolean testPassed, String message) {
}
