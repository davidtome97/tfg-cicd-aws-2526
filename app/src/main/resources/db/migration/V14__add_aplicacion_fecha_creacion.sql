-- Añadir columna si no existe
ALTER TABLE aplicacion
    ADD COLUMN IF NOT EXISTS fecha_creacion TIMESTAMP;

-- Si quieres que no sea null y auto-rellene:
UPDATE aplicacion
SET fecha_creacion = NOW()
WHERE fecha_creacion IS NULL;

ALTER TABLE aplicacion
    ALTER COLUMN fecha_creacion SET NOT NULL;

-- Default para nuevas filas
ALTER TABLE aplicacion
    ALTER COLUMN fecha_creacion SET DEFAULT NOW();