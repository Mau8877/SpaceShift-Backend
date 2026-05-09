ALTER TABLE usuario
ADD COLUMN codigo_recuperacion VARCHAR(6),
ADD COLUMN expiracion_codigo_recuperacion TIMESTAMP;