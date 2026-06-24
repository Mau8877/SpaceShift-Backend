package com.sw.api.modules.iot.controller;

import com.sw.api.modules.iot.dto.InstallationTicketDTO;
import com.sw.api.modules.iot.dto.UpdateTicketStatusRequestDTO;
import com.sw.api.modules.iot.service.InstallationTicketService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/iot/tickets")
public class InstallationTicketController {

    private final InstallationTicketService installationTicketService;

    public InstallationTicketController(InstallationTicketService installationTicketService) {
        this.installationTicketService = installationTicketService;
    }

    @GetMapping("/pending")
    public ResponseEntity<List<InstallationTicketDTO>> pendientes() {
        return ResponseEntity.ok(installationTicketService.getPendingTickets());
    }

    @PutMapping("/{id}/status")
    public ResponseEntity<InstallationTicketDTO> actualizarEstado(@PathVariable UUID id,
            @RequestBody UpdateTicketStatusRequestDTO request) {
        return ResponseEntity.ok(installationTicketService.updateStatus(id, request));
    }

    @PostMapping("/sync-inmueble/{inmuebleId}")
    public ResponseEntity<Void> sincronizar(@PathVariable UUID inmuebleId) {
        installationTicketService.sincronizarTicketsDeInmueble(inmuebleId);
        return ResponseEntity.noContent().build();
    }
}
