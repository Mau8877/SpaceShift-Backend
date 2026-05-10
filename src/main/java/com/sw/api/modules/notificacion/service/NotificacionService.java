package com.sw.api.modules.notificacion.service;

import com.google.firebase.messaging.AndroidConfig;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingException;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.Notification;
import com.sw.api.modules.chat.model.PlataformaDispositivo;
import com.sw.api.modules.chat.model.TokenDispositivo;
import com.sw.api.modules.chat.repository.TokenDispositivoRepository;
import com.sw.api.modules.usuario.model.Usuario;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class NotificacionService {

    private final TokenDispositivoRepository tokenDispositivoRepository;

    public void registrarToken(Usuario usuario, String tokenFcm, PlataformaDispositivo plataforma) {
        TokenDispositivo token = tokenDispositivoRepository
                .findByTokenFcm(tokenFcm)
                .orElse(new TokenDispositivo());
        token.setUsuario(usuario);
        token.setTokenFcm(tokenFcm);
        token.setPlataforma(plataforma);
        tokenDispositivoRepository.save(token);
    }

    public void revocarToken(String tokenFcm) {
        tokenDispositivoRepository.findByTokenFcm(tokenFcm)
                .ifPresent(tokenDispositivoRepository::delete);
    }

    public void enviarNotificacion(UUID usuarioId, String titulo, String cuerpo, Map<String, String> data) {
        List<TokenDispositivo> tokens = tokenDispositivoRepository.findByUsuarioId(usuarioId);
        if (tokens.isEmpty()) return;

        for (TokenDispositivo tokenDispositivo : tokens) {
            try {
                Message message = Message.builder()
                        .setToken(tokenDispositivo.getTokenFcm())
                        .setNotification(Notification.builder()
                                .setTitle(titulo)
                                .setBody(cuerpo)
                                .build())
                        .putAllData(data)
                        .setAndroidConfig(AndroidConfig.builder()
                                .setPriority(AndroidConfig.Priority.HIGH)
                                .build())
                        .build();
                FirebaseMessaging.getInstance().send(message);
            } catch (FirebaseMessagingException e) {
                if ("UNREGISTERED".equals(e.getMessagingErrorCode().name())) {
                    tokenDispositivoRepository.delete(tokenDispositivo);
                }
            }
        }
    }
}
