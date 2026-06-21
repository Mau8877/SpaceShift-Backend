-- Usuario que paga/procesa el video (para reembolsos correctos) y URL del modelo .sog
ALTER TABLE video_publicacion
ADD COLUMN id_usuario UUID,
ADD COLUMN url_sog VARCHAR(500);

ALTER TABLE video_publicacion
ADD CONSTRAINT fk_video_usuario FOREIGN KEY (id_usuario) REFERENCES usuario(id) ON DELETE SET NULL;
