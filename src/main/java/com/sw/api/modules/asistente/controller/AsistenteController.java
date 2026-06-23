package com.sw.api.modules.asistente.controller;

import com.sw.api.modules.asistente.dto.ChatRequest;
import com.sw.api.modules.asistente.dto.ChatResponse;
import com.sw.api.modules.asistente.dto.SpeechTokenResponse;
import com.sw.api.modules.asistente.service.AsistenteService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/asistente")
@RequiredArgsConstructor
public class AsistenteController {

    private final AsistenteService asistenteService;

    /** Proxy al LLM: recibe el mensaje del usuario y devuelve la respuesta. */
    @PostMapping("/chat")
    public ResponseEntity<ChatResponse> chat(@RequestBody ChatRequest request) {
        return ResponseEntity.ok(asistenteService.chat(request.message(), request.pagina()));
    }

    /** Token efímero de Azure Speech para el lip-sync en el navegador. */
    @GetMapping("/speech-token")
    public ResponseEntity<SpeechTokenResponse> speechToken() {
        return ResponseEntity.ok(asistenteService.getSpeechToken());
    }
}
