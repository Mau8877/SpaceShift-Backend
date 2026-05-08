package com.sw.api.modules.chat.controller;

import com.sw.api.modules.chat.dto.MensajeDTO;
import com.sw.api.modules.chat.dto.SendMessageRequest;
import com.sw.api.modules.chat.dto.TypingEvent;
import com.sw.api.modules.chat.dto.TypingRequest;
import com.sw.api.modules.chat.model.Conversacion;
import com.sw.api.modules.chat.model.EstadoMensaje;
import com.sw.api.modules.chat.model.Mensaje;
import com.sw.api.modules.chat.repository.ConversacionRepository;
import com.sw.api.modules.chat.repository.MensajeRepository;
import com.sw.api.modules.chat.repository.ParticipanteConversacionRepository;
import com.sw.api.modules.usuario.model.Usuario;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;

import java.util.Optional;

@Controller
@RequiredArgsConstructor
public class ChatWebSocketController {

    private final SimpMessagingTemplate messagingTemplate;
    private final MensajeRepository mensajeRepository;
    private final ConversacionRepository conversacionRepository;
    private final ParticipanteConversacionRepository participanteConversacionRepository;

    @MessageMapping("/chat.send")
    public void sendMessage(@Payload SendMessageRequest request, SimpMessageHeaderAccessor headerAccessor) {
        Authentication auth = (Authentication) headerAccessor.getUser();
        if (auth == null || !(auth.getPrincipal() instanceof Usuario)) {
            return;
        }

        Usuario remitente = (Usuario) auth.getPrincipal();

        Optional<Conversacion> conversacionOpt = conversacionRepository.findById(request.getConversacionId());
        if (conversacionOpt.isEmpty()) {
            return;
        }

        Conversacion conversacion = conversacionOpt.get();

        Mensaje mensaje = new Mensaje();
        mensaje.setConversacion(conversacion);
        mensaje.setRemitente(remitente);
        mensaje.setContenido(request.getContenido());
        mensaje.setEstado(EstadoMensaje.ENVIADO);

        Mensaje savedMensaje = mensajeRepository.save(mensaje);

        MensajeDTO mensajeDTO = new MensajeDTO(
                savedMensaje.getId(),
                conversacion.getId(),
                remitente.getId(),
                savedMensaje.getContenido(),
                savedMensaje.getEstado(),
                savedMensaje.getCreadoEn()
        );

        messagingTemplate.convertAndSend("/topic/chat." + conversacion.getId(), mensajeDTO);
    }

    @MessageMapping("/chat.typing")
    public void handleTyping(@Payload TypingRequest request, SimpMessageHeaderAccessor headerAccessor) {
        Authentication auth = (Authentication) headerAccessor.getUser();
        if (auth == null || !(auth.getPrincipal() instanceof Usuario)) {
            return;
        }

        Usuario remitente = (Usuario) auth.getPrincipal();

        TypingEvent event = new TypingEvent(
                "TYPING",
                request.conversacionId(),
                remitente.getId(),
                request.escribiendo()
        );

        messagingTemplate.convertAndSend(
                "/topic/chat.typing." + request.conversacionId(),
                event
        );
    }
}
