package com.sw.api.modules.notificacion.controller;

import com.sw.api.modules.notificacion.dto.RegistrarTokenRequest;
import com.sw.api.modules.notificacion.service.NotificacionService;
import com.sw.api.modules.usuario.model.Usuario;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/notificaciones")
@RequiredArgsConstructor
public class NotificacionController {

    private final NotificacionService notificacionService;

    @PostMapping("/token")
    public ResponseEntity<Void> registrarToken(
            @RequestBody @Valid RegistrarTokenRequest request,
            @AuthenticationPrincipal Usuario usuario) {
        notificacionService.registrarToken(usuario, request.tokenFcm(), request.plataforma());
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/token")
    public ResponseEntity<Void> revocarToken(
            @RequestBody @Valid RegistrarTokenRequest request) {
        notificacionService.revocarToken(request.tokenFcm());
        return ResponseEntity.noContent().build();
    }
}
