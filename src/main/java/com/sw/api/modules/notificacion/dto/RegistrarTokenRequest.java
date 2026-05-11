package com.sw.api.modules.notificacion.dto;

import com.sw.api.modules.chat.model.PlataformaDispositivo;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record RegistrarTokenRequest(
        @NotBlank String tokenFcm,
        @NotNull PlataformaDispositivo plataforma
) {}
