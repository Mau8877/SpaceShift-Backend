-- 1. Tabla de Paquetes de Créditos Administrables
CREATE TABLE paquete_credito (
    id UUID PRIMARY KEY,
    nombre_paquete VARCHAR(100) NOT NULL,
    precio DECIMAL(10, 2) NOT NULL,
    descripcion VARCHAR(255),
    creditos_paquetes INTEGER NOT NULL,
    
    -- Auditoría y Borrado Lógico (Soft Delete)
    created_by UUID,
    created_date TIMESTAMP NOT NULL DEFAULT NOW(),
    last_modified_by UUID,
    last_modified_date TIMESTAMP,
    deleted BOOLEAN DEFAULT FALSE
);

-- 2. Tabla de Registro Monetario de Stripe (Enlazada al paquete comprado)
CREATE TABLE pago_stripe (
    id UUID PRIMARY KEY,
    usuario_id UUID NOT NULL,
    paquete_credito_id UUID NOT NULL,
    transaccion_credito_id UUID,
    
    -- Identificadores únicos de Stripe (Idempotencia de pagos)
    stripe_session_id VARCHAR(255) UNIQUE NOT NULL,
    stripe_payment_intent_id VARCHAR(255) UNIQUE,
    
    -- Datos contables consolidados de la transacción
    monto DECIMAL(10, 2) NOT NULL,
    moneda VARCHAR(10) NOT NULL DEFAULT 'BOB', -- Bolivia Bolivianos
    estado_pago VARCHAR(50) NOT NULL DEFAULT 'PENDIENTE', -- 'PENDIENTE', 'COMPLETADO', 'FALLIDO'
    
    -- Auditoría
    created_by UUID,
    created_date TIMESTAMP NOT NULL DEFAULT NOW(),
    last_modified_by UUID,
    last_modified_date TIMESTAMP,
    deleted BOOLEAN DEFAULT FALSE,
    
    CONSTRAINT fk_pago_usuario FOREIGN KEY (usuario_id) REFERENCES usuario(id) ON DELETE CASCADE,
    CONSTRAINT fk_pago_paquete FOREIGN KEY (paquete_credito_id) REFERENCES paquete_credito(id),
    CONSTRAINT fk_pago_credito FOREIGN KEY (transaccion_credito_id) REFERENCES transaccion_credito(id) ON DELETE SET NULL
);

-- 3. Vincular transaccion_credito con el pago origen
ALTER TABLE transaccion_credito ADD COLUMN pago_stripe_id UUID;
ALTER TABLE transaccion_credito ADD CONSTRAINT fk_transaccion_pago FOREIGN KEY (pago_stripe_id) REFERENCES pago_stripe(id) ON DELETE SET NULL;

-- 4. Insertar paquetes de créditos por defecto (en Bolivianos - BOB)
INSERT INTO paquete_credito (id, nombre_paquete, precio, descripcion, creditos_paquetes, deleted) VALUES
('b2cf4ef7-b248-43d2-bf8f-8d2698944510', 'Paquete Bronce', 35.00, 'Obtén 500 SST de créditos para procesamiento 3D básico.', 500, false),
('c8cf4ef7-c248-43d2-bf8f-8d2698944511', 'Paquete Plata (Popular)', 70.00, 'Obtén 1,200 SST de créditos (incluye 200 de bono de bienvenida).', 1200, false),
('d2cf4ef7-d248-43d2-bf8f-8d2698944512', 'Paquete Oro (El mejor valor)', 140.00, 'Obtén 3,000 SST de créditos (incluye 1,000 de bono gratuito!).', 3000, false);
