package com.sw.api.modules.chat.controller;

import com.sw.api.modules.chat.dto.MensajeDTO;
import com.sw.api.modules.chat.dto.SendMessageRequest;
import com.sw.api.modules.chat.dto.TypingEvent;
import com.sw.api.modules.chat.dto.TypingRequest;
import com.sw.api.modules.chat.model.Conversacion;
import com.sw.api.modules.chat.model.EstadoMensaje;
import com.sw.api.modules.chat.model.Mensaje;
import com.sw.api.modules.chat.model.ParticipanteConversacion;
import com.sw.api.modules.chat.repository.ConversacionRepository;
import com.sw.api.modules.chat.repository.MensajeRepository;
import com.sw.api.modules.chat.repository.ParticipanteConversacionRepository;
import com.sw.api.modules.notificacion.service.NotificacionService;
import com.sw.api.modules.usuario.model.Usuario;
import com.sw.api.modules.usuario.repository.PerfilRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@Controller
@RequiredArgsConstructor
public class ChatWebSocketController {

    private final SimpMessagingTemplate messagingTemplate;
    private final MensajeRepository mensajeRepository;
    private final ConversacionRepository conversacionRepository;
    private final ParticipanteConversacionRepository participanteConversacionRepository;
    private final NotificacionService notificacionService;
    private final PerfilRepository perfilRepository;

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

        conversacion.setActualizadoEn(java.time.LocalDateTime.now());
        conversacionRepository.save(conversacion);

        MensajeDTO mensajeDTO = new MensajeDTO(
                savedMensaje.getId(),
                conversacion.getId(),
                remitente.getId(),
                savedMensaje.getContenido(),
                savedMensaje.getEstado(),
                savedMensaje.getCreadoEn()
        );

        messagingTemplate.convertAndSend("/topic/chat." + conversacion.getId(), mensajeDTO);

        // Also deliver to each participant's private queue so the inbox
        // updates in real time even when they are not inside this conversation
        List<ParticipanteConversacion> todosParticipantes = participanteConversacionRepository
                .findAllByConversacion(conversacion);
        for (ParticipanteConversacion participante : todosParticipantes) {
            messagingTemplate.convertAndSendToUser(
                    participante.getUsuario().getCorreo(),
                    "/queue/messages",
                    mensajeDTO
            );
        }

        enviarPushADestinatariosOffline(conversacion.getId(), remitente, request.getContenido());
    }

    private void enviarPushADestinatariosOffline(java.util.UUID conversacionId, Usuario remitente, String contenido) {
        List<ParticipanteConversacion> otros = participanteConversacionRepository
                .findOtrosParticipantes(conversacionId, remitente.getId());

        String nombreRemitente = perfilRepository.findByUsuario(remitente)
                .map(p -> p.getNombre())
                .orElse("Alguien");

        String cuerpo = contenido.length() > 60 ? contenido.substring(0, 60) + "..." : contenido;

        for (ParticipanteConversacion participante : otros) {
            Usuario destinatario = participante.getUsuario();
            if (!destinatario.isEstadoConexion()) {
                notificacionService.enviarNotificacion(
                        destinatario.getId(),
                        "Nuevo mensaje de " + nombreRemitente,
                        cuerpo,
                        Map.of(
                                "type", "NEW_MESSAGE",
                                "conversacionId", conversacionId.toString()
                        )
                );
            }
        }
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
