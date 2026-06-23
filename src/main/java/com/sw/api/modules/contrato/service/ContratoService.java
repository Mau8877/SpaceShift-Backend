package com.sw.api.modules.contrato.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.sw.api.modules.contrato.dto.ContratoRequestDTO;
import com.sw.api.modules.contrato.dto.ContratoResponseDTO;
import com.sw.api.modules.contrato.dto.DashboardClientResponseDTO;
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
import com.sw.api.modules.usuario.model.Perfil;
import com.sw.api.modules.blockchain.service.Web3Service;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
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
    private final ObjectMapper objectMapper;

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
    public ContratoResponseDTO firmarContrato(UUID contratoId, com.sw.api.modules.contrato.dto.FirmaContratoRequestDTO dto, UUID usuarioId) {
        Contrato contrato = contratoRepository.findById(contratoId)
                .orElseThrow(() -> new IllegalArgumentException("Contrato no encontrado"));

        if (contrato.getEstadoContrato() != EstadoContrato.PENDIENTE_FIRMA) {
            throw new IllegalStateException("El contrato no está en estado de firma pendiente");
        }

        // Validar que se haya realizado algún pago ( Stripe, Efectivo, etc. )
        boolean tienePagoCompletado = pagoContratoRepository.findByContratoIdOrderByFechaVencimientoAsc(contratoId)
                .stream()
                .anyMatch(pago -> pago.getEstadoPago() == EstadoPago.COMPLETADO);

        if (!tienePagoCompletado) {
            throw new IllegalStateException("Debes realizar el pago correspondiente antes de firmar el contrato.");
        }

        // Si viene el DTO con dispositivos seleccionados
        if (dto != null) {
            if (dto.getDispositivosAlquilados() != null) {
                if (contrato.getEspecificaciones() == null) {
                    contrato.setEspecificaciones(new HashMap<>());
                }
                // Guardar la lista de dispositivos seleccionados en especificaciones
                contrato.getEspecificaciones().put("dispositivos_alquilados", dto.getDispositivosAlquilados());
                
                // Calcular y guardar el precio total de dispositivos en especificaciones
                BigDecimal totalDispositivos = BigDecimal.ZERO;
                for (Map<String, Object> disp : dto.getDispositivosAlquilados()) {
                    try {
                        Object precioObj = disp.get("precio");
                        Object diasObj = disp.get("diasUso");
                        if (precioObj != null && diasObj != null) {
                            BigDecimal precio = new BigDecimal(precioObj.toString());
                            BigDecimal dias = new BigDecimal(diasObj.toString());
                            totalDispositivos = totalDispositivos.add(precio.multiply(dias));
                        }
                    } catch (Exception ex) {
                        // ignore error
                    }
                }
                contrato.getEspecificaciones().put("precio_dispositivos_total", totalDispositivos);
            }
            if (dto.getMontoAcordado() != null && dto.getMontoAcordado().compareTo(BigDecimal.ZERO) > 0) {
                contrato.setMontoAcordado(dto.getMontoAcordado());
                
                // Borrar cobros viejos de mensualidades / cuotas del plan de pagos provisional
                List<PagoContrato> pagosViejos = pagoContratoRepository.findByContratoIdOrderByFechaVencimientoAsc(contrato.getId());
                pagoContratoRepository.deleteAll(pagosViejos);
                
                // Regenerar plan de pagos con el nuevo monto total
                generarPlanDePagos(contrato, contrato.getEspecificaciones());
            }
        }

        boolean isCliente = usuarioId.equals(contrato.getCliente().getId());
        boolean isPropietario = usuarioId.equals(contrato.getPropietario().getId());

        if (!isCliente && !isPropietario) {
            throw new IllegalArgumentException("Usuario no autorizado para firmar este contrato.");
        }

        if (contrato.getEspecificaciones() == null) {
            contrato.setEspecificaciones(new HashMap<>());
        }

        boolean shouldActivate = false;

        if (contrato.getTipoContrato() == TipoContrato.VENTA) {
            if (isCliente) {
                contrato.getEspecificaciones().put("firmaCliente", true);
            }
            if (isPropietario) {
                contrato.getEspecificaciones().put("firmaPropietario", true);
            }

            boolean clienteFirmado = Boolean.TRUE.equals(contrato.getEspecificaciones().get("firmaCliente"));
            boolean propietarioFirmado = Boolean.TRUE.equals(contrato.getEspecificaciones().get("firmaPropietario"));

            if (clienteFirmado && propietarioFirmado) {
                shouldActivate = true;
            } else {
                contrato = contratoRepository.save(contrato);
                return mapToResponse(contrato);
            }
        } else {
            if (!isCliente) {
                throw new IllegalStateException("Solo el cliente puede firmar digitalmente este tipo de contrato.");
            }
            contrato.getEspecificaciones().put("firmaCliente", true);
            shouldActivate = true;
        }

        if (shouldActivate) {
            contrato.setEstadoContrato(EstadoContrato.VIGENTE);
        }

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
                if (contrato.getEspecificaciones() == null) {
                    contrato.setEspecificaciones(new HashMap<>());
                }
                Map<String, Object> blockchainSnapshot = buildBlockchainSnapshot(contrato);
                String contractContentHash = calculateSha256(canonicalJson(blockchainSnapshot));
                contrato.getEspecificaciones().put("contenidoContratoHash", contractContentHash);

                String conditions = getSpecAsString(contrato.getEspecificaciones(), "reglasContrato");
                String penalties = getSpecAsString(contrato.getEspecificaciones(), "sancionesContrato");
                String rentedDevices = getDevicesForBlockchain(contrato.getEspecificaciones());
                BigDecimal devicePriceVal = calculateContractDevicesTotal(contrato.getEspecificaciones());
                long rentalDays = contrato.getNoches() != null ? contrato.getNoches().longValue() : 0;
                BigInteger devicePriceWei = devicePriceVal.multiply(BigDecimal.valueOf(100)).toBigInteger(); // cents precision

                String txHash = web3Service.registerPropertyContractOnChain(
                        contrato.getId().toString(),
                        finalWallet,
                        conditions,
                        penalties,
                        rentedDevices,
                        devicePriceWei,
                        BigInteger.valueOf(rentalDays),
                        contractContentHash
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

    private Map<String, Object> buildBlockchainSnapshot(Contrato contrato) {
        Map<String, Object> specs = contrato.getEspecificaciones() != null ? contrato.getEspecificaciones() : Map.of();
        Map<String, Object> snapshot = new TreeMap<>();
        snapshot.put("contratoId", contrato.getId().toString());
        snapshot.put("inmuebleId", contrato.getInmueble().getId().toString());
        snapshot.put("publicacionId", contrato.getPublicacion() != null ? contrato.getPublicacion().getId().toString() : null);
        snapshot.put("propietarioId", contrato.getPropietario().getId().toString());
        snapshot.put("clienteId", contrato.getCliente().getId().toString());
        snapshot.put("tipoContrato", contrato.getTipoContrato().name());
        snapshot.put("montoAcordado", contrato.getMontoAcordado());
        snapshot.put("moneda", contrato.getMoneda());
        snapshot.put("fechaInicio", contrato.getFechaInicio());
        snapshot.put("fechaFin", contrato.getFechaFin());
        snapshot.put("reglasContrato", specs.getOrDefault("reglasContrato", ""));
        snapshot.put("sancionesContrato", specs.getOrDefault("sancionesContrato", ""));
        snapshot.put("dispositivosContrato", specs.getOrDefault("dispositivosContrato", specs.getOrDefault("dispositivos_alquilados", List.of())));
        return snapshot;
    }

    private String getSpecAsString(Map<String, Object> specs, String key) {
        Object value = specs != null ? specs.get(key) : null;
        return value != null ? value.toString() : "";
    }

    private String getDevicesForBlockchain(Map<String, Object> specs) throws JsonProcessingException {
        if (specs == null) return "";
        Object devices = specs.getOrDefault("dispositivosContrato", specs.get("dispositivos_alquilados"));
        return devices != null ? canonicalJson(devices) : "";
    }

    @SuppressWarnings("unchecked")
    private BigDecimal calculateContractDevicesTotal(Map<String, Object> specs) {
        if (specs == null) return BigDecimal.ZERO;

        Object devicesObj = specs.get("dispositivosContrato");
        if (devicesObj instanceof List<?> devices) {
            return devices.stream()
                    .filter(Map.class::isInstance)
                    .map(device -> calculateNewDeviceTotal((Map<String, Object>) device))
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
        }

        Object legacyTotal = specs.get("precio_dispositivos_total");
        if (legacyTotal != null) {
            try {
                return new BigDecimal(legacyTotal.toString());
            } catch (NumberFormatException ignored) {
                return BigDecimal.ZERO;
            }
        }

        return BigDecimal.ZERO;
    }

    private BigDecimal calculateNewDeviceTotal(Map<String, Object> device) {
        BigDecimal price = toBigDecimal(device.get("precioContrato"));
        BigDecimal quantity = toBigDecimal(device.getOrDefault("cantidad", 1));
        return price.multiply(quantity);
    }

    private BigDecimal toBigDecimal(Object value) {
        if (value == null) return BigDecimal.ZERO;
        try {
            return new BigDecimal(value.toString());
        } catch (NumberFormatException ex) {
            return BigDecimal.ZERO;
        }
    }

    private String canonicalJson(Object value) throws JsonProcessingException {
        ObjectMapper canonicalMapper = objectMapper.copy()
                .configure(MapperFeature.SORT_PROPERTIES_ALPHABETICALLY, true)
                .configure(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS, true);
        return canonicalMapper.writeValueAsString(value);
    }

    private String calculateSha256(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(value.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder(hash.length * 2);
            for (byte b : hash) {
                hex.append(String.format("%02x", b));
            }
            return hex.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 no disponible", e);
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
        dto.setDispositivosInmueble(c.getInmueble().getDispositivos());
        dto.setCondicionesInmueble(c.getInmueble().getCondiciones());
        dto.setMultasSancionesInmueble(c.getInmueble().getMultasSanciones());
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

    public List<DashboardClientResponseDTO> obtenerClientesDePropietario(UUID propietarioId) {
        List<Contrato> contratos = contratoRepository.findByPropietarioIdOrderByCreatedDateDesc(propietarioId);
        
        java.time.format.DateTimeFormatter formatter = java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
        
        return contratos.stream().map(c -> {
            String clientName = "Cliente";
            String clientEmail = c.getCliente().getCorreo();
            String clientPhone = "";

            Perfil perfilCliente = perfilRepository.findByUsuarioId(c.getCliente().getId()).orElse(null);
            if (perfilCliente != null) {
                clientName = perfilCliente.getNombre() + " " + (perfilCliente.getApellido() != null ? perfilCliente.getApellido() : "");
                clientName = clientName.trim();
                clientPhone = perfilCliente.getTelefono() != null ? perfilCliente.getTelefono() : "";
            }

            String codigoContrato = "CTR-" + c.getTipoContrato().name().substring(0, 3) + "-" + c.getId().toString().substring(0, 8).toUpperCase();
            
            LocalDateTime lastAct = c.getLastModifiedDate() != null ? c.getLastModifiedDate() : c.getCreatedDate();
            String ultimaAct = lastAct != null ? lastAct.format(formatter) : "";

            boolean contratoPorVencer = false;
            if (c.getEstadoContrato() == EstadoContrato.VIGENTE && c.getFechaFin() != null) {
                long days = ChronoUnit.DAYS.between(LocalDate.now(), c.getFechaFin());
                if (days >= 0 && days <= 30) {
                    contratoPorVencer = true;
                }
            }

            DashboardClientResponseDTO dto = DashboardClientResponseDTO.builder()
                    .id(c.getCliente().getId().toString())
                    .nombre(clientName)
                    .correo(clientEmail)
                    .telefono(clientPhone)
                    .tipoCliente(mapTipoCliente(c.getTipoContrato()))
                    .estado(mapEstadoCliente(c.getEstadoContrato()))
                    .inmueble(c.getPublicacion() != null ? c.getPublicacion().getTitulo() : "Propiedad " + c.getInmueble().getTipoInmueble())
                    .contrato(codigoContrato)
                    .tipoContrato(c.getTipoContrato().name())
                    .fechaInicio(c.getFechaInicio() != null ? c.getFechaInicio().toString() : "")
                    .fechaFin(c.getFechaFin() != null ? c.getFechaFin().toString() : "")
                    .ultimaActividad(ultimaAct)
                    .moneda(c.getMoneda())
                    .montoContrato(c.getMontoAcordado())
                    .contratoPorVencer(contratoPorVencer)
                    .build();
            return dto;
        }).collect(Collectors.toList());
    }

    private String mapTipoCliente(TipoContrato tipo) {
        if (tipo == TipoContrato.ALQUILER) return "INQUILINO";
        if (tipo == TipoContrato.VENTA) return "COMPRADOR";
        if (tipo == TipoContrato.ALOJAMIENTO) return "HUESPED";
        if (tipo == TipoContrato.ANTICRETICO) return "ANTICRESISTA";
        return "INQUILINO";
    }

    private String mapEstadoCliente(EstadoContrato estado) {
        if (estado == EstadoContrato.VIGENTE) return "ACTIVO";
        if (estado == EstadoContrato.PENDIENTE_FIRMA) return "PENDIENTE";
        return "HISTORICO";
    }
}
