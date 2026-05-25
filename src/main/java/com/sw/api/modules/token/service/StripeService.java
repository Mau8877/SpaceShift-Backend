package com.sw.api.modules.token.service;

import com.stripe.Stripe;
import com.stripe.exception.StripeException;
import com.stripe.model.checkout.Session;
import com.stripe.param.checkout.SessionCreateParams;
import com.sw.api.modules.token.model.PaqueteCredito;
import com.sw.api.modules.token.repository.PaqueteCreditoRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class StripeService {

    private final PaqueteCreditoRepository paqueteRepository;

    @Value("${stripe.secret-key}")
    private String stripeSecretKey;

    @Value("${stripe.success-url}")
    private String successUrl;

    @Value("${stripe.cancel-url}")
    private String cancelUrl;

    @PostConstruct
    public void init() {
        Stripe.apiKey = stripeSecretKey;
    }

    public String createCheckoutSession(UUID usuarioId, UUID paqueteId) throws StripeException {
        // 1. Obtener paquete dinámico de base de datos
        PaqueteCredito paquete = paqueteRepository.findById(paqueteId)
                .orElseThrow(() -> new IllegalArgumentException("El paquete de créditos seleccionado no existe o está inactivo"));

        log.info("[DEBUG - StripeService] Generando sesión de Stripe. usuarioId: {}, paqueteId: {}, créditos: {}, precio: {} BOB",
                usuarioId, paquete.getId(), paquete.getCreditosPaquetes(), paquete.getPrecio());

        // 2. Crear parámetros dinámicos con moneda BOB (Bolivianos)
        SessionCreateParams params = SessionCreateParams.builder()
                .setMode(SessionCreateParams.Mode.PAYMENT)
                .setSuccessUrl(successUrl)
                .setCancelUrl(cancelUrl)
                
                // Elemento de línea para el cobro
                .addLineItem(
                        SessionCreateParams.LineItem.builder()
                                .setQuantity(1L)
                                .setPriceData(
                                        SessionCreateParams.LineItem.PriceData.builder()
                                                .setCurrency("bob") // Bolivianos
                                                .setUnitAmount((long) (paquete.getPrecio().doubleValue() * 100)) // En centavos de BOB
                                                .setProductData(
                                                        SessionCreateParams.LineItem.PriceData.ProductData.builder()
                                                                .setName(paquete.getNombrePaquete())
                                                                .setDescription(paquete.getDescripcion())
                                                                .build()
                                                )
                                                .build()
                                )
                                .build()
                )
                
                // Metadata indispensable para acreditar en Webhook de forma asíncrona y segura
                .putMetadata("usuarioId", usuarioId.toString())
                .putMetadata("paqueteId", paquete.getId().toString())
                .putMetadata("cantidadCreditos", String.valueOf(paquete.getCreditosPaquetes()))
                .build();

        // 3. Crear sesión en el API de Stripe y retornar su URL segura
        Session session = Session.create(params);
        log.info("[DEBUG - StripeService] Sesión de Stripe creada exitosamente. SessionId: {}, URL: {}", session.getId(), session.getUrl());
        return session.getUrl();
    }
}
