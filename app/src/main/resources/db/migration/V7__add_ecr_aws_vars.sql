ALTER TABLE aplicacion
    ADD COLUMN IF NOT EXISTS ecr_repository            VARCHAR(255),
    ADD COLUMN IF NOT EXISTS aws_region                VARCHAR(50),
    ADD COLUMN IF NOT EXISTS aws_access_key_id         VARCHAR(255),
    ADD COLUMN IF NOT EXISTS aws_secret_access_key     TEXT,
    ADD COLUMN IF NOT EXISTS aws_account_id            VARCHAR(50);
