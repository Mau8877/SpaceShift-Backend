package com.sw.api.modules.iot.controller;

import com.sw.api.modules.iot.dto.AssignPlugRequestDTO;
import com.sw.api.modules.iot.dto.PlugCommandRequestDTO;
import com.sw.api.modules.iot.dto.PlugTestResultDTO;
import com.sw.api.modules.iot.dto.SmartPlugCreateRequestDTO;
import com.sw.api.modules.iot.dto.SmartPlugDTO;
import com.sw.api.modules.iot.dto.TuyaDeviceScanResultDTO;
import com.sw.api.modules.iot.service.SmartPlugService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/iot/plugs")
public class SmartPlugController {

    private final SmartPlugService smartPlugService;

    public SmartPlugController(SmartPlugService smartPlugService) {
        this.smartPlugService = smartPlugService;
    }

    @GetMapping("/scan")
    public ResponseEntity<List<TuyaDeviceScanResultDTO>> scan() {
        return ResponseEntity.ok(smartPlugService.scanTuyaDevices());
    }

    @GetMapping
    public ResponseEntity<List<SmartPlugDTO>> obtenerTodos() {
        return ResponseEntity.ok(smartPlugService.getPlugs());
    }

    @PostMapping("/verify-register")
    public ResponseEntity<SmartPlugDTO> verificarYRegistrar(@RequestBody SmartPlugCreateRequestDTO request) {
        return new ResponseEntity<>(smartPlugService.verifyAndRegisterPlug(request), HttpStatus.CREATED);
    }

    @PostMapping("/{id}/test")
    public ResponseEntity<PlugTestResultDTO> test(@PathVariable UUID id) {
        return ResponseEntity.ok(smartPlugService.testPlugConnection(id));
    }

    @PostMapping("/{id}/command")
    public ResponseEntity<Void> enviarComando(@PathVariable UUID id, @RequestBody PlugCommandRequestDTO request) {
        smartPlugService.sendCommand(id, request.isOn());
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{id}/assign")
    public ResponseEntity<SmartPlugDTO> asignar(@PathVariable UUID id, @RequestBody AssignPlugRequestDTO request) {
        return ResponseEntity.ok(smartPlugService.assignPlug(id, request.applianceId()));
    }

    @PostMapping("/{id}/unassign")
    public ResponseEntity<SmartPlugDTO> desasignar(@PathVariable UUID id) {
        return ResponseEntity.ok(smartPlugService.unassignPlug(id));
    }
}
