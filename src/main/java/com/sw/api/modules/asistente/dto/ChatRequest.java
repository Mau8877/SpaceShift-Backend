package com.sw.api.modules.asistente.dto;

/** Petición de chat del asistente: solo el mensaje del usuario. */
public record ChatRequest(String message) {
}
