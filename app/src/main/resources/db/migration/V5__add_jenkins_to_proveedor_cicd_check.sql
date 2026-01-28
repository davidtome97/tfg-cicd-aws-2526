ALTER TABLE aplicacion
    ADD COLUMN IF NOT EXISTS proveedor_cicd VARCHAR(30);

ALTER TABLE aplicacion
DROP CONSTRAINT IF EXISTS aplicacion_proveedor_cicd_check;

ALTER TABLE aplicacion
    ADD CONSTRAINT aplicacion_proveedor_cicd_check
        CHECK (proveedor_cicd IS NULL OR proveedor_cicd IN ('GITHUB', 'GITLAB', 'JENKINS'));