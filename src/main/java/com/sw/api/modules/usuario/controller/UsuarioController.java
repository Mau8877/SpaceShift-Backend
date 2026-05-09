package com.sw.api.modules.usuario.controller;

import com.sw.api.modules.usuario.dto.UsuarioPageResponseDTO;
import com.sw.api.modules.usuario.dto.UsuarioPatchRequestDTO;
import com.sw.api.modules.usuario.dto.UsuarioRequestDTO;
import com.sw.api.modules.usuario.dto.UsuarioResponseDTO;
import com.sw.api.modules.usuario.service.UsuarioService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/usuarios")
@RequiredArgsConstructor
public class UsuarioController {

    private final UsuarioService usuarioService;

    @GetMapping
    public ResponseEntity<UsuarioPageResponseDTO> listar(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) Boolean estado,
            @RequestParam(required = false) Boolean estadoConexion) {

        return ResponseEntity.ok(usuarioService.listarUsuarios(page, size, search, estado, estadoConexion));
    }

    @GetMapping("/{id}")
    public ResponseEntity<UsuarioResponseDTO> detalle(@PathVariable UUID id) {
        return ResponseEntity.ok(usuarioService.obtenerDetalle(id));
    }

    @PostMapping
    public ResponseEntity<UsuarioResponseDTO> crear(@Valid @RequestBody UsuarioRequestDTO request) {
        return ResponseEntity.ok(usuarioService.crearUsuario(request));
    }

    @PatchMapping("/{id}")
    public ResponseEntity<UsuarioResponseDTO> editarBasico(
            @PathVariable UUID id,
            @RequestBody UsuarioPatchRequestDTO request) {
        return ResponseEntity.ok(usuarioService.actualizarBasico(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> desactivar(@PathVariable UUID id) {
        usuarioService.desactivar(id);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/activar")
    public ResponseEntity<UsuarioResponseDTO> activar(@PathVariable UUID id) {
        return ResponseEntity.ok(usuarioService.activar(id));
    }
}
