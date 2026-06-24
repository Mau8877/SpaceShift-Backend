package com.sw.api.modules.iot.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class TuyaWebClientConfig {

    @Bean
    public WebClient tuyaWebClient(TuyaProperties properties) {
        return WebClient.builder()
                .baseUrl(properties.getBaseUrl())
                .build();
    }
}
