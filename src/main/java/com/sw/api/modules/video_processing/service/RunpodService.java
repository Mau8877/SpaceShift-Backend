package com.sw.api.modules.video_processing.service;

import com.sw.api.modules.video_processing.dto.RunpodRequest;
import com.sw.api.modules.video_processing.dto.RunpodResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
public class RunpodService {

    private final RestTemplate restTemplate;

    @Value("${runpod.api.key}")
    private String apiKey;

    // Endpoint de Runpod para modelos .splat (mejor calidad, más pesado).
    @Value("${runpod.endpoint.splat}")
    private String splatEndpointId;

    // Endpoint de Runpod para modelos .sog (más ligero).
    @Value("${runpod.endpoint.sog}")
    private String sogEndpointId;

    public RunpodService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    /**
     * Resuelve el endpoint de Runpod según el formato. Si no se indica splat,
     * se usa el endpoint de .sog (también el valor por defecto para videos previos).
     */
    private String resolveEndpointId(boolean splat) {
        return splat ? splatEndpointId : sogEndpointId;
    }

    public RunpodResponse processVideo(String videoUrl, boolean splat) {
        String runpodUrl = "https://api.runpod.ai/v2/" + resolveEndpointId(splat) + "/run";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(apiKey);

        RunpodRequest requestPayload = new RunpodRequest(new RunpodRequest.RunpodInput(videoUrl));
        HttpEntity<RunpodRequest> request = new HttpEntity<>(requestPayload, headers);

        ResponseEntity<RunpodResponse> response = restTemplate.exchange(
                runpodUrl,
                HttpMethod.POST,
                request,
                RunpodResponse.class
        );

        return response.getBody();
    }

    public RunpodResponse checkStatus(String jobId, boolean splat) {
        String runpodUrl = "https://api.runpod.ai/v2/" + resolveEndpointId(splat) + "/status/" + jobId;

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(apiKey);

        HttpEntity<Void> request = new HttpEntity<>(headers);

        ResponseEntity<RunpodResponse> response = restTemplate.exchange(
                runpodUrl,
                HttpMethod.GET,
                request,
                RunpodResponse.class
        );

        return response.getBody();
    }
}
