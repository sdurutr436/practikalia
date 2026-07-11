CREATE TABLE usuario_etiqueta (
    usuario_id BIGINT NOT NULL REFERENCES usuario(id),
    etiqueta_id BIGINT NOT NULL REFERENCES etiqueta(id),
    PRIMARY KEY (usuario_id, etiqueta_id)
);
