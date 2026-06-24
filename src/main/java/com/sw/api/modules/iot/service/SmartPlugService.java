package com.sw.api.modules.iot.service;

import com.sw.api.modules.inmueble.model.Inmueble;
import com.sw.api.modules.inmueble.repository.InmuebleRepository;
import com.sw.api.modules.iot.dto.DeviceViolationDTO;
import com.sw.api.modules.iot.dto.PlugPowerReadingDTO;
import com.sw.api.modules.iot.dto.PlugTestResultDTO;
import com.sw.api.modules.iot.dto.SmartPlugCreateRequestDTO;
import com.sw.api.modules.iot.dto.SmartPlugDTO;
import com.sw.api.modules.iot.dto.TuyaDeviceScanResultDTO;
import com.sw.api.modules.iot.model.InstallationTicketStatus;
import com.sw.api.modules.iot.model.PlugAssignment;
import com.sw.api.modules.iot.model.PlugStatus;
import com.sw.api.modules.iot.model.SmartPlug;
import com.sw.api.modules.iot.repository.DeviceViolationRepository;
import com.sw.api.modules.iot.repository.InstallationTicketRepository;
import com.sw.api.modules.iot.repository.PlugAssignmentRepository;
import com.sw.api.modules.iot.repository.PlugPowerReadingRepository;
import com.sw.api.modules.iot.repository.SmartPlugRepository;
import com.sw.api.modules.iot.tuya.TuyaApiClient;
import com.sw.api.modules.iot.tuya.dto.TuyaDevice;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class SmartPlugService {

    private final SmartPlugRepository smartPlugRepository;
    private final InmuebleRepository inmuebleRepository;
    private final PlugAssignmentRepository plugAssignmentRepository;
    private final InstallationTicketRepository installationTicketRepository;
    private final PlugPowerReadingRepository plugPowerReadingRepository;
    private final DeviceViolationRepository deviceViolationRepository;
    private final DispositivoLookup dispositivoLookup;
    private final TuyaApiClient tuyaApiClient;

    public SmartPlugService(SmartPlugRepository smartPlugRepository, InmuebleRepository inmuebleRepository,
            PlugAssignmentRepository plugAssignmentRepository,
            InstallationTicketRepository installationTicketRepository,
            PlugPowerReadingRepository plugPowerReadingRepository,
            DeviceViolationRepository deviceViolationRepository, DispositivoLookup dispositivoLookup,
            TuyaApiClient tuyaApiClient) {
        this.smartPlugRepository = smartPlugRepository;
        this.inmuebleRepository = inmuebleRepository;
        this.plugAssignmentRepository = plugAssignmentRepository;
        this.installationTicketRepository = installationTicketRepository;
        this.plugPowerReadingRepository = plugPowerReadingRepository;
        this.deviceViolationRepository = deviceViolationRepository;
        this.dispositivoLookup = dispositivoLookup;
        this.tuyaApiClient = tuyaApiClient;
    }

    @Transactional(readOnly = true)
    public List<TuyaDeviceScanResultDTO> scanTuyaDevices() {
        List<TuyaDevice> devices = tuyaApiClient.getDeviceList();
        Set<String> registered = smartPlugRepository
                .findAllByTuyaDeviceIdIn(devices.stream().map(TuyaDevice::id).toList())
                .stream()
                .map(SmartPlug::getTuyaDeviceId)
                .collect(Collectors.toSet());

        return devices.stream()
                .map(d -> new TuyaDeviceScanResultDTO(d.id(), d.name(), d.online(), registered.contains(d.id())))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<SmartPlugDTO> getPlugs() {
        return smartPlugRepository.findAll().stream().map(this::mapToDTO).toList();
    }

    @Transactional
    public SmartPlugDTO verifyAndRegisterPlug(SmartPlugCreateRequestDTO request) {
        if (smartPlugRepository.findByTuyaDeviceId(request.tuyaDeviceId()).isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Este enchufe ya está registrado en el sistema");
        }

        // Verifica que el dispositivo exista realmente en la cuenta Tuya antes de guardarlo
        tuyaApiClient.getDeviceDetail(request.tuyaDeviceId());

        SmartPlug plug = new SmartPlug();
        plug.setTuyaDeviceId(request.tuyaDeviceId());
        plug.setAlias(request.alias());
        plug.setNotes(request.notes());
        plug.setStatus(PlugStatus.AVAILABLE);

        return mapToDTO(smartPlugRepository.save(plug));
    }

    @Transactional
    public PlugTestResultDTO testPlugConnection(UUID plugId) {
        SmartPlug plug = obtenerEntidadPorId(plugId);

        // El campo "online" del detalle del dispositivo refleja la conexión real con Tuya.
        // El endpoint de status puede devolver dp's con valores cacheados aunque el equipo
        // esté desconectado, así que no sirve para decidir si está online.
        TuyaDevice device = tuyaApiClient.getDeviceDetail(plug.getTuyaDeviceId());
        if (!device.online()) {
            return new PlugTestResultDTO(false, false,
                    "El enchufe está fuera de línea. Verifica que esté conectado al WiFi.");
        }

        try {
            tuyaApiClient.sendCommand(plug.getTuyaDeviceId(), true);
            sleepQuietly();
            tuyaApiClient.sendCommand(plug.getTuyaDeviceId(), false);
            return new PlugTestResultDTO(true, true, "El enchufe respondió correctamente.");
        } catch (ResponseStatusException ex) {
            return new PlugTestResultDTO(true, false, "El enchufe no respondió al comando de encendido/apagado.");
        }
    }

    @Transactional
    public void sendCommand(UUID plugId, boolean on) {
        SmartPlug plug = obtenerEntidadPorId(plugId);
        if (plug.getStatus() != PlugStatus.ASSIGNED) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "El enchufe no tiene una asignación activa");
        }
        tuyaApiClient.sendCommand(plug.getTuyaDeviceId(), on);
    }

    @Transactional
    public SmartPlugDTO assignPlug(UUID plugId, UUID inmuebleId, String dispositivoId) {
        SmartPlug plug = obtenerEntidadPorId(plugId);
        Inmueble inmueble = inmuebleRepository.findById(inmuebleId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Inmueble no encontrado"));
        dispositivoLookup.buscar(inmueble, dispositivoId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "El dispositivo no existe en este inmueble"));

        PlugAssignment assignment = new PlugAssignment();
        assignment.setSmartPlug(plug);
        assignment.setInmueble(inmueble);
        assignment.setDispositivoId(dispositivoId);
        assignment.setAssignedAt(LocalDateTime.now());
        plugAssignmentRepository.save(assignment);

        plug.setStatus(PlugStatus.ASSIGNED);

        // El técnico ya instaló y probó el enchufe físicamente; esta acción explícita
        // es la única señal real de que la instalación quedó completa, así que cierra
        // el ticket correspondiente si todavía estaba abierto.
        installationTicketRepository
                .findFirstByInmueble_IdAndDispositivoIdAndStatusIn(inmuebleId, dispositivoId,
                        List.of(InstallationTicketStatus.PENDING, InstallationTicketStatus.SCHEDULED,
                                InstallationTicketStatus.IN_PROGRESS))
                .ifPresent(ticket -> {
                    ticket.setStatus(InstallationTicketStatus.CLOSED);
                    installationTicketRepository.save(ticket);
                });

        return mapToDTO(smartPlugRepository.save(plug));
    }

    @Transactional
    public SmartPlugDTO unassignPlug(UUID plugId) {
        SmartPlug plug = obtenerEntidadPorId(plugId);

        plugAssignmentRepository.findFirstBySmartPlug_IdAndUnassignedAtIsNull(plugId)
                .ifPresent(a -> {
                    a.setUnassignedAt(LocalDateTime.now());
                    plugAssignmentRepository.save(a);
                });

        plug.setStatus(PlugStatus.AVAILABLE);
        return mapToDTO(smartPlugRepository.save(plug));
    }

    @Transactional(readOnly = true)
    public List<PlugPowerReadingDTO> getPowerReadings(UUID plugId, int hours) {
        SmartPlug plug = obtenerEntidadPorId(plugId);
        LocalDateTime desde = LocalDateTime.now().minusHours(hours);
        return plugPowerReadingRepository
                .findAllBySmartPlug_IdAndRecordedAtAfterOrderByRecordedAtAsc(plug.getId(), desde)
                .stream()
                .map(r -> new PlugPowerReadingDTO(r.getRecordedAt(), r.getCurPower(), r.isOnline()))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<DeviceViolationDTO> getViolations(UUID plugId) {
        SmartPlug plug = obtenerEntidadPorId(plugId);
        return deviceViolationRepository.findAllBySmartPlug_IdOrderByDetectedAtDesc(plug.getId())
                .stream()
                .map(v -> new DeviceViolationDTO(v.getId(), v.getTipo(), v.getDetectedAt(), v.getDetalle()))
                .toList();
    }

    private SmartPlug obtenerEntidadPorId(UUID id) {
        return smartPlugRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Enchufe no encontrado"));
    }

    private void sleepQuietly() {
        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private SmartPlugDTO mapToDTO(SmartPlug plug) {
        SmartPlugDTO.CurrentAssignmentDTO current = plugAssignmentRepository
                .findFirstBySmartPlug_IdAndUnassignedAtIsNull(plug.getId())
                .map(a -> new SmartPlugDTO.CurrentAssignmentDTO(
                        a.getDispositivoId(),
                        dispositivoLookup.nombreOFallback(a.getInmueble(), a.getDispositivoId()),
                        describeProperty(a.getInmueble()),
                        a.getAssignedAt()))
                .orElse(null);

        return new SmartPlugDTO(plug.getId(), plug.getTuyaDeviceId(), plug.getAlias(), plug.getStatus(),
                plug.getNotes(), current);
    }

    private String describeProperty(Inmueble inmueble) {
        if (inmueble == null) {
            return "—";
        }
        String zona = inmueble.getUbicacion() != null ? inmueble.getUbicacion().getZonaBarrios() : null;
        return zona != null ? inmueble.getTipoInmueble() + " · " + zona : inmueble.getTipoInmueble();
    }
}
