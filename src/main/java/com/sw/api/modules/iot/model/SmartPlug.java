package com.sw.api.modules.iot.model;

import com.sw.api.shared.model.Auditable;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
@Entity
@Table(name = "smart_plug")
public class SmartPlug extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "tuya_device_id", nullable = false, unique = true, length = 100)
    private String tuyaDeviceId;

    @Column(nullable = false, length = 100)
    private String alias;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PlugStatus status = PlugStatus.AVAILABLE;

    @Column(columnDefinition = "TEXT")
    private String notes;
}
