package com.sw.api.modules.iot.service;

import com.sw.api.modules.iot.dto.InstallationTicketDTO;
import com.sw.api.modules.iot.dto.UpdateTicketStatusRequestDTO;
import com.sw.api.modules.iot.model.InstallationTicket;
import com.sw.api.modules.iot.model.InstallationTicketStatus;
import com.sw.api.modules.iot.repository.InstallationTicketRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.UUID;

@Service
public class InstallationTicketService {

    private final InstallationTicketRepository repository;

    public InstallationTicketService(InstallationTicketRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public java.util.List<InstallationTicketDTO> getPendingTickets() {
        return repository.findAllByStatus(InstallationTicketStatus.PENDING).stream().map(this::mapToDTO).toList();
    }

    @Transactional
    public InstallationTicketDTO updateStatus(UUID ticketId, UpdateTicketStatusRequestDTO request) {
        InstallationTicket ticket = repository.findById(ticketId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Ticket no encontrado"));

        ticket.setStatus(request.status());
        if (request.scheduledAt() != null) {
            ticket.setScheduledAt(request.scheduledAt());
        }

        return mapToDTO(repository.save(ticket));
    }

    private InstallationTicketDTO mapToDTO(InstallationTicket ticket) {
        var inmueble = ticket.getInmueble();
        String propertyName = inmueble.getUbicacion() != null && inmueble.getUbicacion().getZonaBarrios() != null
                ? inmueble.getTipoInmueble() + " · " + inmueble.getUbicacion().getZonaBarrios()
                : inmueble.getTipoInmueble();

        return new InstallationTicketDTO(ticket.getId(), inmueble.getId(), propertyName, ticket.getStatus(),
                ticket.getRequestedAt(), ticket.getScheduledAt());
    }
}
