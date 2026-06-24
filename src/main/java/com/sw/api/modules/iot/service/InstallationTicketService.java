package com.sw.api.modules.iot.service;

import com.sw.api.modules.inmueble.model.Inmueble;
import com.sw.api.modules.inmueble.repository.InmuebleRepository;
import com.sw.api.modules.iot.dto.InstallationTicketDTO;
import com.sw.api.modules.iot.dto.UpdateTicketStatusRequestDTO;
import com.sw.api.modules.iot.model.InstallationTicket;
import com.sw.api.modules.iot.model.InstallationTicketStatus;
import com.sw.api.modules.iot.repository.InstallationTicketRepository;
import com.sw.api.modules.notificacion.service.NotificacionService;
import com.sw.api.modules.publicacion.model.Publicacion;
import com.sw.api.modules.publicacion.repository.PublicacionRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class InstallationTicketService {

    private static final List<InstallationTicketStatus> ESTADOS_ABIERTOS = List.of(
            InstallationTicketStatus.PENDING, InstallationTicketStatus.SCHEDULED,
            InstallationTicketStatus.IN_PROGRESS);

    private static final DateTimeFormatter FORMATO_FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private final InstallationTicketRepository repository;
    private final InmuebleRepository inmuebleRepository;
    private final PublicacionRepository publicacionRepository;
    private final DispositivoLookup dispositivoLookup;
    private final NotificacionService notificacionService;

    public InstallationTicketService(InstallationTicketRepository repository, InmuebleRepository inmuebleRepository,
            PublicacionRepository publicacionRepository, DispositivoLookup dispositivoLookup,
            NotificacionService notificacionService) {
        this.repository = repository;
        this.inmuebleRepository = inmuebleRepository;
        this.publicacionRepository = publicacionRepository;
        this.dispositivoLookup = dispositivoLookup;
        this.notificacionService = notificacionService;
    }

    @Transactional(readOnly = true)
    public List<InstallationTicketDTO> getPendingTickets() {
        return repository.findAllByStatusIn(ESTADOS_ABIERTOS).stream().map(this::mapToDTO).toList();
    }

    @Transactional
    public InstallationTicketDTO updateStatus(UUID ticketId, UpdateTicketStatusRequestDTO request) {
        InstallationTicket ticket = repository.findById(ticketId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Ticket no encontrado"));

        ticket.setStatus(request.status());
        if (request.scheduledAt() != null) {
            ticket.setScheduledAt(request.scheduledAt());
        }

        InstallationTicket guardado = repository.save(ticket);

        if (request.status() == InstallationTicketStatus.SCHEDULED && request.scheduledAt() != null) {
            notificarVisitaProgramada(guardado);
        }

        return mapToDTO(guardado);
    }

    private void notificarVisitaProgramada(InstallationTicket ticket) {
        publicacionRepository.findFirstByInmueble_IdOrderByFechaPublicacionDesc(ticket.getInmueble().getId())
                .map(Publicacion::getUsuario)
                .ifPresent(propietario -> {
                    String dispositivoNombre = dispositivoLookup.nombreOFallback(ticket.getInmueble(),
                            ticket.getDispositivoId());
                    String fecha = ticket.getScheduledAt().format(FORMATO_FECHA);
                    notificacionService.enviarNotificacion(propietario.getId(),
                            "Visita de instalación programada",
                            "Se programó la instalación del enchufe inteligente para \"" + dispositivoNombre
                                    + "\" el " + fecha + ".",
                            Map.of("tipo", "INSTALACION_PROGRAMADA", "ticketId", ticket.getId().toString()));
                });
    }

    /**
     * Crea un InstallationTicket PENDING por cada dispositivo del inmueble que todavía
     * no tenga ningún ticket (en cualquier estado). Idempotente: se puede llamar tantas
     * veces como se publique/edite el inmueble sin generar duplicados.
     */
    @Transactional
    public void sincronizarTicketsDeInmueble(UUID inmuebleId) {
        Inmueble inmueble = inmuebleRepository.findById(inmuebleId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Inmueble no encontrado"));

        if (inmueble.getDispositivos() == null) {
            return;
        }

        for (Map<String, Object> dispositivo : inmueble.getDispositivos()) {
            String dispositivoId = String.valueOf(dispositivo.get("id"));
            if (repository.existsByInmueble_IdAndDispositivoId(inmuebleId, dispositivoId)) {
                continue;
            }

            InstallationTicket ticket = new InstallationTicket();
            ticket.setInmueble(inmueble);
            ticket.setDispositivoId(dispositivoId);
            ticket.setStatus(InstallationTicketStatus.PENDING);
            ticket.setRequestedAt(LocalDateTime.now());
            repository.save(ticket);
        }
    }

    private InstallationTicketDTO mapToDTO(InstallationTicket ticket) {
        Inmueble inmueble = ticket.getInmueble();
        String propertyName = inmueble.getUbicacion() != null && inmueble.getUbicacion().getZonaBarrios() != null
                ? inmueble.getTipoInmueble() + " · " + inmueble.getUbicacion().getZonaBarrios()
                : inmueble.getTipoInmueble();

        String dispositivoNombre = dispositivoLookup.nombreOFallback(inmueble, ticket.getDispositivoId());
        UUID publicacionId = publicacionRepository
                .findFirstByInmueble_IdOrderByFechaPublicacionDesc(inmueble.getId())
                .map(p -> p.getId())
                .orElse(null);

        return new InstallationTicketDTO(ticket.getId(), inmueble.getId(), propertyName, ticket.getDispositivoId(),
                dispositivoNombre, publicacionId, ticket.getStatus(), ticket.getRequestedAt(),
                ticket.getScheduledAt());
    }
}
