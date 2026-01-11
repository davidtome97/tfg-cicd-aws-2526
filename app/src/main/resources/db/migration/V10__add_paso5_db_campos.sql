-- V10__add_paso5_db_campos.sql
-- Campos necesarios para Paso 5 (Base de datos) en tabla aplicacion.

-- 1) Columnas principales (PASO 5)
ALTER TABLE aplicacion
    ADD COLUMN IF NOT EXISTS tipo_base_datos      VARCHAR(30),
    ADD COLUMN IF NOT EXISTS db_modo              VARCHAR(20),
    ADD COLUMN IF NOT EXISTS nombre_base_datos    VARCHAR(120),
    ADD COLUMN IF NOT EXISTS usuario_base_datos   VARCHAR(120),
    ADD COLUMN IF NOT EXISTS password_base_datos  TEXT;

-- 2) Opcionales recomendadas (para precargar y para resumen final sin depender del navegador)
ALTER TABLE aplicacion
    ADD COLUMN IF NOT EXISTS db_host              VARCHAR(255),
    ADD COLUMN IF NOT EXISTS db_port              INTEGER,
    ADD COLUMN IF NOT EXISTS db_uri               TEXT,
    ADD COLUMN IF NOT EXISTS db_sslmode           VARCHAR(20);

-- 3) Defaults para filas antiguas
UPDATE aplicacion
SET db_modo = 'LOCAL'
WHERE db_modo IS NULL OR TRIM(db_modo) = '';

-- 4) Constraints (alineadas con tus enums)
ALTER TABLE aplicacion
DROP CONSTRAINT IF EXISTS aplicacion_db_modo_check;

ALTER TABLE aplicacion
    ADD CONSTRAINT aplicacion_db_modo_check
        CHECK (db_modo IN ('LOCAL', 'REMOTE'));

ALTER TABLE aplicacion
DROP CONSTRAINT IF EXISTS aplicacion_tipo_base_datos_check;

-- IMPORTANTE: Ajusta estos valores a tu enum real.
-- Si tu enum es POSTGRESQL / MYSQL / MONGODB (lo más típico):
ALTER TABLE aplicacion
    ADD CONSTRAINT aplicacion_tipo_base_datos_check
        CHECK (
            tipo_base_datos IS NULL
                OR tipo_base_datos IN ('POSTGRESQL', 'MYSQL', 'MONGODB')
            );