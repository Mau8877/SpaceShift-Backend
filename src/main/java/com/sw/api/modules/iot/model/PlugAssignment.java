package com.sw.api.modules.iot.model;

import com.sw.api.shared.model.Auditable;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
@Table(name = "plug_assignment")
public class PlugAssignment extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne
    @JoinColumn(name = "id_smart_plug", nullable = false)
    private SmartPlug smartPlug;

    @ManyToOne
    @JoinColumn(name = "id_appliance", nullable = false)
    private Appliance appliance;

    @Column(name = "assigned_at", nullable = false)
    private LocalDateTime assignedAt;

    @Column(name = "unassigned_at")
    private LocalDateTime unassignedAt;
}
