-- 1. Modificar tabla contrato para añadir el campo de especificaciones JSONB
ALTER TABLE contrato ADD COLUMN IF NOT EXISTS especificaciones JSONB;

-- 2. Crear la tabla de pagos de contrato
CREATE TABLE IF NOT EXISTS pago_contrato (
    id UUID PRIMARY KEY,
    id_contrato UUID NOT NULL,
    monto DECIMAL(12,2) NOT NULL,
    moneda VARCHAR(10) NOT NULL,
    tipo_pago VARCHAR(50) NOT NULL, -- 'MENSUALIDAD', 'GARANTIA', 'CUOTA_VENTA', 'DEPOSITO_ANTICRETICO', 'DEVOLUCION_ANTICRETICO'
    estado_pago VARCHAR(50) NOT NULL, -- 'PENDIENTE', 'COMPLETADO', 'ATRASADO', 'REEMBOLSADO'
    metodo_pago VARCHAR(50) NOT NULL, -- 'STRIPE', 'EFECTIVO', 'TRANSFERENCIA_BANCARIA'
    fecha_vencimiento DATE NOT NULL,
    fecha_pago TIMESTAMP,
    documento_comprobante_url VARCHAR(500),
    stripe_pago_id VARCHAR(255),
    
    -- Auditoría
    created_by UUID,
    created_date TIMESTAMP NOT NULL DEFAULT NOW(),
    last_modified_by UUID,
    last_modified_date TIMESTAMP,
    deleted BOOLEAN DEFAULT FALSE,

    CONSTRAINT fk_pago_contrato FOREIGN KEY (id_contrato) REFERENCES contrato(id) ON DELETE CASCADE
);

-- Indexar para optimizar las consultas de pagos de un contrato
CREATE INDEX IF NOT EXISTS idx_pago_contrato_id ON pago_contrato(id_contrato);
