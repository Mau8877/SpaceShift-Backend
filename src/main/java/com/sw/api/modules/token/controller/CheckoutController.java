package com.sw.api.modules.token.controller;

import com.sw.api.modules.token.service.StripeService;
import com.sw.api.modules.token.service.StripeWebhookService;
import com.sw.api.modules.usuario.model.Usuario;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@Slf4j
public class CheckoutController {

    private final StripeService stripeService;
    private final StripeWebhookService stripeWebhookService;

    @PostMapping("/checkout/crear-sesion")
    public ResponseEntity<Map<String, String>> crearSesion(
            @RequestParam UUID paqueteId,
            Authentication authentication) {
        try {
            Usuario usuario = (Usuario) authentication.getPrincipal();
            log.info("Usuario {} inicia proceso de compra de paquete {}", usuario.getUsername(), paqueteId);
            
            String sessionUrl = stripeService.createCheckoutSession(usuario.getId(), paqueteId);
            
            Map<String, String> response = new HashMap<>();
            response.put("sessionUrl", sessionUrl);
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error al crear sesión de cobro de Stripe", e);
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    @PostMapping("/webhooks/stripe")
    public ResponseEntity<String> handleStripeWebhook(
            @RequestBody String payload,
            @RequestHeader("Stripe-Signature") String sigHeader) {
        try {
            log.info("Recibida notificación de Webhook desde Stripe");
            stripeWebhookService.processWebhook(payload, sigHeader);
            return ResponseEntity.ok("Webhook recibido y procesado correctamente");
        } catch (Exception e) {
            log.error("Error crítico al procesar webhook de Stripe", e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body("Error al procesar la firma o el evento: " + e.getMessage());
        }
    }
}
