package com.sw.api.modules.asistente.dto;

/** Token efímero de Azure Speech + región, para que el SDK del navegador lo use. */
public record SpeechTokenResponse(String token, String region) {
}
