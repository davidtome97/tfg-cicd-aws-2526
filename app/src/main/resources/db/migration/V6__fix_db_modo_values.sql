ALTER TABLE aplicacion
    ADD COLUMN IF NOT EXISTS db_modo VARCHAR(20);

UPDATE aplicacion
SET db_modo = 'LOCAL'
WHERE db_modo IS NULL OR TRIM(db_modo) = '';

-- Normaliza posibles valores viejos
UPDATE aplicacion
SET db_modo = 'REMOTE'
WHERE UPPER(TRIM(db_modo)) IN ('REMOTO', 'RDS', 'CLOUD');

ALTER TABLE aplicacion
DROP CONSTRAINT IF EXISTS aplicacion_db_modo_check;

ALTER TABLE aplicacion
    ADD CONSTRAINT aplicacion_db_modo_check
        CHECK (db_modo IS NULL OR db_modo IN ('LOCAL', 'REMOTE'));