package com.sw.api.modules.iot.tuya.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record TuyaStatus(String code, Object value) {
}
