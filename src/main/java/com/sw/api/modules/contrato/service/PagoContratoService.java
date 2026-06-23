package com.sw.api.modules.contrato.service;

import com.sw.api.modules.contrato.dto.PagoContratoResponseDTO;
import com.sw.api.modules.contrato.model.*;
import com.sw.api.modules.contrato.repository.PagoContratoRepository;
import com.sw.api.modules.notificacion.service.NotificacionService;
import com.sw.api.modules.token.service.StripeService;
import com.sw.api.shared.service.CloudinaryService;
import com.sw.api.modules.usuario.repository.PerfilRepository;
import com.sw.api.modules.usuario.model.Perfil;
import com.sw.api.modules.reporte.service.PdfGeneratorService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PagoContratoService {

    private final PagoContratoRepository pagoContratoRepository;
    private final CloudinaryService cloudinaryService;
    private final NotificacionService notificacionService;
    private final StripeService stripeService;
    private final PerfilRepository perfilRepository;
    private final PdfGeneratorService pdfGeneratorService;

    public List<PagoContratoResponseDTO> obtenerPagosDeContrato(UUID contratoId) {
        return pagoContratoRepository.findByContratoIdOrderByFechaVencimientoAsc(contratoId)
                .stream().map(this::mapToResponse).collect(Collectors.toList());
    }

    @Transactional
    public PagoContratoResponseDTO subirComprobanteTransferencia(UUID pagoId, MultipartFile file) throws IOException {
        PagoContrato pago = pagoContratoRepository.findById(pagoId)
                .orElseThrow(() -> new IllegalArgumentException("Pago no encontrado"));

        if (pago.getEstadoPago() == EstadoPago.COMPLETADO) {
            throw new IllegalStateException("El pago ya está completado.");
        }

        String secureUrl = cloudinaryService.uploadFile(file);
        pago.setDocumentoComprobanteUrl(secureUrl);
        pago.setMetodoPago(MetodoPago.TRANSFERENCIA_BANCARIA);
        pago.setEstadoPago(EstadoPago.PENDIENTE); // Espera aprobación del dueño
        pago = pagoContratoRepository.save(pago);

        // Notificar al dueño
        notificacionService.enviarNotificacion(
                pago.getContrato().getPropietario().getId(),
                "Comprobante de pago recibido",
                "El cliente ha subido un comprobante para el pago de " + pago.getMonto() + " " + pago.getMoneda(),
                Map.of("type", "PAYMENT_RECEIPT_SUBMITTED", "pagoId", pago.getId().toString())
        );

        return mapToResponse(pago);
    }

    @Transactional
    public PagoContratoResponseDTO aprobarPagoManual(UUID pagoId, UUID ownerId) {
        PagoContrato pago = pagoContratoRepository.findById(pagoId)
                .orElseThrow(() -> new IllegalArgumentException("Pago no encontrado"));

        if (!pago.getContrato().getPropietario().getId().equals(ownerId)) {
            throw new SecurityException("No tienes permisos para aprobar este pago.");
        }

        if (pago.getEstadoPago() == EstadoPago.COMPLETADO) {
            throw new IllegalStateException("El pago ya está completado.");
        }

        pago.setEstadoPago(EstadoPago.COMPLETADO);
        pago.setFechaPago(LocalDateTime.now());
        pago = pagoContratoRepository.save(pago);

        // Notificar al cliente
        notificacionService.enviarNotificacion(
                pago.getContrato().getCliente().getId(),
                "Pago Aprobado",
                "Tu pago de " + pago.getMonto() + " " + pago.getMoneda() + " ha sido aprobado con éxito.",
                Map.of("type", "PAYMENT_APPROVED", "contratoId", pago.getContrato().getId().toString())
        );

        return mapToResponse(pago);
    }

    @Transactional
    public PagoContratoResponseDTO registrarPagoEfectivo(UUID pagoId, UUID ownerId) {
        PagoContrato pago = pagoContratoRepository.findById(pagoId)
                .orElseThrow(() -> new IllegalArgumentException("Pago no encontrado"));

        if (!pago.getContrato().getPropietario().getId().equals(ownerId)) {
            throw new SecurityException("No tienes permisos para registrar este pago.");
        }

        pago.setEstadoPago(EstadoPago.COMPLETADO);
        pago.setMetodoPago(MetodoPago.EFECTIVO);
        pago.setFechaPago(LocalDateTime.now());
        pago = pagoContratoRepository.save(pago);

        // Notificar al cliente
        notificacionService.enviarNotificacion(
                pago.getContrato().getCliente().getId(),
                "Pago en Efectivo Registrado",
                "Se registró tu pago en efectivo de " + pago.getMonto() + " " + pago.getMoneda(),
                Map.of("type", "PAYMENT_APPROVED", "contratoId", pago.getContrato().getId().toString())
        );

        return mapToResponse(pago);
    }

    public String generarSesionPagoStripe(UUID pagoId, UUID usuarioId, String originUrl) throws Exception {
        PagoContrato pago = pagoContratoRepository.findById(pagoId)
                .orElseThrow(() -> new IllegalArgumentException("Pago no encontrado"));

        if (pago.getEstadoPago() == EstadoPago.COMPLETADO) {
            throw new IllegalStateException("El pago ya está completado.");
        }

        String concept = "Pago de mensualidad - Contrato " + pago.getContrato().getInmueble().getTipoInmueble();
        if (pago.getTipoPago() == TipoPago.GARANTIA) {
            concept = "Pago de garantía - Contrato " + pago.getContrato().getInmueble().getTipoInmueble();
        } else if (pago.getTipoPago() == TipoPago.CUOTA_VENTA) {
            concept = "Pago de cuota - Compraventa Inmueble";
        }

        UUID publicacionId = null;
        if (pago.getContrato().getPublicacion() != null) {
            publicacionId = pago.getContrato().getPublicacion().getId();
        }

        return stripeService.createCheckoutSessionForPayment(
                usuarioId,
                pago.getId(),
                pago.getContrato().getId(),
                publicacionId,
                pago.getMonto(),
                pago.getMoneda(),
                concept,
                originUrl
        );
    }

    public byte[] generarPdfReciboPago(UUID pagoId, UUID usuarioId) {
        PagoContrato pago = pagoContratoRepository.findById(pagoId)
                .orElseThrow(() -> new IllegalArgumentException("Pago no encontrado"));

        if (pago.getEstadoPago() != EstadoPago.COMPLETADO) {
            throw new IllegalStateException("El recibo solo está disponible para pagos completados.");
        }

        if (!pago.getContrato().getPropietario().getId().equals(usuarioId) &&
                !pago.getContrato().getCliente().getId().equals(usuarioId)) {
            throw new SecurityException("No tienes permisos para descargar el recibo de este pago.");
        }

        String clientName = "Cliente";
        String clientEmail = pago.getContrato().getCliente().getCorreo();

        Perfil perfilCliente = perfilRepository.findByUsuarioId(pago.getContrato().getCliente().getId()).orElse(null);
        if (perfilCliente != null) {
            clientName = perfilCliente.getNombre() + " " + (perfilCliente.getApellido() != null ? perfilCliente.getApellido() : "");
            clientName = clientName.trim();
        }

        String concept = "Pago de mensualidad - Contrato " + pago.getContrato().getInmueble().getTipoInmueble();
        if (pago.getTipoPago() == TipoPago.GARANTIA) {
            concept = "Pago de garantía - Contrato " + pago.getContrato().getInmueble().getTipoInmueble();
        } else if (pago.getTipoPago() == TipoPago.CUOTA_VENTA) {
            concept = "Pago de cuota - Compraventa Inmueble";
        } else if (pago.getTipoPago() == TipoPago.DEPOSITO_ANTICRETICO) {
            concept = "Pago de depósito anticrético - Contrato " + pago.getContrato().getInmueble().getTipoInmueble();
        }

        String codigoContrato = "CTR-" + pago.getContrato().getTipoContrato().name().substring(0, 3) + "-" + pago.getContrato().getId().toString().substring(0, 8).toUpperCase();

        return pdfGeneratorService.generarPdfReciboPago(
                codigoContrato,
                clientName,
                clientEmail,
                concept,
                pago.getMonto(),
                pago.getMoneda(),
                pago.getStripePagoId(),
                pago.getFechaPago()
        );
    }

    private PagoContratoResponseDTO mapToResponse(PagoContrato p) {
        PagoContratoResponseDTO dto = new PagoContratoResponseDTO();
        dto.setId(p.getId());
        dto.setIdContrato(p.getContrato().getId());
        dto.setMonto(p.getMonto());
        dto.setMoneda(p.getMoneda());
        dto.setTipoPago(p.getTipoPago());
        dto.setEstadoPago(p.getEstadoPago());
        dto.setMetodoPago(p.getMetodoPago());
        dto.setFechaVencimiento(p.getFechaVencimiento());
        dto.setFechaPago(p.getFechaPago());
        dto.setDocumentoComprobanteUrl(p.getDocumentoComprobanteUrl());
        dto.setStripePagoId(p.getStripePagoId());
        return dto;
    }
}
