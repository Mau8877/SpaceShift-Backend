ALTER TABLE installation_ticket ADD COLUMN IF NOT EXISTS dispositivo_id VARCHAR(64);

CREATE INDEX IF NOT EXISTS idx_installation_ticket_dispositivo
  ON installation_ticket(id_inmueble, dispositivo_id);
