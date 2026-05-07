CREATE TABLE contrato (
    id UUID PRIMARY KEY,
    id_inmueble UUID NOT NULL,
    id_publicacion UUID,
    id_propietario UUID NOT NULL,
    id_cliente UUID NOT NULL,

    tipo_contrato VARCHAR(50) NOT NULL,
    estado_contrato VARCHAR(50) NOT NULL,

    monto_acordado DECIMAL(12,2) NOT NULL,
    moneda VARCHAR(10) NOT NULL,

    fecha_inicio DATE,
    fecha_fin DATE,

    cantidad_huespedes INT,
    noches INT,

    documento_url VARCHAR(500),
    observacion TEXT,
    creado_en TIMESTAMP NOT NULL,

    FOREIGN KEY (id_inmueble) REFERENCES inmueble(id),
    FOREIGN KEY (id_publicacion) REFERENCES publicacion(id),
    FOREIGN KEY (id_propietario) REFERENCES usuario(id),
    FOREIGN KEY (id_cliente) REFERENCES usuario(id)
);