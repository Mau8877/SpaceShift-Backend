package com.sw.api.modules.video_processing.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RunpodRequest {
    private RunpodInput input;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RunpodInput {
        private String video_url;
    }
}
