CREATE TABLE IF NOT EXISTS plug_usage_session (
    id UUID PRIMARY KEY,
    id_smart_plug UUID NOT NULL,
    started_at TIMESTAMP NOT NULL,
    ended_at TIMESTAMP,

    created_by UUID,
    created_date TIMESTAMP NOT NULL DEFAULT NOW(),
    last_modified_by UUID,
    last_modified_date TIMESTAMP,
    deleted BOOLEAN DEFAULT FALSE,

    CONSTRAINT fk_plug_usage_session_smart_plug FOREIGN KEY (id_smart_plug) REFERENCES smart_plug(id)
);
CREATE INDEX IF NOT EXISTS idx_plug_usage_session_plug_abierta
  ON plug_usage_session(id_smart_plug) WHERE ended_at IS NULL;
