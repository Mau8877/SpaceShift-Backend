package com.sw.api.modules.video_processing.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UploadUrlResponse {
    private String uploadUrl;
    private String key;
}
