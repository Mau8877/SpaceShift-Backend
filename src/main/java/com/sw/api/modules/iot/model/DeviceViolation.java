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
@Table(name = "device_violation")
public class DeviceViolation extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne
    @JoinColumn(name = "id_smart_plug", nullable = false)
    private SmartPlug smartPlug;

    @ManyToOne
    @JoinColumn(name = "id_inmueble", nullable = false)
    private Inmueble inmueble;

    @Column(name = "dispositivo_id", nullable = false, length = 64)
    private String dispositivoId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private TipoIncumplimiento tipo;

    @Column(name = "detected_at", nullable = false)
    private LocalDateTime detectedAt;

    @Column(columnDefinition = "TEXT")
    private String detalle;
}
