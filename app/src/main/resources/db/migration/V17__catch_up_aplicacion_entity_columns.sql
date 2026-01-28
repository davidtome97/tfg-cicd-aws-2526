-- 1) Añadir columnas que tu entidad espera (si faltan)
ALTER TABLE aplicacion
    ADD COLUMN IF NOT EXISTS lenguaje VARCHAR(30),
    ADD COLUMN IF NOT EXISTS proveedor_ci_cd VARCHAR(30),
    ADD COLUMN IF NOT EXISTS repositorio_git VARCHAR(255),
    ADD COLUMN IF NOT EXISTS puerto_aplicacion INTEGER,
    ADD COLUMN IF NOT EXISTS tipo_base_datos VARCHAR(30),
    ADD COLUMN IF NOT EXISTS nombre_base_datos VARCHAR(120),
    ADD COLUMN IF NOT EXISTS usuario_base_datos VARCHAR(120),
    ADD COLUMN IF NOT EXISTS password_base_datos TEXT,
    ADD COLUMN IF NOT EXISTS sonar_project_key VARCHAR(255),
    ADD COLUMN IF NOT EXISTS sonar_host_url VARCHAR(255),
    ADD COLUMN IF NOT EXISTS sonar_organization VARCHAR(255),
    ADD COLUMN IF NOT EXISTS nombre_imagen_ecr VARCHAR(255);

-- 2) Ajustar tamaños para que no falle validate
ALTER TABLE aplicacion
ALTER COLUMN image_tag TYPE VARCHAR(80);

-- 3) Copiar datos desde columnas antiguas a las nuevas (si existen)
UPDATE aplicacion
SET repositorio_git = COALESCE(repositorio_git, repo_url)
WHERE repo_url IS NOT NULL;

UPDATE aplicacion
SET proveedor_ci_cd = COALESCE(proveedor_ci_cd, proveedor_cicd)
WHERE proveedor_cicd IS NOT NULL;

UPDATE aplicacion
SET nombre_base_datos = COALESCE(nombre_base_datos, db_name)
WHERE db_name IS NOT NULL;

UPDATE aplicacion
SET usuario_base_datos = COALESCE(usuario_base_datos, db_user)
WHERE db_user IS NOT NULL;

UPDATE aplicacion
SET password_base_datos = COALESCE(password_base_datos, db_password)
WHERE db_password IS NOT NULL;

-- Si quieres mapear tipo_base_datos desde db_engine (si tienes valores compatibles)
-- UPDATE aplicacion
-- SET tipo_base_datos = COALESCE(tipo_base_datos, db_engine)
-- WHERE db_engine IS NOT NULL;

-- 4) (Opcional) checks para enums si los necesitas (depende de tus enums)
-- Ejemplo genérico: ajusta a tus valores reales
DO $$
BEGIN
  IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'aplicacion_lenguaje_check') THEN
ALTER TABLE aplicacion
    ADD CONSTRAINT aplicacion_lenguaje_check
        CHECK (lenguaje IS NULL OR lenguaje IN ('JAVA','PYTHON','NODE'));
END IF;
END $$;