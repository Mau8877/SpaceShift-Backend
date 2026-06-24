package com.sw.api.modules.iot.service;

import com.sw.api.modules.contrato.model.Contrato;
import com.sw.api.modules.inmueble.model.Inmueble;
import com.sw.api.modules.iot.model.DeviceViolation;
import com.sw.api.modules.iot.model.PlugAssignment;
import com.sw.api.modules.iot.model.PlugPowerReading;
import com.sw.api.modules.iot.model.PlugUsageSession;
import com.sw.api.modules.iot.model.SmartPlug;
import com.sw.api.modules.iot.model.TipoIncumplimiento;
import com.sw.api.modules.iot.repository.DeviceViolationRepository;
import com.sw.api.modules.iot.repository.PlugAssignmentRepository;
import com.sw.api.modules.iot.repository.PlugPowerReadingRepository;
import com.sw.api.modules.iot.repository.PlugUsageSessionRepository;
import com.sw.api.modules.iot.tuya.TuyaApiClient;
import com.sw.api.modules.iot.tuya.dto.TuyaDevice;
import com.sw.api.modules.iot.tuya.dto.TuyaStatus;
import com.sw.api.modules.notificacion.service.NotificacionService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Cada 5 minutos, sondea Tuya por cada enchufe con asignación activa: registra el
 * consumo (para la gráfica), detecta desconexiones sospechosas, y corta la energía si
 * el dispositivo asignado tiene un contrato vigente y excede horarioLimiteUso o
 * maxHorasSeguidas. Reactivo por diseño: el inquilino enciende/apaga directamente
 * desde Smart Life, este scheduler solo puede apagar, nunca encender.
 */
@Component
public class DeviceEnforcementScheduler {

    private final PlugAssignmentRepository plugAssignmentRepository;
    private final PlugPowerReadingRepository plugPowerReadingRepository;
    private final PlugUsageSessionRepository plugUsageSessionRepository;
    private final DeviceViolationRepository deviceViolationRepository;
    private final DispositivoLookup dispositivoLookup;
    private final ContratoVigenciaChecker contratoVigenciaChecker;
    private final TuyaApiClient tuyaApiClient;
    private final NotificacionService notificacionService;

    @Value("${tuya.enforcement.min-power-threshold:5}")
    private int minPowerThreshold;

    public DeviceEnforcementScheduler(PlugAssignmentRepository plugAssignmentRepository,
            PlugPowerReadingRepository plugPowerReadingRepository,
            PlugUsageSessionRepository plugUsageSessionRepository,
            DeviceViolationRepository deviceViolationRepository, DispositivoLookup dispositivoLookup,
            ContratoVigenciaChecker contratoVigenciaChecker, TuyaApiClient tuyaApiClient,
            NotificacionService notificacionService) {
        this.plugAssignmentRepository = plugAssignmentRepository;
        this.plugPowerReadingRepository = plugPowerReadingRepository;
        this.plugUsageSessionRepository = plugUsageSessionRepository;
        this.deviceViolationRepository = deviceViolationRepository;
        this.dispositivoLookup = dispositivoLookup;
        this.contratoVigenciaChecker = contratoVigenciaChecker;
        this.tuyaApiClient = tuyaApiClient;
        this.notificacionService = notificacionService;
    }

    @Scheduled(fixedDelay = 300_000)
    public void revisarEnchufesAsignados() {
        for (PlugAssignment asignacion : plugAssignmentRepository.findAllByUnassignedAtIsNull()) {
            try {
                revisarAsignacion(asignacion);
            } catch (Exception ex) {
                System.err.println("DeviceEnforcementScheduler: error revisando enchufe "
                        + asignacion.getSmartPlug().getTuyaDeviceId() + ": " + ex.getMessage());
            }
        }
    }

