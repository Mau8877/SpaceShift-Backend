package com.sw.api.modules.iot.tuya.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record TuyaDevice(
        String id,
        String name,
        boolean online,
        String category,
        @JsonProperty("product_id") String productId,
        List<TuyaStatus> status) {
}
