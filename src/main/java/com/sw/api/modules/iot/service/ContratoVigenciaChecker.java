package com.sw.api.modules.iot.service;

import com.sw.api.modules.contrato.model.Contrato;
import com.sw.api.modules.contrato.model.EstadoContrato;
import com.sw.api.modules.contrato.repository.ContratoRepository;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Decide si un dispositivo de un inmueble está cubierto hoy por un contrato VIGENTE,
 * que es la condición para que el enforcement automático (cortes por horario/horas
 * continuas) aplique. Replica el mismo fallback de claves que ya usa ContratoService
 * al leer Contrato.especificaciones ("dispositivosContrato" con fallback a
 * "dispositivos_alquilados").
 */
@Component
public class ContratoVigenciaChecker {

    private final ContratoRepository contratoRepository;

    public ContratoVigenciaChecker(ContratoRepository contratoRepository) {
        this.contratoRepository = contratoRepository;
    }

    public Optional<Contrato> contratoVigentePara(UUID inmuebleId, String dispositivoId) {
        LocalDate hoy = LocalDate.now();

        return contratoRepository.findByInmueble_IdAndEstadoContrato(inmuebleId, EstadoContrato.VIGENTE).stream()
                .filter(c -> c.getFechaInicio() != null && c.getFechaFin() != null)
                .filter(c -> !hoy.isBefore(c.getFechaInicio()) && !hoy.isAfter(c.getFechaFin()))
                .filter(c -> cubreDispositivo(c, dispositivoId))
                .findFirst();
    }

    @SuppressWarnings("unchecked")
    private boolean cubreDispositivo(Contrato contrato, String dispositivoId) {
        Map<String, Object> specs = contrato.getEspecificaciones();
        if (specs == null) {
            return false;
        }

        Object raw = specs.getOrDefault("dispositivosContrato", specs.get("dispositivos_alquilados"));
        if (!(raw instanceof List<?> dispositivos)) {
            return false;
        }

        return dispositivos.stream()
                .filter(Map.class::isInstance)
                .map(d -> (Map<String, Object>) d)
                .anyMatch(d -> dispositivoId.equals(String.valueOf(d.get("id"))));
    }
}
