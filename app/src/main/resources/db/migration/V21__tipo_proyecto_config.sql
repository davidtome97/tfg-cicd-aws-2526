ALTER TABLE aplicacion
DROP CONSTRAINT IF EXISTS aplicacion_tipo_proyecto_check;

UPDATE aplicacion
SET tipo_proyecto = 'CONFIG'
WHERE tipo_proyecto = 'APP_PRINCIPAL';

ALTER TABLE aplicacion
    ADD CONSTRAINT aplicacion_tipo_proyecto_check
        CHECK (
            tipo_proyecto IS NULL
                OR tipo_proyecto IN ('CONFIG', 'DEMO')
            );