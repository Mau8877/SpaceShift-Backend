package com.sw.api.dtos;

import lombok.Getter;
import lombok.Setter;
import java.util.UUID;

@Getter
@Setter
public class CrearChatRequest {
    private UUID publicacionId;
}