    @Transactional
    void revisarAsignacion(PlugAssignment asignacion) {
        SmartPlug plug = asignacion.getSmartPlug();
        Inmueble inmueble = asignacion.getInmueble();
        String dispositivoId = asignacion.getDispositivoId();

        TuyaDevice detalle;
        List<TuyaStatus> status;
        try {
            detalle = tuyaApiClient.getDeviceDetail(plug.getTuyaDeviceId());
            status = tuyaApiClient.getDeviceStatus(plug.getTuyaDeviceId());
        } catch (Exception ex) {
            System.err.println("DeviceEnforcementScheduler: no se pudo consultar Tuya para "
                    + plug.getTuyaDeviceId() + ": " + ex.getMessage());
            return;
        }

        boolean online = detalle.online();
        Integer curPower = leerCurPower(status);

        Optional<PlugPowerReading> ultimaLectura = plugPowerReadingRepository
                .findFirstBySmartPlug_IdOrderByRecordedAtDesc(plug.getId());

        PlugPowerReading lectura = new PlugPowerReading();
        lectura.setSmartPlug(plug);
        lectura.setRecordedAt(LocalDateTime.now());
        lectura.setCurPower(curPower);
        lectura.setOnline(online);
        plugPowerReadingRepository.save(lectura);

        if (ultimaLectura.isPresent() && ultimaLectura.get().isOnline() && !online) {
            contratoVigenciaChecker.contratoVigentePara(inmueble.getId(), dispositivoId)
                    .ifPresent(contrato -> registrarDesconexionSospechosa(plug, inmueble, dispositivoId, contrato));
        }

        DispositivoLookup.CondicionesUso condiciones = dispositivoLookup.condiciones(inmueble, dispositivoId);
        if (condiciones.horarioLimiteUso() == null && condiciones.maxHorasSeguidas() == null) {
            return;
        }

        Optional<Contrato> contratoVigente = contratoVigenciaChecker.contratoVigentePara(inmueble.getId(),
                dispositivoId);
        if (contratoVigente.isEmpty()) {
            return;
        }

        boolean enUso = curPower != null ? curPower > minPowerThreshold : leerSwitchOn(status);
        Optional<PlugUsageSession> sesionAbierta = plugUsageSessionRepository
                .findFirstBySmartPlug_IdAndEndedAtIsNull(plug.getId());

        boolean cortadoPorHorasContinuas = false;

        if (enUso) {
            if (sesionAbierta.isEmpty()) {
                PlugUsageSession sesion = new PlugUsageSession();
                sesion.setSmartPlug(plug);
                sesion.setStartedAt(LocalDateTime.now());
                plugUsageSessionRepository.save(sesion);
            } else if (condiciones.maxHorasSeguidas() != null) {
                LocalDateTime limite = sesionAbierta.get().getStartedAt().plusHours(condiciones.maxHorasSeguidas());
                if (LocalDateTime.now().isAfter(limite)) {
                    cortarPorIncumplimiento(plug, inmueble, dispositivoId, contratoVigente.get(), sesionAbierta.get(),
                            TipoIncumplimiento.HORAS_CONTINUAS_EXCEDIDAS,
                            "Se superaron las " + condiciones.maxHorasSeguidas() + " horas continuas de uso permitidas.");
                    cortadoPorHorasContinuas = true;
                }
            }
        } else if (sesionAbierta.isPresent()) {
            sesionAbierta.get().setEndedAt(LocalDateTime.now());
            plugUsageSessionRepository.save(sesionAbierta.get());
        }

        if (!cortadoPorHorasContinuas && enUso && condiciones.horarioLimiteUso() != null) {
            LocalTime limiteHora = parseHoraOLimiteNull(condiciones.horarioLimiteUso());
            if (limiteHora != null && LocalTime.now().isAfter(limiteHora)) {
                cortarPorIncumplimiento(plug, inmueble, dispositivoId, contratoVigente.get(), sesionAbierta.orElse(null),
                        TipoIncumplimiento.HORARIO_LIMITE_EXCEDIDO,
                        "El dispositivo seguía en uso después del horario límite (" + condiciones.horarioLimiteUso()
                                + ").");
            }
        }
    }

    private void cortarPorIncumplimiento(SmartPlug plug, Inmueble inmueble, String dispositivoId, Contrato contrato,
            PlugUsageSession sesionAbierta, TipoIncumplimiento tipo, String detalle) {
        tuyaApiClient.sendCommand(plug.getTuyaDeviceId(), false);

        if (sesionAbierta != null) {
            sesionAbierta.setEndedAt(LocalDateTime.now());
            plugUsageSessionRepository.save(sesionAbierta);
        }

        DeviceViolation violacion = new DeviceViolation();
        violacion.setSmartPlug(plug);
        violacion.setInmueble(inmueble);
        violacion.setDispositivoId(dispositivoId);
        violacion.setTipo(tipo);
        violacion.setDetectedAt(LocalDateTime.now());
        violacion.setDetalle(detalle);
        deviceViolationRepository.save(violacion);

        String dispositivoNombre = dispositivoLookup.nombreOFallback(inmueble, dispositivoId);
        notificacionService.enviarNotificacion(contrato.getCliente().getId(), "Dispositivo apagado automáticamente",
                "\"" + dispositivoNombre + "\" fue apagado automáticamente. " + detalle,
                Map.of("tipo", "DEVICE_VIOLATION", "violationType", tipo.name(), "smartPlugId", plug.getId().toString()));
    }

    private void registrarDesconexionSospechosa(SmartPlug plug, Inmueble inmueble, String dispositivoId,
            Contrato contrato) {
        DeviceViolation violacion = new DeviceViolation();
        violacion.setSmartPlug(plug);
        violacion.setInmueble(inmueble);
        violacion.setDispositivoId(dispositivoId);
        violacion.setTipo(TipoIncumplimiento.DESCONEXION_SOSPECHOSA);
        violacion.setDetectedAt(LocalDateTime.now());
        violacion.setDetalle("El enchufe se desconectó de la red mientras el contrato estaba vigente.");
        deviceViolationRepository.save(violacion);

        String dispositivoNombre = dispositivoLookup.nombreOFallback(inmueble, dispositivoId);
        notificacionService.enviarNotificacion(contrato.getCliente().getId(), "Desconexión sospechosa detectada",
                "El enchufe inteligente de \"" + dispositivoNombre + "\" se desconectó. Si esto no fue intencional,"
                        + " revisa su conexión a WiFi.",
                Map.of("tipo", "DEVICE_VIOLATION", "violationType", TipoIncumplimiento.DESCONEXION_SOSPECHOSA.name(),
                        "smartPlugId", plug.getId().toString()));
    }

    private Integer leerCurPower(List<TuyaStatus> status) {
        return status.stream()
                .filter(s -> "cur_power".equals(s.code()))
                .map(s -> s.value() instanceof Number numero ? numero.intValue() : null)
                .filter(java.util.Objects::nonNull)
                .findFirst()
                .orElse(null);
    }

    private boolean leerSwitchOn(List<TuyaStatus> status) {
        return status.stream()
                .filter(s -> "switch_1".equals(s.code()))
                .map(s -> Boolean.TRUE.equals(s.value()))
                .findFirst()
                .orElse(false);
    }

    private LocalTime parseHoraOLimiteNull(String horarioLimiteUso) {
        try {
            return LocalTime.parse(horarioLimiteUso);
        } catch (Exception ex) {
            return null;
        }
    }
}
