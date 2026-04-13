-- 1. Tabla conversacion
CREATE TABLE conversacion (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    propiedad_id UUID NOT NULL,
    creado_en TIMESTAMP NOT NULL DEFAULT NOW(),
    actualizado_en TIMESTAMP NOT NULL DEFAULT NOW(),
    
    CONSTRAINT fk_conversacion_inmueble FOREIGN KEY (propiedad_id) REFERENCES inmueble (id)
);

-- 2. Tabla participante_conversacion
-- No se puede auto referenciar a mensaje para ultimo_mensaje_leido_id aún, se hará después o se deja sin FK restrictiva.
CREATE TABLE participante_conversacion (
    conversacion_id UUID NOT NULL,
    usuario_id UUID NOT NULL,
    ultimo_mensaje_leido_id UUID,
    rol VARCHAR(50) NOT NULL,
    
    PRIMARY KEY (conversacion_id, usuario_id),
    CONSTRAINT fk_part_conv_conversacion FOREIGN KEY (conversacion_id) REFERENCES conversacion (id),
    CONSTRAINT fk_part_conv_usuario FOREIGN KEY (usuario_id) REFERENCES usuario (id)
);

-- 3. Tabla mensaje
CREATE TABLE mensaje (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    conversacion_id UUID NOT NULL,
    remitente_id UUID NOT NULL,
    contenido TEXT NOT NULL,
    estado VARCHAR(20) NOT NULL DEFAULT 'ENVIADO',
    creado_en TIMESTAMP NOT NULL DEFAULT NOW(),
    
    CONSTRAINT fk_mensaje_conversacion FOREIGN KEY (conversacion_id) REFERENCES conversacion (id),
    CONSTRAINT fk_mensaje_usuario FOREIGN KEY (remitente_id) REFERENCES usuario (id)
);

-- Agregar la FK de ultimo_mensaje_leido_id en participante_conversacion ahora que la tabla mensaje existe
ALTER TABLE participante_conversacion 
ADD CONSTRAINT fk_part_conv_mensaje 
FOREIGN KEY (ultimo_mensaje_leido_id) REFERENCES mensaje (id);

-- 4. Tabla token_dispositivo
CREATE TABLE token_dispositivo (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    usuario_id UUID NOT NULL,
    token_fcm VARCHAR(255) NOT NULL UNIQUE,
    plataforma VARCHAR(20) NOT NULL,
    ultimo_uso TIMESTAMP NOT NULL DEFAULT NOW(),
    
    CONSTRAINT fk_token_usuario FOREIGN KEY (usuario_id) REFERENCES usuario (id)
);
