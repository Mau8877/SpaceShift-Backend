package com.sw.api.controllers;

import com.sw.api.dtos.MensajeDTO;
import com.sw.api.dtos.SendMessageRequest;
import com.sw.api.dtos.TypingEvent;
import com.sw.api.dtos.TypingRequest;
import com.sw.api.models.Chat.Conversacion;
import com.sw.api.models.Chat.EstadoMensaje;
import com.sw.api.models.Chat.Mensaje;
import com.sw.api.models.Chat.ParticipanteConversacion;
import com.sw.api.models.Usuario;
import com.sw.api.repositories.ConversacionRepository;
import com.sw.api.repositories.MensajeRepository;
import com.sw.api.repositories.ParticipanteConversacionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;

import java.util.Optional;
import java.util.UUID;

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
            return; // No autorizado
        }
        
        Usuario remitente = (Usuario) auth.getPrincipal();

        Optional<Conversacion> conversacionOpt = conversacionRepository.findById(request.getConversacionId());
        if (conversacionOpt.isEmpty()) {
            return;
        }

        Conversacion conversacion = conversacionOpt.get();

        // Crear y guardar el mensaje
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

        // Retransmitir al otro participante (o a todos en el canal)
        // Por simplicidad en esta fase, podemos enviar a un tópico de la conversacion 
        // o si queremos usar convertAndSendToUser, iterar sobre la conversacion
        
        // Retransmitir al canal general del chat para que todos los usuarios 
        // subscritos a esa conversacion lo reciban.
        messagingTemplate.convertAndSend("/topic/chat." + conversacion.getId(), mensajeDTO);
        
        // Si quisieramos enviarlo a la cola privada del User:
        // messagingTemplate.convertAndSendToUser(otroUsuarioId.toString(), "/queue/messages", mensajeDTO);
    }

    /**
     * Escucha eventos de "está escribiendo" enviados por el frontend a /app/chat.typing
     * y los retransmite al topic /topic/chat.typing.{conversacionId} para que
     * el otro usuario lo reciba en tiempo real.
     */
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

        // Retransmitir al topic de typing de la conversación
        messagingTemplate.convertAndSend(
                "/topic/chat.typing." + request.conversacionId(),
                event
        );
    }
}
