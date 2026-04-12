package com.sw.api.controllers;

import com.sw.api.dtos.ChatDTO;
import com.sw.api.dtos.MensajeDTO;
import com.sw.api.models.Usuario;
import com.sw.api.services.ChatService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import com.sw.api.dtos.CrearChatRequest;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/chats")
@RequiredArgsConstructor
public class ChatController {

    private final ChatService chatService;

    @GetMapping
    public ResponseEntity<List<ChatDTO>> obtenerChats(@AuthenticationPrincipal Usuario usuario) {
        if (usuario == null) {
            return ResponseEntity.status(401).build();
        }
        return ResponseEntity.ok(chatService.obtenerChatsUsuario(usuario));
    }

    @GetMapping("/{id}/messages")
    public ResponseEntity<Page<MensajeDTO>> obtenerMensajesChat(
            @PathVariable UUID id,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return ResponseEntity.ok(chatService.obtenerHistorialPaginado(id, page, size));
    }

    @PostMapping
    public ResponseEntity<ChatDTO> iniciarChat(@RequestBody CrearChatRequest request, @AuthenticationPrincipal Usuario usuario) {
        if (usuario == null) {
            return ResponseEntity.status(401).build();
        }
        return ResponseEntity.ok(chatService.crearChat(request.getPublicacionId(), usuario));
    }
}
