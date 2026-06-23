package com.sw.api.modules.asistente.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

/** Subconjunto de la respuesta de OpenRouter que nos interesa (choices[0].message.content). */
@JsonIgnoreProperties(ignoreUnknown = true)
public record OpenRouterResponse(List<Choice> choices) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Choice(Message message) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Message(String role, String content) {
    }
}
