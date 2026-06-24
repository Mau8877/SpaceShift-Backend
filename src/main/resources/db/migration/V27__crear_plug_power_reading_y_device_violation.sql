CREATE TABLE IF NOT EXISTS plug_power_reading (
    id UUID PRIMARY KEY,
    id_smart_plug UUID NOT NULL,
    recorded_at TIMESTAMP NOT NULL,
    cur_power INTEGER,
    online BOOLEAN NOT NULL,

    created_by UUID,
    created_date TIMESTAMP NOT NULL DEFAULT NOW(),
    last_modified_by UUID,
    last_modified_date TIMESTAMP,
    deleted BOOLEAN DEFAULT FALSE,

    CONSTRAINT fk_power_reading_smart_plug FOREIGN KEY (id_smart_plug) REFERENCES smart_plug(id)
);
CREATE INDEX IF NOT EXISTS idx_power_reading_plug_fecha ON plug_power_reading(id_smart_plug, recorded_at);

CREATE TABLE IF NOT EXISTS device_violation (
    id UUID PRIMARY KEY,
    id_smart_plug UUID NOT NULL,
    id_inmueble UUID NOT NULL,
    dispositivo_id VARCHAR(64) NOT NULL,
    tipo VARCHAR(30) NOT NULL,
    detected_at TIMESTAMP NOT NULL,
    detalle TEXT,

    created_by UUID,
    created_date TIMESTAMP NOT NULL DEFAULT NOW(),
    last_modified_by UUID,
    last_modified_date TIMESTAMP,
    deleted BOOLEAN DEFAULT FALSE,

    CONSTRAINT fk_device_violation_smart_plug FOREIGN KEY (id_smart_plug) REFERENCES smart_plug(id),
    CONSTRAINT fk_device_violation_inmueble FOREIGN KEY (id_inmueble) REFERENCES inmueble(id)
);
CREATE INDEX IF NOT EXISTS idx_device_violation_plug ON device_violation(id_smart_plug, detected_at);
