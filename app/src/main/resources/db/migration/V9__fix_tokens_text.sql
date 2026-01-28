ALTER TABLE aplicacion ADD COLUMN IF NOT EXISTS github_token TEXT;
ALTER TABLE aplicacion ADD COLUMN IF NOT EXISTS gitlab_token TEXT;
-- si aquí estabas cambiando tipos: en Postgres “TEXT” ya te vale, no fuerces ALTER TYPE si no hace falta