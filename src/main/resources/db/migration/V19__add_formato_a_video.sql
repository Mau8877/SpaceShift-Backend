-- Formato de salida del modelo 3D elegido al generar el recorrido (SPLAT o SOG).
-- Nullable: los videos existentes se tratan como SOG (endpoint de Runpod por defecto).
ALTER TABLE video_publicacion
ADD COLUMN formato VARCHAR(20);
