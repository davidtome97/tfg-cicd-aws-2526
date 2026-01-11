-- V12__add_ec2_vars.sql
-- Campos del Paso 6 (EC2) por aplicación

ALTER TABLE aplicacion
    ADD COLUMN IF NOT EXISTS ec2_host        VARCHAR(255),
    ADD COLUMN IF NOT EXISTS ec2_user        VARCHAR(120),
    ADD COLUMN IF NOT EXISTS ec2_known_hosts TEXT,
    ADD COLUMN IF NOT EXISTS ec2_llave_ssh   TEXT,
    ADD COLUMN IF NOT EXISTS app_port        INTEGER;
