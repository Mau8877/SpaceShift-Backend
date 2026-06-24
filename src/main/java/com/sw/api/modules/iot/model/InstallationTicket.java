package com.sw.api.modules.iot.model;

import com.sw.api.modules.inmueble.model.Inmueble;
import com.sw.api.shared.model.Auditable;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@Entity
@Table(name = "installation_ticket")
public class InstallationTicket extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne
    @JoinColumn(name = "id_inmueble", nullable = false)
    private Inmueble inmueble;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private InstallationTicketStatus status = InstallationTicketStatus.PENDING;

    @Column(name = "requested_at", nullable = false)
    private LocalDateTime requestedAt;

    @Column(name = "scheduled_at")
    private LocalDateTime scheduledAt;
}
