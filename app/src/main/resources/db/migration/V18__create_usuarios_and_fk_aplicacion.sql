-- 1) Tabla usuarios con las columnas que pide Usuario.java
CREATE TABLE IF NOT EXISTS usuarios (
                                        id BIGSERIAL PRIMARY KEY,
                                        nombre VARCHAR(255),
    correo VARCHAR(255),
    password VARCHAR(255)
    );

-- 2) Uniqueness de correo (por @Column(unique = true))
DO $$
BEGIN
  IF NOT EXISTS (
    SELECT 1
    FROM pg_constraint
    WHERE conname = 'uk_usuarios_correo'
  ) THEN
ALTER TABLE usuarios
    ADD CONSTRAINT uk_usuarios_correo UNIQUE (correo);
END IF;
END $$;

-- 3) Columna usuario_id en aplicacion
ALTER TABLE aplicacion
    ADD COLUMN IF NOT EXISTS usuario_id BIGINT;

-- 4) FK aplicacion.usuario_id -> usuarios.id
DO $$
BEGIN
  IF NOT EXISTS (
    SELECT 1
    FROM pg_constraint
    WHERE conname = 'fk_aplicacion_usuario'
  ) THEN
ALTER TABLE aplicacion
    ADD CONSTRAINT fk_aplicacion_usuario
        FOREIGN KEY (usuario_id) REFERENCES usuarios(id)
            ON DELETE SET NULL;
END IF;
END $$;