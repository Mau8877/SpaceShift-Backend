package com.sw.api.modules.auth.controller;

import com.sw.api.modules.auth.dto.AuthResponse;
import com.sw.api.modules.auth.dto.LoginRequest;
import com.sw.api.modules.auth.dto.RegisterRequest;
import com.sw.api.modules.auth.dto.RefreshTokenRequest;
import com.sw.api.modules.auth.dto.RecuperarPasswordRequest;
import com.sw.api.modules.auth.dto.ValidarCodigoRequest;
import com.sw.api.modules.auth.dto.CambiarPasswordRequest;
import com.sw.api.modules.auth.dto.MensajeResponse;
import com.sw.api.modules.auth.service.AuthService;
import com.sw.api.modules.usuario.model.Usuario;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/registro")
    public ResponseEntity<AuthResponse> registrar(@RequestBody RegisterRequest request) {
        AuthResponse response = authService.registrar(request);
        return ResponseEntity.ok()
                .headers(crearCookieHibrida(response.token()))
                .body(response);
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@RequestBody LoginRequest request) {
        AuthResponse response = authService.login(request);
        return ResponseEntity.ok()
                .headers(crearCookieHibrida(response.token()))
                .body(response);
    }

    private HttpHeaders crearCookieHibrida(String token) {
        ResponseCookie cookie = ResponseCookie.from("jwt_token", token)
                .httpOnly(true)
                .secure(false)
                .path("/")
                .maxAge(24 * 60 * 60)
                .sameSite("Strict")
                .build();

        HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.SET_COOKIE, cookie.toString());
        return headers;
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@AuthenticationPrincipal Usuario usuario) {
        if (usuario != null) {
            authService.logout(usuario);
        }
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse> refresh(@RequestBody RefreshTokenRequest request) {
        return ResponseEntity.ok(authService.actualizarToken(request));
    }

    @PostMapping("/recuperar-password")
    public ResponseEntity<MensajeResponse> recuperarPassword(@RequestBody RecuperarPasswordRequest request) {
        MensajeResponse response = authService.solicitarRecuperacion(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/validar-codigo")
    public ResponseEntity<MensajeResponse> validarCodigo(@RequestBody ValidarCodigoRequest request) {
        MensajeResponse response = authService.validarCodigo(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/cambiar-password")
    public ResponseEntity<MensajeResponse> cambiarPassword(@RequestBody CambiarPasswordRequest request) {
        MensajeResponse response = authService.cambiarPassword(request);
        return ResponseEntity.ok(response);
    }
}
