ALTER TABLE aplicacion ADD COLUMN IF NOT EXISTS db_engine VARCHAR(20);
ALTER TABLE aplicacion ADD COLUMN IF NOT EXISTS db_host VARCHAR(255);
ALTER TABLE aplicacion ADD COLUMN IF NOT EXISTS db_port INTEGER;
ALTER TABLE aplicacion ADD COLUMN IF NOT EXISTS db_name VARCHAR(255);
ALTER TABLE aplicacion ADD COLUMN IF NOT EXISTS db_user VARCHAR(255);
ALTER TABLE aplicacion ADD COLUMN IF NOT EXISTS db_password TEXT;
ALTER TABLE aplicacion ADD COLUMN IF NOT EXISTS db_uri TEXT;
ALTER TABLE aplicacion ADD COLUMN IF NOT EXISTS db_sslmode VARCHAR(20);

-- defaults
UPDATE aplicacion SET db_modo = 'LOCAL'
WHERE db_modo IS NULL OR TRIM(db_modo) = '';

-- constraints
ALTER TABLE aplicacion DROP CONSTRAINT IF EXISTS aplicacion_tipo_base_datos_check;
-- si tienes columna tipo_base_datos:
-- ALTER TABLE aplicacion ADD CONSTRAINT ... (solo si esa columna existe)