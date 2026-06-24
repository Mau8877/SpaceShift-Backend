-- Módulo IoT: enchufes inteligentes (Tuya), electrodomésticos y tickets de instalación
CREATE TABLE IF NOT EXISTS smart_plug (
    id UUID PRIMARY KEY,
    tuya_device_id VARCHAR(100) NOT NULL UNIQUE,
    alias VARCHAR(100) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'AVAILABLE',
    notes TEXT,

    created_by UUID,
    created_date TIMESTAMP NOT NULL DEFAULT NOW(),
    last_modified_by UUID,
    last_modified_date TIMESTAMP,
    deleted BOOLEAN DEFAULT FALSE
);

CREATE TABLE IF NOT EXISTS appliance (
    id UUID PRIMARY KEY,
    id_inmueble UUID NOT NULL,
    name VARCHAR(100) NOT NULL,
    condition VARCHAR(20) NOT NULL,

    created_by UUID,
    created_date TIMESTAMP NOT NULL DEFAULT NOW(),
    last_modified_by UUID,
    last_modified_date TIMESTAMP,
    deleted BOOLEAN DEFAULT FALSE,

    CONSTRAINT fk_appliance_inmueble FOREIGN KEY (id_inmueble) REFERENCES inmueble(id)
);

CREATE TABLE IF NOT EXISTS plug_assignment (
    id UUID PRIMARY KEY,
    id_smart_plug UUID NOT NULL,
    id_appliance UUID NOT NULL,
    assigned_at TIMESTAMP NOT NULL,
    unassigned_at TIMESTAMP,

    created_by UUID,
    created_date TIMESTAMP NOT NULL DEFAULT NOW(),
    last_modified_by UUID,
    last_modified_date TIMESTAMP,
    deleted BOOLEAN DEFAULT FALSE,

    CONSTRAINT fk_plug_assignment_smart_plug FOREIGN KEY (id_smart_plug) REFERENCES smart_plug(id),
    CONSTRAINT fk_plug_assignment_appliance FOREIGN KEY (id_appliance) REFERENCES appliance(id)
);

CREATE TABLE IF NOT EXISTS installation_ticket (
    id UUID PRIMARY KEY,
    id_inmueble UUID NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    requested_at TIMESTAMP NOT NULL,
    scheduled_at TIMESTAMP,

    created_by UUID,
    created_date TIMESTAMP NOT NULL DEFAULT NOW(),
    last_modified_by UUID,
    last_modified_date TIMESTAMP,
    deleted BOOLEAN DEFAULT FALSE,

    CONSTRAINT fk_installation_ticket_inmueble FOREIGN KEY (id_inmueble) REFERENCES inmueble(id)
);

CREATE INDEX IF NOT EXISTS idx_plug_assignment_smart_plug ON plug_assignment(id_smart_plug);
CREATE INDEX IF NOT EXISTS idx_plug_assignment_appliance ON plug_assignment(id_appliance);
CREATE INDEX IF NOT EXISTS idx_installation_ticket_inmueble ON installation_ticket(id_inmueble);
CREATE INDEX IF NOT EXISTS idx_installation_ticket_status ON installation_ticket(status);
