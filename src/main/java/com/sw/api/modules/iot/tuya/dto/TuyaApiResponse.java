package com.sw.api.modules.iot.tuya.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record TuyaApiResponse<T>(boolean success, String msg, Integer code, T result, long t) {
}
