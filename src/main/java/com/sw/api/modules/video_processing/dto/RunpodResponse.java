package com.sw.api.modules.video_processing.dto;

import lombok.Data;

@Data
public class RunpodResponse {
    private String id;
    private String status;
    private Output output;
    private String error;

    @Data
    public static class Output {
        private Assets assets;
        private String job_id;
        private String status;
    }

    @Data
    public static class Assets {
        private String metadata;
        private String model;
        private String preview;
    }
}
