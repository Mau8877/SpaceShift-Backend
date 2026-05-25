-- 1. Agregar la columna de créditos al Perfil del usuario (Inicia en 1000)
ALTER TABLE perfil ADD COLUMN saldo_creditos INTEGER DEFAULT 1000;

-- 2. Crear la tabla de transacciones de créditos
CREATE TABLE transaccion_credito (
    id UUID PRIMARY KEY,
    usuario_id UUID NOT NULL,
    cantidad INTEGER NOT NULL,
    tipo VARCHAR(50) NOT NULL, -- 'REGISTRO_INICIAL', 'CONSUMO_PROCESAMIENTO', 'REEMBOLSO'
    descripcion VARCHAR(255),
    id_publicacion UUID,
    
    -- Campos de auditoría
    created_by UUID,
    created_date TIMESTAMP NOT NULL DEFAULT NOW(),
    last_modified_by UUID,
    last_modified_date TIMESTAMP,
    deleted BOOLEAN DEFAULT FALSE,
    
    CONSTRAINT fk_transaccion_usuario FOREIGN KEY (usuario_id) REFERENCES usuario(id) ON DELETE CASCADE
);

-- 3. Crear la tabla de videos de la publicación (Símil a imagen_publicacion)
CREATE TABLE video_publicacion (
    id UUID PRIMARY KEY,
    id_publicacion UUID NOT NULL,
    url_video VARCHAR(500) NOT NULL,
    url_modelo_3d VARCHAR(500),
    duracion_segundos INTEGER NOT NULL,
    creditos_consumidos INTEGER NOT NULL,
    estado_procesamiento VARCHAR(50) NOT NULL DEFAULT 'PENDIENTE', -- 'PENDIENTE', 'PROCESANDO', 'COMPLETADO', 'FALLIDO'
    nombre_archivo VARCHAR(255) NOT NULL,
    tamano_bytes BIGINT NOT NULL,
    error_mensaje TEXT,
    
    -- Campos de auditoría
    created_by UUID,
    created_date TIMESTAMP NOT NULL DEFAULT NOW(),
    last_modified_by UUID,
    last_modified_date TIMESTAMP,
    deleted BOOLEAN DEFAULT FALSE,
    
    CONSTRAINT fk_video_publicacion FOREIGN KEY (id_publicacion) REFERENCES publicacion(id) ON DELETE CASCADE
);
