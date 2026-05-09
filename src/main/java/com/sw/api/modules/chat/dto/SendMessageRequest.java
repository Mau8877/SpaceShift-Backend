package com.sw.api.modules.chat.dto;

import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SendMessageRequest {
    private UUID conversacionId;
    private String contenido;
}
