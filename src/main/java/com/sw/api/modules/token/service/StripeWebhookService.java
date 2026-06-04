package com.sw.api.modules.token.service;

import com.stripe.exception.SignatureVerificationException;
import com.stripe.model.Event;
import com.stripe.model.EventDataObjectDeserializer;
import com.stripe.model.checkout.Session;
import com.stripe.net.Webhook;
import com.sw.api.modules.contrato.model.EstadoPago;
import com.sw.api.modules.contrato.model.MetodoPago;
import com.sw.api.modules.contrato.model.PagoContrato;
import com.sw.api.modules.contrato.repository.PagoContratoRepository;
import com.sw.api.modules.notificacion.service.NotificacionService;
import com.sw.api.modules.token.model.PagoStripe;
import com.sw.api.modules.token.model.PaqueteCredito;
import com.sw.api.modules.token.model.TipoTransaccion;
import com.sw.api.modules.token.model.TransaccionCredito;
import com.sw.api.modules.token.repository.PagoStripeRepository;
import com.sw.api.modules.token.repository.PaqueteCreditoRepository;
import com.sw.api.modules.usuario.model.Usuario;
import com.sw.api.modules.usuario.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class StripeWebhookService {

    private final PagoStripeRepository pagoStripeRepository;
    private final PaqueteCreditoRepository paqueteRepository;
    private final UsuarioRepository usuarioRepository;
    private final TokenService tokenService;
    private final PagoContratoRepository pagoContratoRepository;
    private final NotificacionService notificacionService;

    @Value("${stripe.webhook-secret}")
    private String webhookSecret;

    @Transactional
    public void processWebhook(String payload, String sigHeader) throws SignatureVerificationException {
        // 1. Verificar la firma del webhook para garantizar que viene de Stripe
        Event event = Webhook.constructEvent(payload, sigHeader, webhookSecret);

        log.info("Recibido evento de Stripe: {}, ID: {}", event.getType(), event.getId());

        // 2. Procesar el evento de Checkout Exitoso
        if ("checkout.session.completed".equals(event.getType())) {
            EventDataObjectDeserializer dataObjectDeserializer = event.getDataObjectDeserializer();
            try {
                Session session = (Session) dataObjectDeserializer.deserializeUnsafe();
                if (session.getMetadata() != null && "CONTRATO_PAGO".equals(session.getMetadata().get("tipo"))) {
                    acreditarPagoContrato(session);
                } else {
                    acreditarCompraSession(session);
                }
            } catch (Exception e) {
                log.error("Error al procesar webhook de Stripe", e);
                throw new RuntimeException("Error al procesar evento de Stripe", e);
            }
        }
    }

    private void acreditarPagoContrato(Session session) {
        String stripeSessionId = session.getId();
        String pagoIdStr = session.getMetadata().get("pagoId");

        if (pagoIdStr == null) {
            log.error("Falta metadata pagoId en la sesión de Stripe");
            throw new IllegalArgumentException("Metadata pagoId faltante");
        }

        UUID pagoId = UUID.fromString(pagoIdStr);
        PagoContrato pago = pagoContratoRepository.findById(pagoId)
                .orElseThrow(() -> new RuntimeException("Pago de contrato no encontrado: " + pagoId));

        if (pago.getEstadoPago() == EstadoPago.COMPLETADO) {
            log.info("El pago con ID {} ya se encuentra completado (idempotencia).", pagoId);
            return;
        }

        pago.setEstadoPago(EstadoPago.COMPLETADO);
        pago.setMetodoPago(MetodoPago.STRIPE);
        pago.setFechaPago(LocalDateTime.now());
        pago.setStripePagoId(stripeSessionId);
        pagoContratoRepository.save(pago);

        log.info("¡Pago de contrato completado vía Stripe con éxito! Pago ID: {}", pagoId);

        // Notificar al propietario y cliente
        notificacionService.enviarNotificacion(
                pago.getContrato().getPropietario().getId(),
                "Pago de Contrato Recibido",
                "Se completó el pago de " + pago.getMonto() + " " + pago.getMoneda() + " vía Stripe.",
                Map.of("type", "PAYMENT_APPROVED", "contratoId", pago.getContrato().getId().toString())
        );

        notificacionService.enviarNotificacion(
                pago.getContrato().getCliente().getId(),
                "Pago Procesado con Éxito",
                "Tu pago de " + pago.getMonto() + " " + pago.getMoneda() + " vía Stripe fue procesado con éxito.",
                Map.of("type", "PAYMENT_APPROVED", "contratoId", pago.getContrato().getId().toString())
        );
    }

    private void acreditarCompraSession(Session session) {
        String stripeSessionId = session.getId();

        // 3. Garantizar IDEMPOTENCIA estricta (no duplicar saldo si reenvían el webhook)
        if (pagoStripeRepository.existsByStripeSessionId(stripeSessionId)) {
            log.warn("El pago con SessionID {} ya fue procesado e ignorado para evitar duplicados.", stripeSessionId);
            return;
        }

        // 4. Leer metadata inyectada durante la creación
        log.info("[DEBUG - StripeWebhook] SessionID: {}, Metadata recibida de Stripe: {}", 
                stripeSessionId, session.getMetadata());

        String usuarioIdStr = session.getMetadata().get("usuarioId");
        String paqueteIdStr = session.getMetadata().get("paqueteId");
        String cantidadCreditosStr = session.getMetadata().get("cantidadCreditos");

        if (usuarioIdStr == null || paqueteIdStr == null || cantidadCreditosStr == null) {
            log.error("Falta metadata crucial en la sesión de Stripe: usuarioId={}, paqueteId={}, cantidadCreditos={}",
                    usuarioIdStr, paqueteIdStr, cantidadCreditosStr);
            throw new IllegalArgumentException("Metadata faltante en sesión de Stripe");
        }

        UUID usuarioId = UUID.fromString(usuarioIdStr);
        UUID paqueteId = UUID.fromString(paqueteIdStr);
        int cantidadCreditos = Integer.parseInt(cantidadCreditosStr);

        Usuario usuario = usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado al acreditar compra: " + usuarioId));

        PaqueteCredito paquete = paqueteRepository.findById(paqueteId)
                .orElseThrow(() -> new RuntimeException("Paquete no encontrado al acreditar compra: " + paqueteId));

        log.info("Acreditando compra para el usuario {}: {} créditos por paquete {}", 
                usuario.getUsername(), cantidadCreditos, paquete.getNombrePaquete());

        // 5. Acreditar créditos al Perfil del usuario y crear TransaccionCredito de forma transaccional segura
        String descripcion = "Compra de " + paquete.getNombrePaquete() + " (Stripe)";
        TransaccionCredito transaccion = tokenService.acreditarCreditos(
                usuarioId, 
                cantidadCreditos, 
                descripcion, 
                TipoTransaccion.RECARGA_CREDITOS
        );

        // 6. Registrar el Pago de Stripe para el historial contable e idempotencia
        BigDecimal monto = BigDecimal.valueOf(session.getAmountTotal() / 100.0); // Convertir centavos a decimal
        String moneda = session.getCurrency().toUpperCase();

        PagoStripe pago = PagoStripe.builder()
                .usuario(usuario)
                .paqueteCredito(paquete)
                .transaccionCredito(transaccion)
                .stripeSessionId(stripeSessionId)
                .stripePaymentIntentId(session.getPaymentIntent())
                .monto(monto)
                .moneda(moneda)
                .estadoPago("COMPLETADO")
                .build();

        pagoStripeRepository.save(pago);
        log.info("¡Pago de Stripe registrado con éxito! ID de Pago: {}, Total: {} {}", pago.getId(), monto, moneda);
    }
}
