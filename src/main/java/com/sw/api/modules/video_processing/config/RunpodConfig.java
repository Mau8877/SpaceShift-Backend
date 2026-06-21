package com.sw.api.modules.video_processing.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

@Configuration
public class RunpodConfig {

    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }
}
