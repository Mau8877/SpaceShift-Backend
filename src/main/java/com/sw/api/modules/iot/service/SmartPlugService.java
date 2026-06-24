package com.sw.api.modules.iot.service;

import com.sw.api.modules.iot.dto.PlugTestResultDTO;
import com.sw.api.modules.iot.dto.SmartPlugCreateRequestDTO;
import com.sw.api.modules.iot.dto.SmartPlugDTO;
import com.sw.api.modules.iot.dto.TuyaDeviceScanResultDTO;
import com.sw.api.modules.iot.model.Appliance;
import com.sw.api.modules.iot.model.PlugAssignment;
import com.sw.api.modules.iot.model.PlugStatus;
import com.sw.api.modules.iot.model.SmartPlug;
import com.sw.api.modules.iot.repository.ApplianceRepository;
import com.sw.api.modules.iot.repository.PlugAssignmentRepository;
import com.sw.api.modules.iot.repository.SmartPlugRepository;
import com.sw.api.modules.iot.tuya.TuyaApiClient;
import com.sw.api.modules.iot.tuya.dto.TuyaDevice;
import com.sw.api.modules.iot.tuya.dto.TuyaStatus;
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
    private final ApplianceRepository applianceRepository;
    private final PlugAssignmentRepository plugAssignmentRepository;
    private final TuyaApiClient tuyaApiClient;

    public SmartPlugService(SmartPlugRepository smartPlugRepository, ApplianceRepository applianceRepository,
            PlugAssignmentRepository plugAssignmentRepository, TuyaApiClient tuyaApiClient) {
        this.smartPlugRepository = smartPlugRepository;
        this.applianceRepository = applianceRepository;
        this.plugAssignmentRepository = plugAssignmentRepository;
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

        List<TuyaStatus> statusList = tuyaApiClient.getDeviceStatus(plug.getTuyaDeviceId());
        boolean online = statusList.stream().anyMatch(s -> "switch_1".equals(s.code()));
        if (!online) {
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
    public SmartPlugDTO assignPlug(UUID plugId, UUID applianceId) {
        SmartPlug plug = obtenerEntidadPorId(plugId);
        Appliance appliance = applianceRepository.findById(applianceId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Appliance no encontrado"));

        PlugAssignment assignment = new PlugAssignment();
        assignment.setSmartPlug(plug);
        assignment.setAppliance(appliance);
        assignment.setAssignedAt(LocalDateTime.now());
        plugAssignmentRepository.save(assignment);

        plug.setStatus(PlugStatus.ASSIGNED);
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
                        a.getAppliance().getId(),
                        a.getAppliance().getName(),
                        describeProperty(a.getAppliance()),
                        a.getAssignedAt()))
                .orElse(null);

        return new SmartPlugDTO(plug.getId(), plug.getTuyaDeviceId(), plug.getAlias(), plug.getStatus(),
                plug.getNotes(), current);
    }

    private String describeProperty(Appliance appliance) {
        var inmueble = appliance.getInmueble();
        if (inmueble == null) {
            return "—";
        }
        String zona = inmueble.getUbicacion() != null ? inmueble.getUbicacion().getZonaBarrios() : null;
        return zona != null ? inmueble.getTipoInmueble() + " · " + zona : inmueble.getTipoInmueble();
    }
}
