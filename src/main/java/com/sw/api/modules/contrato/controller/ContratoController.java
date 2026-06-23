package com.sw.api.modules.contrato.controller;

import com.sw.api.modules.contrato.dto.ContratoRequestDTO;
import com.sw.api.modules.contrato.dto.ContratoResponseDTO;
import com.sw.api.modules.contrato.service.ContratoService;
import com.sw.api.modules.usuario.model.Usuario;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/contratos")
@RequiredArgsConstructor
public class ContratoController {

    private final ContratoService contratoService;

    @PostMapping
    public ResponseEntity<ContratoResponseDTO> crearContrato(
            @RequestBody ContratoRequestDTO dto,
            @AuthenticationPrincipal Usuario usuario) {
        // Opcionalmente podemos forzar el idCliente como el usuario autenticado si es el cliente quien solicita el contrato.
        // Pero para dar flexibilidad si lo inicia un administrador o un agente, respetamos el idCliente del DTO si viene especificado.
        if (dto.getIdCliente() == null) {
            dto.setIdCliente(usuario.getId());
        }
        return ResponseEntity.ok(contratoService.crearContrato(dto));
    }

    @PostMapping("/{id}/firmar")
    public ResponseEntity<ContratoResponseDTO> firmarContrato(
            @PathVariable UUID id,
            @RequestBody(required = false) com.sw.api.modules.contrato.dto.FirmaContratoRequestDTO dto,
            @AuthenticationPrincipal Usuario usuario) {
        return ResponseEntity.ok(contratoService.firmarContrato(id, dto));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ContratoResponseDTO> obtenerContratoPorId(
            @PathVariable UUID id,
            @AuthenticationPrincipal Usuario usuario) {
        return ResponseEntity.ok(contratoService.obtenerContratoPorId(id));
    }

    @GetMapping("/propietario")
    public ResponseEntity<List<ContratoResponseDTO>> obtenerContratosComoPropietario(
            @AuthenticationPrincipal Usuario usuario) {
        return ResponseEntity.ok(contratoService.obtenerContratosPropietario(usuario.getId()));
    }

    @GetMapping("/cliente")
    public ResponseEntity<List<ContratoResponseDTO>> obtenerContratosComoCliente(
            @AuthenticationPrincipal Usuario usuario) {
        return ResponseEntity.ok(contratoService.obtenerContratosCliente(usuario.getId()));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminarContrato(
            @PathVariable UUID id,
            @AuthenticationPrincipal Usuario usuario) {
        contratoService.eliminarContrato(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/cancelar")
    public ResponseEntity<ContratoResponseDTO> cancelarContrato(
            @PathVariable UUID id,
            @AuthenticationPrincipal Usuario usuario) {
        return ResponseEntity.ok(contratoService.cancelarContrato(id));
    }
}
