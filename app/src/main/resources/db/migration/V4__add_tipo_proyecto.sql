ALTER TABLE aplicacion
    ADD COLUMN IF NOT EXISTS tipo_proyecto VARCHAR(20);

UPDATE aplicacion
SET tipo_proyecto = 'CONFIG'
WHERE tipo_proyecto IS NULL;