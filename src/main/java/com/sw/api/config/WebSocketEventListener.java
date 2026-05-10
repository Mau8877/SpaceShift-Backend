package com.sw.api.config;

import com.sw.api.modules.usuario.model.Usuario;
import com.sw.api.modules.usuario.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.simp.user.SimpUser;
import org.springframework.messaging.simp.user.SimpUserRegistry;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.socket.messaging.SessionConnectedEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

@Component
@RequiredArgsConstructor
public class WebSocketEventListener {

    private final UsuarioRepository usuarioRepository;
    private final SimpUserRegistry simpUserRegistry;

    @EventListener
    @Transactional
    public void handleConnect(SessionConnectedEvent event) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(event.getMessage());
        Authentication auth = (Authentication) accessor.getUser();
        if (auth != null && auth.getPrincipal() instanceof Usuario usuario) {
            usuarioRepository.actualizarEstadoConexion(usuario.getId(), true);
        }
    }

    @EventListener
    @Transactional
    public void handleDisconnect(SessionDisconnectEvent event) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(event.getMessage());
        Authentication auth = (Authentication) accessor.getUser();
        if (auth != null && auth.getPrincipal() instanceof Usuario usuario) {
            SimpUser simpUser = simpUserRegistry.getUser(usuario.getUsername());
            if (simpUser == null || simpUser.getSessions().isEmpty()) {
                usuarioRepository.actualizarEstadoConexion(usuario.getId(), false);
            }
        }
    }
}
