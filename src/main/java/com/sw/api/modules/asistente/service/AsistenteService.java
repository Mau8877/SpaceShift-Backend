package com.sw.api.modules.asistente.service;

import com.sw.api.modules.asistente.config.AsistenteContext;
import com.sw.api.modules.asistente.dto.ChatResponse;
import com.sw.api.modules.asistente.dto.OpenRouterResponse;
import com.sw.api.modules.asistente.dto.SpeechTokenResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

/**
 * Hace de proxy entre el frontend y los servicios externos del asistente
 * (OpenRouter para el LLM y Azure Speech para la voz), de modo que las API keys
 * vivan solo en el servidor y nunca lleguen al bundle del navegador.
 */
@Service
public class AsistenteService {

    private final RestTemplate restTemplate;

    @Value("${openrouter.api.key}")
    private String openRouterKey;

    @Value("${openrouter.model}")
    private String openRouterModel;

    @Value("${azure.speech.key}")
    private String azureKey;

    @Value("${azure.speech.region}")
    private String azureRegion;

    public AsistenteService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    /** Reenvía la pregunta del usuario a OpenRouter con el system prompt y la key del servidor. */
    public ChatResponse chat(String message, String pagina) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(openRouterKey);

        // System prompt = conocimiento base + (si aplica) guía de la pantalla actual.
        String systemPrompt = AsistenteContext.BASE_SYSTEM_PROMPT;
        String guiaPagina = AsistenteContext.resolver(pagina);
        if (guiaPagina != null) {
            systemPrompt += "\n\nContexto de la pantalla actual: " + guiaPagina;
        }

        Map<String, Object> body = Map.of(
                "model", openRouterModel,
                "messages", List.of(
                        Map.of("role", "system", "content", systemPrompt),
                        Map.of("role", "user", "content", message)),
                "max_tokens", 300,
                "temperature", 0.7);

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);

        ResponseEntity<OpenRouterResponse> response = restTemplate.exchange(
                "https://openrouter.ai/api/v1/chat/completions",
                HttpMethod.POST,
                request,
                OpenRouterResponse.class);

        OpenRouterResponse result = response.getBody();
        if (result == null || result.choices() == null || result.choices().isEmpty()) {
            throw new IllegalStateException("Respuesta inválida de OpenRouter");
        }
        return new ChatResponse(result.choices().get(0).message().content());
    }

    /**
     * Pide a Azure un token efímero (~10 min) usando la subscription key del servidor.
     * El frontend lo usa con SpeechConfig.fromAuthorizationToken, así la key nunca sale del backend.
     */
    public SpeechTokenResponse getSpeechToken() {
        String url = "https://" + azureRegion + ".api.cognitive.microsoft.com/sts/v1.0/issueToken";

        HttpHeaders headers = new HttpHeaders();
        headers.set("Ocp-Apim-Subscription-Key", azureKey);

        HttpEntity<Void> request = new HttpEntity<>(headers);

        // Azure devuelve el token como texto plano (no JSON).
        ResponseEntity<String> response = restTemplate.exchange(
                url,
                HttpMethod.POST,
                request,
                String.class);

        return new SpeechTokenResponse(response.getBody(), azureRegion);
    }
}
