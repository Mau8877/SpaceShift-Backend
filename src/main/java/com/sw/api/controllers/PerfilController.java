package com.sw.api.controllers;

import com.sw.api.dtos.PerfilPatchRequestDTO;
import com.sw.api.dtos.PerfilResponseDTO;
import com.sw.api.services.PerfilService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/perfil")
@RequiredArgsConstructor
public class PerfilController {

    private final PerfilService perfilService;

    @GetMapping("/me")
    public ResponseEntity<PerfilResponseDTO> obtenerMiPerfil(Authentication authentication) {
        String correo = authentication.getName();
        PerfilResponseDTO perfil = perfilService.obtenerPerfilPorCorreo(correo);
        return ResponseEntity.ok(perfil);
    }

    @GetMapping("/usuario/{idUsuario}")
    public ResponseEntity<PerfilResponseDTO> obtenerPerfilPorUsuario(@PathVariable UUID idUsuario) {
        PerfilResponseDTO perfil = perfilService.obtenerPerfilPorIdUsuario(idUsuario);
        return ResponseEntity.ok(perfil);
    }

    @PatchMapping("/usuario/{idUsuario}")
    public ResponseEntity<PerfilResponseDTO> actualizarMiPerfil(
            @PathVariable UUID idUsuario,
            @RequestBody PerfilPatchRequestDTO request,
            Authentication authentication) {
        String correoAutenticado = authentication.getName();
        PerfilResponseDTO perfil = perfilService.actualizarMiPerfil(idUsuario, correoAutenticado, request);
        return ResponseEntity.ok(perfil);
    }
}
