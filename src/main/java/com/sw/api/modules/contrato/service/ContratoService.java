package com.sw.api.modules.contrato.service;

import com.sw.api.modules.contrato.dto.ContratoRequestDTO;
import com.sw.api.modules.contrato.dto.ContratoResponseDTO;
import com.sw.api.modules.contrato.model.*;
import com.sw.api.modules.contrato.repository.ContratoRepository;
import com.sw.api.modules.contrato.repository.PagoContratoRepository;
import com.sw.api.modules.inmueble.model.Inmueble;
import com.sw.api.modules.inmueble.repository.InmuebleRepository;
import com.sw.api.modules.notificacion.service.NotificacionService;
import com.sw.api.modules.publicacion.model.Publicacion;
import com.sw.api.modules.publicacion.repository.PublicacionRepository;
import com.sw.api.modules.usuario.model.Usuario;
import com.sw.api.modules.usuario.repository.UsuarioRepository;
import com.sw.api.modules.usuario.repository.PerfilRepository;
import com.sw.api.modules.blockchain.service.Web3Service;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ContratoService {

    private final ContratoRepository contratoRepository;
    private final PagoContratoRepository pagoContratoRepository;
    private final InmuebleRepository inmuebleRepository;
    private final PublicacionRepository publicacionRepository;
    private final UsuarioRepository usuarioRepository;
    private final NotificacionService notificacionService;
    private final PerfilRepository perfilRepository;
    private final Web3Service web3Service;

    @Transactional
    public ContratoResponseDTO crearContrato(ContratoRequestDTO dto) {
        Inmueble inmueble = inmuebleRepository.findById(dto.getIdInmueble())
                .orElseThrow(() -> new IllegalArgumentException("Inmueble no encontrado"));

        Usuario cliente = usuarioRepository.findById(dto.getIdCliente())
                .orElseThrow(() -> new IllegalArgumentException("Cliente no encontrado"));

        Publicacion publicacion = null;
        Usuario propietario = null;

        if (dto.getIdPublicacion() != null) {
            publicacion = publicacionRepository.findById(dto.getIdPublicacion())
                    .orElseThrow(() -> new IllegalArgumentException("Publicación no encontrada"));
            propietario = publicacion.getUsuario();
        } else {
            throw new IllegalArgumentException("Se requiere idPublicacion para definir al propietario.");
        }

        // Validar colisión de reservas
        if (dto.getTipoContrato() == TipoContrato.ALQUILER || dto.getTipoContrato() == TipoContrato.ALOJAMIENTO) {
            boolean overlaps = contratoRepository.overlapsWithExistingBooking(inmueble.getId(), dto.getFechaInicio(), dto.getFechaFin());
            if (overlaps) {
                throw new IllegalStateException("El inmueble ya cuenta con un contrato vigente para el rango de fechas seleccionado.");
            }
        }

        Contrato contrato = new Contrato();
        contrato.setInmueble(inmueble);
        contrato.setPublicacion(publicacion);
        contrato.setPropietario(propietario);
        contrato.setCliente(cliente);
        contrato.setTipoContrato(dto.getTipoContrato());
        contrato.setEstadoContrato(EstadoContrato.PENDIENTE_FIRMA);
        contrato.setMontoAcordado(dto.getMontoAcordado());
        contrato.setMoneda(dto.getMoneda());
        contrato.setFechaInicio(dto.getFechaInicio());
        contrato.setFechaFin(dto.getFechaFin());
        contrato.setCantidadHuespedes(dto.getCantidadHuespedes());
        contrato.setObservacion(dto.getObservacion());
        contrato.setEspecificaciones(dto.getEspecificaciones());

        if (dto.getFechaInicio() != null && dto.getFechaFin() != null) {
            long nights = ChronoUnit.DAYS.between(dto.getFechaInicio(), dto.getFechaFin());
            contrato.setNoches((int) nights);
        }

        contrato = contratoRepository.save(contrato);

        // Generación de cobros/pagos según tipo de contrato
        generarPlanDePagos(contrato, dto.getEspecificaciones());

        // Notificar al cliente
        notificacionService.enviarNotificacion(
                cliente.getId(),
                "Nueva propuesta de contrato",
                "Tienes una nueva propuesta de contrato para " + (publicacion != null ? publicacion.getTitulo() : "un inmueble"),
                Map.of("type", "NEW_CONTRACT", "contratoId", contrato.getId().toString())
        );

        return mapToResponse(contrato);
    }

    @Transactional
    public ContratoResponseDTO firmarContrato(UUID contratoId) {
        Contrato contrato = contratoRepository.findById(contratoId)
                .orElseThrow(() -> new IllegalArgumentException("Contrato no encontrado"));

        if (contrato.getEstadoContrato() != EstadoContrato.PENDIENTE_FIRMA) {
            throw new IllegalStateException("El contrato no está en estado de firma pendiente");
        }

        contrato.setEstadoContrato(EstadoContrato.VIGENTE);

        // Generar wallet determinista del inquilino
        String targetWallet = web3Service.generateDeterministicWalletAddress(contrato.getCliente().getId());

        if (targetWallet != null && !targetWallet.trim().isEmpty()) {
            final String finalWallet = targetWallet;
            perfilRepository.findByUsuarioId(contrato.getCliente().getId()).ifPresent(perfil -> {
                perfil.setWalletAddress(finalWallet);
                perfilRepository.save(perfil);
            });

            // Intentar registrar el contrato en la Blockchain
            try {
                String txHash = web3Service.registerPropertyContractOnChain(
                        contrato.getId().toString(),
                        finalWallet
                );
                if (txHash != null) {
                    contrato.setTransactionHash(txHash);
                }
            } catch (Exception e) {
                // Loguear error sin bloquear el flujo local de la aplicacion
                System.err.println("Advertencia: No se pudo registrar en la Blockchain: " + e.getMessage());
            }
        }

        contrato = contratoRepository.save(contrato);

        // Notificar al propietario
        notificacionService.enviarNotificacion(
                contrato.getPropietario().getId(),
                "Contrato Firmado",
                "El contrato para el inmueble " + contrato.getInmueble().getTipoInmueble() + " ha sido firmado por el cliente.",
                Map.of("type", "CONTRACT_SIGNED", "contratoId", contrato.getId().toString())
        );

        return mapToResponse(contrato);
    }

    public List<ContratoResponseDTO> obtenerContratosPropietario(UUID propietarioId) {
        return contratoRepository.findByPropietarioIdOrderByCreatedDateDesc(propietarioId)
                .stream().map(this::mapToResponse).collect(Collectors.toList());
    }

    public List<ContratoResponseDTO> obtenerContratosCliente(UUID clienteId) {
        return contratoRepository.findByClienteIdOrderByCreatedDateDesc(clienteId)
                .stream().map(this::mapToResponse).collect(Collectors.toList());
    }

    public ContratoResponseDTO obtenerContratoPorId(UUID contratoId) {
        Contrato contrato = contratoRepository.findById(contratoId)
                .orElseThrow(() -> new IllegalArgumentException("Contrato no encontrado"));
        return mapToResponse(contrato);
    }

    private void generarPlanDePagos(Contrato contrato, Map<String, Object> especificaciones) {
        LocalDate start = contrato.getFechaInicio();
        LocalDate end = contrato.getFechaFin();
        BigDecimal monto = contrato.getMontoAcordado();
        String moneda = contrato.getMoneda();

        if (contrato.getTipoContrato() == TipoContrato.ALOJAMIENTO) {
            PagoContrato pago = new PagoContrato();
            pago.setContrato(contrato);
            pago.setMonto(monto);
            pago.setMoneda(moneda);
            pago.setTipoPago(TipoPago.MENSUALIDAD);
            pago.setEstadoPago(EstadoPago.PENDIENTE);
            pago.setMetodoPago(MetodoPago.EFECTIVO);
            pago.setFechaVencimiento(start != null ? start : LocalDate.now());
            pagoContratoRepository.save(pago);

        } else if (contrato.getTipoContrato() == TipoContrato.ALQUILER) {
            // 1. Garantía (si está en especificaciones)
            if (especificaciones != null && especificaciones.containsKey("monto_garantia")) {
                Object gVal = especificaciones.get("monto_garantia");
                BigDecimal garantiaMonto = new BigDecimal(gVal.toString());
                if (garantiaMonto.compareTo(BigDecimal.ZERO) > 0) {
                    PagoContrato garantia = new PagoContrato();
                    garantia.setContrato(contrato);
                    garantia.setMonto(garantiaMonto);
                    garantia.setMoneda(moneda);
                    garantia.setTipoPago(TipoPago.GARANTIA);
                    garantia.setEstadoPago(EstadoPago.PENDIENTE);
                    garantia.setMetodoPago(MetodoPago.EFECTIVO);
                    garantia.setFechaVencimiento(start != null ? start : LocalDate.now());
                    pagoContratoRepository.save(garantia);
                }
            }

            // 2. Mensualidades
            if (start != null && end != null) {
                LocalDate temp = start;
                while (temp.isBefore(end)) {
                    PagoContrato mensualidad = new PagoContrato();
                    mensualidad.setContrato(contrato);
                    mensualidad.setMonto(monto);
                    mensualidad.setMoneda(moneda);
                    mensualidad.setTipoPago(TipoPago.MENSUALIDAD);
                    mensualidad.setEstadoPago(EstadoPago.PENDIENTE);
                    mensualidad.setMetodoPago(MetodoPago.EFECTIVO);
                    mensualidad.setFechaVencimiento(temp);
                    pagoContratoRepository.save(mensualidad);

                    temp = temp.plusMonths(1);
                }
            }

        } else if (contrato.getTipoContrato() == TipoContrato.ANTICRETICO) {
            PagoContrato deposito = new PagoContrato();
            deposito.setContrato(contrato);
            deposito.setMonto(monto);
            deposito.setMoneda(moneda);
            deposito.setTipoPago(TipoPago.DEPOSITO_ANTICRETICO);
            deposito.setEstadoPago(EstadoPago.PENDIENTE);
            deposito.setMetodoPago(MetodoPago.TRANSFERENCIA_BANCARIA);
            deposito.setFechaVencimiento(start != null ? start : LocalDate.now());
            pagoContratoRepository.save(deposito);

            PagoContrato devolucion = new PagoContrato();
            devolucion.setContrato(contrato);
            devolucion.setMonto(monto);
            devolucion.setMoneda(moneda);
            devolucion.setTipoPago(TipoPago.DEVOLUCION_ANTICRETICO);
            devolucion.setEstadoPago(EstadoPago.PENDIENTE);
            devolucion.setMetodoPago(MetodoPago.TRANSFERENCIA_BANCARIA);
            devolucion.setFechaVencimiento(end != null ? end : LocalDate.now().plusYears(1));
            pagoContratoRepository.save(devolucion);

        } else if (contrato.getTipoContrato() == TipoContrato.VENTA) {
            if (especificaciones != null && especificaciones.containsKey("plan_pagos")) {
                List<Map<String, Object>> plan = (List<Map<String, Object>>) especificaciones.get("plan_pagos");
                for (Map<String, Object> cuotaMap : plan) {
                    BigDecimal cuotaMonto = new BigDecimal(cuotaMap.get("monto").toString());
                    LocalDate vcto = LocalDate.parse(cuotaMap.get("fecha_vencimiento").toString());

                    PagoContrato cuota = new PagoContrato();
                    cuota.setContrato(contrato);
                    cuota.setMonto(cuotaMonto);
                    cuota.setMoneda(moneda);
                    cuota.setTipoPago(TipoPago.CUOTA_VENTA);
                    cuota.setEstadoPago(EstadoPago.PENDIENTE);
                    cuota.setMetodoPago(MetodoPago.TRANSFERENCIA_BANCARIA);
                    cuota.setFechaVencimiento(vcto);
                    pagoContratoRepository.save(cuota);
                }
            } else {
                PagoContrato cuotaUnica = new PagoContrato();
                cuotaUnica.setContrato(contrato);
                cuotaUnica.setMonto(monto);
                cuotaUnica.setMoneda(moneda);
                cuotaUnica.setTipoPago(TipoPago.CUOTA_VENTA);
                cuotaUnica.setEstadoPago(EstadoPago.PENDIENTE);
                cuotaUnica.setMetodoPago(MetodoPago.TRANSFERENCIA_BANCARIA);
                cuotaUnica.setFechaVencimiento(start != null ? start : LocalDate.now());
                pagoContratoRepository.save(cuotaUnica);
            }
        }
    }

    private ContratoResponseDTO mapToResponse(Contrato c) {
        ContratoResponseDTO dto = new ContratoResponseDTO();
        dto.setId(c.getId());
        dto.setCodigo("CTR-" + c.getTipoContrato().name().substring(0, 3) + "-" + c.getId().toString().substring(0, 8).toUpperCase());
        dto.setIdInmueble(c.getInmueble().getId());
        dto.setInmuebleTitulo(c.getPublicacion() != null ? c.getPublicacion().getTitulo() : "Propiedad " + c.getInmueble().getTipoInmueble());
        dto.setIdPublicacion(c.getPublicacion() != null ? c.getPublicacion().getId() : null);
        dto.setIdPropietario(c.getPropietario().getId());
        
        String propNombre = perfilRepository.findByUsuarioId(c.getPropietario().getId())
                .map(p -> p.getNombre() + " " + (p.getApellido() != null ? p.getApellido() : ""))
                .orElse("Propietario");
        dto.setPropietarioNombre(propNombre.trim());
        
        dto.setIdCliente(c.getCliente().getId());
        
        String cliNombre = perfilRepository.findByUsuarioId(c.getCliente().getId())
                .map(p -> p.getNombre() + " " + (p.getApellido() != null ? p.getApellido() : ""))
                .orElse("Cliente");
        dto.setClienteNombre(cliNombre.trim());

        dto.setTipoContrato(c.getTipoContrato());
        dto.setEstadoContrato(c.getEstadoContrato());
        dto.setMontoAcordado(c.getMontoAcordado());
        dto.setMonto(c.getMontoAcordado());
        dto.setMoneda(c.getMoneda());
        dto.setFechaInicio(c.getFechaInicio());
        dto.setFechaFin(c.getFechaFin());
        dto.setCantidadHuespedes(c.getCantidadHuespedes());
        dto.setNoches(c.getNoches());
        dto.setDocumentoUrl(c.getDocumentoUrl());
        dto.setObservacion(c.getObservacion());
        dto.setEspecificaciones(c.getEspecificaciones());
        dto.setCreatedDate(c.getCreatedDate());
        dto.setCreatedAt(c.getCreatedDate());
        dto.setTransactionHash(c.getTransactionHash());
        return dto;
    }

    @Transactional
    public void eliminarContrato(UUID contratoId) {
        Contrato contrato = contratoRepository.findById(contratoId)
                .orElseThrow(() -> new IllegalArgumentException("Contrato no encontrado"));
        if (contrato.getEstadoContrato() == EstadoContrato.VIGENTE) {
            throw new IllegalStateException("No se puede eliminar un contrato vigente. Debe ser cancelado.");
        }
        List<PagoContrato> pagos = pagoContratoRepository.findByContratoIdOrderByFechaVencimientoAsc(contratoId);
        pagoContratoRepository.deleteAll(pagos);
        contratoRepository.delete(contrato);
    }

    @Transactional
    public ContratoResponseDTO cancelarContrato(UUID contratoId) {
        Contrato contrato = contratoRepository.findById(contratoId)
                .orElseThrow(() -> new IllegalArgumentException("Contrato no encontrado"));
        if (contrato.getEstadoContrato() != EstadoContrato.VIGENTE) {
            throw new IllegalStateException("Solo se puede cancelar un contrato vigente.");
        }
        contrato.setEstadoContrato(EstadoContrato.CANCELADO);
        contrato = contratoRepository.save(contrato);
        return mapToResponse(contrato);
    }
}
