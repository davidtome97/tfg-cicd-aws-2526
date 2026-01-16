CREATE TABLE control_despliegue (
                                    id BIGSERIAL PRIMARY KEY,
                                    aplicacion_id BIGINT NOT NULL,
                                    paso VARCHAR(50) NOT NULL,
                                    estado VARCHAR(20) NOT NULL,
                                    mensaje TEXT,
                                    creado_en TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);