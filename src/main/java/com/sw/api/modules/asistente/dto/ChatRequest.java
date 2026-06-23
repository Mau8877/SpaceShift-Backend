package com.sw.api.modules.asistente.dto;

/**
 * Petición de chat del asistente: el mensaje del usuario y, opcionalmente, la ruta
 * de la pantalla en la que está (para darle contexto al modelo).
 */
public record ChatRequest(String message, String pagina) {
}
