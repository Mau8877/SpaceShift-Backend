-- Appliance nunca se usó (sin datos reales) y duplicaba el concepto de "dispositivos"
-- que ya existe como JSONB en inmueble. plug_assignment pasa a referenciar inmueble +
-- el id (string) del dispositivo dentro de ese JSONB.
ALTER TABLE plug_assignment DROP CONSTRAINT IF EXISTS fk_plug_assignment_appliance;
ALTER TABLE plug_assignment DROP COLUMN IF EXISTS id_appliance;

ALTER TABLE plug_assignment ADD COLUMN IF NOT EXISTS id_inmueble UUID;
ALTER TABLE plug_assignment ADD COLUMN IF NOT EXISTS dispositivo_id VARCHAR(64);

ALTER TABLE plug_assignment
  ADD CONSTRAINT fk_plug_assignment_inmueble FOREIGN KEY (id_inmueble) REFERENCES inmueble(id);

ALTER TABLE plug_assignment ALTER COLUMN id_inmueble SET NOT NULL;
ALTER TABLE plug_assignment ALTER COLUMN dispositivo_id SET NOT NULL;

DROP TABLE IF EXISTS appliance;
