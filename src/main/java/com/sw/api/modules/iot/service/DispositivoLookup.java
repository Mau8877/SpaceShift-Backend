package com.sw.api.modules.iot.service;

import com.sw.api.modules.inmueble.model.Inmueble;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Optional;

/**
 * Busca un dispositivo dentro del JSONB {@code Inmueble.dispositivos} por su id.
 * Compartido entre SmartPlugService e InstallationTicketService para no duplicar
 * la lógica de lectura de ese catálogo.
 */
@Component
public class DispositivoLookup {

    public Optional<Map<String, Object>> buscar(Inmueble inmueble, String dispositivoId) {
        if (inmueble == null || inmueble.getDispositivos() == null) {
            return Optional.empty();
        }
        return inmueble.getDispositivos().stream()
                .filter(d -> dispositivoId.equals(String.valueOf(d.get("id"))))
                .findFirst();
    }

    public String nombreOFallback(Inmueble inmueble, String dispositivoId) {
        return buscar(inmueble, dispositivoId)
                .map(d -> String.valueOf(d.get("nombre")))
                .orElse("Dispositivo eliminado");
    }

    public record CondicionesUso(String horarioLimiteUso, Integer maxHorasSeguidas) {
    }

    /**
     * Lee horarioLimiteUso/maxHorasSeguidas del JSONB. Tolera que maxHorasSeguidas
     * venga como Number o String (el wizard del frontend lo guarda como número, pero
     * el JSONB no impone tipo), y trata 0/blank/ausente como "sin esa condición".
     */
    public CondicionesUso condiciones(Inmueble inmueble, String dispositivoId) {
        Map<String, Object> dispositivo = buscar(inmueble, dispositivoId).orElse(Map.of());

        Object horarioRaw = dispositivo.get("horarioLimiteUso");
        String horarioLimiteUso = horarioRaw != null && !String.valueOf(horarioRaw).isBlank()
                ? String.valueOf(horarioRaw)
                : null;

        Integer maxHorasSeguidas = parseHoras(dispositivo.get("maxHorasSeguidas"));

        return new CondicionesUso(horarioLimiteUso, maxHorasSeguidas);
    }

    private Integer parseHoras(Object raw) {
        if (raw == null) {
            return null;
        }
        try {
            int valor = raw instanceof Number numero ? numero.intValue() : Integer.parseInt(String.valueOf(raw).trim());
            return valor > 0 ? valor : null;
        } catch (NumberFormatException ex) {
            return null;
        }
    }
}
