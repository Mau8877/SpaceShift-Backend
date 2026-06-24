package com.sw.api.modules.iot.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "tuya.api")
public class TuyaProperties {

    private String baseUrl;
    private String clientId;
    private String clientSecret;
    private String uid;
}
