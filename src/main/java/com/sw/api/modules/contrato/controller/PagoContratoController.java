package com.sw.api.modules.contrato.controller;

import com.sw.api.modules.contrato.dto.PagoContratoResponseDTO;
import com.sw.api.modules.contrato.service.PagoContratoService;
import com.sw.api.modules.usuario.model.Usuario;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class PagoContratoController {

    private final PagoContratoService pagoContratoService;

    @GetMapping("/contratos/{contratoId}/pagos")
    public ResponseEntity<List<PagoContratoResponseDTO>> obtenerPagosDeContrato(
            @PathVariable UUID contratoId,
            @AuthenticationPrincipal Usuario usuario) {
        return ResponseEntity.ok(pagoContratoService.obtenerPagosDeContrato(contratoId));
    }

    @PostMapping("/pagos/{id}/comprobante")
    public ResponseEntity<PagoContratoResponseDTO> subirComprobanteTransferencia(
            @PathVariable UUID id,
            @RequestParam("comprobante") MultipartFile file,
            @AuthenticationPrincipal Usuario usuario) throws IOException {
        return ResponseEntity.ok(pagoContratoService.subirComprobanteTransferencia(id, file));
    }

    @PostMapping("/pagos/{id}/aprobar")
    public ResponseEntity<PagoContratoResponseDTO> aprobarPagoManual(
            @PathVariable UUID id,
            @AuthenticationPrincipal Usuario usuario) {
        return ResponseEntity.ok(pagoContratoService.aprobarPagoManual(id, usuario.getId()));
    }

    @PostMapping("/pagos/{id}/registrar-efectivo")
    public ResponseEntity<PagoContratoResponseDTO> registrarPagoEfectivo(
            @PathVariable UUID id,
            @AuthenticationPrincipal Usuario usuario) {
        return ResponseEntity.ok(pagoContratoService.registrarPagoEfectivo(id, usuario.getId()));
    }

    @PostMapping("/pagos/{id}/stripe-checkout")
    public ResponseEntity<Map<String, String>> generarSesionPagoStripe(
            @PathVariable UUID id,
            @RequestParam(value = "originUrl", required = false) String originUrl,
            @AuthenticationPrincipal Usuario usuario) throws Exception {
        String sessionUrl = pagoContratoService.generarSesionPagoStripe(id, usuario.getId(), originUrl);
        Map<String, String> response = new HashMap<>();
        response.put("stripeCheckoutUrl", sessionUrl);
        return ResponseEntity.ok(response);
    }
}
