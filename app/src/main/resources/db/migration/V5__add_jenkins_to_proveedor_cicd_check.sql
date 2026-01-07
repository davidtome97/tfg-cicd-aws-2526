-- Permitir JENKINS en el CHECK constraint de proveedor_ci_cd
ALTER TABLE aplicacion
DROP CONSTRAINT IF EXISTS aplicacion_proveedor_ci_cd_check;

ALTER TABLE aplicacion
    ADD CONSTRAINT aplicacion_proveedor_ci_cd_check
        CHECK (proveedor_ci_cd IN ('GITHUB','GITLAB','JENKINS'));