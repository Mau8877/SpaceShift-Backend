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

    @Value("${runpod.endpoint.id}")
    private String endpointId;

    public RunpodService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    public RunpodResponse processVideo(String videoUrl) {
        String runpodUrl = "https://api.runpod.ai/v2/" + endpointId + "/run";

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

    public RunpodResponse checkStatus(String jobId) {
        String runpodUrl = "https://api.runpod.ai/v2/" + endpointId + "/status/" + jobId;

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
