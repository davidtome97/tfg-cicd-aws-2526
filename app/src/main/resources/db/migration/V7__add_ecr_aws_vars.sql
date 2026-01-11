ALTER TABLE aplicacion ADD COLUMN IF NOT EXISTS aws_access_key_id TEXT;
ALTER TABLE aplicacion ADD COLUMN IF NOT EXISTS aws_secret_access_key TEXT;
ALTER TABLE aplicacion ADD COLUMN IF NOT EXISTS aws_region TEXT;
ALTER TABLE aplicacion ADD COLUMN IF NOT EXISTS aws_account_id TEXT;
ALTER TABLE aplicacion ADD COLUMN IF NOT EXISTS ecr_repository TEXT;