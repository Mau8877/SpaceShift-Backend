-- Agregar columnas para soporte de dispositivos inteligentes y condiciones a la tabla de inmuebles
ALTER TABLE inmueble ADD COLUMN IF NOT EXISTS dispositivos JSONB;
ALTER TABLE inmueble ADD COLUMN IF NOT EXISTS condiciones TEXT;
ALTER TABLE inmueble ADD COLUMN IF NOT EXISTS multas_sanciones TEXT;
