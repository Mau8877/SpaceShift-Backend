package com.sw.api.modules.chat.dto;

import lombok.Getter;
import lombok.Setter;
import java.util.UUID;

@Getter
@Setter
public class CrearChatRequest {
    private UUID publicacionId;
}
