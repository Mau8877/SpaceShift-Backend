CREATE TABLE favorito (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    id_usuario UUID NOT NULL,
    id_publicacion UUID NOT NULL,
    fecha_agregado TIMESTAMP NOT NULL,
    
    -- Campos de auditoría (Auditable)
    created_by UUID,
    created_date TIMESTAMP NOT NULL DEFAULT NOW(),
    last_modified_by UUID,
    last_modified_date TIMESTAMP,
    deleted BOOLEAN DEFAULT FALSE,

    CONSTRAINT fk_favorito_usuario FOREIGN KEY (id_usuario) REFERENCES usuario (id) ON DELETE CASCADE,
    CONSTRAINT fk_favorito_publicacion FOREIGN KEY (id_publicacion) REFERENCES publicacion (id) ON DELETE CASCADE
);
