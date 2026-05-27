-- V4__add_tipo_proyecto.sql
-- 1) Crear tabla aplicacion si no existe (mínimo viable para el resto de migraciones)
CREATE TABLE IF NOT EXISTS aplicacion (
                                          id                  BIGSERIAL PRIMARY KEY,

                                          nombre              VARCHAR(255),
    repo_url            TEXT,

    proveedor_cicd      VARCHAR(30),
    tipo_proyecto       VARCHAR(30),

    -- DB
    db_modo             VARCHAR(20),
    db_engine           VARCHAR(20),
    db_host             VARCHAR(255),
    db_port             INTEGER,
    db_name             VARCHAR(255),
    db_user             VARCHAR(255),
    db_password         TEXT,
    db_uri              TEXT,
    db_sslmode          VARCHAR(20),
    db_endpoint         TEXT,

    -- Tokens
    github_token        TEXT,
    gitlab_token        TEXT,
    sonar_token         TEXT,

    -- AWS/ECR
    aws_access_key_id   TEXT,
    aws_secret_access_key TEXT,
    aws_region          TEXT,
    aws_account_id      TEXT,
    ecr_repository      TEXT,

    -- EC2
    ec2_host            TEXT,
    ec2_user            TEXT,
    ec2_llave_ssh       TEXT,
    ec2_known_hosts     TEXT,

    -- app
    app_port            INTEGER
    );

-- 2) Asegurar columna tipo_proyecto (por si la tabla ya existía en algún entorno)
ALTER TABLE aplicacion
    ADD COLUMN IF NOT EXISTS tipo_proyecto VARCHAR(30);

-- 3) (Opcional) constraint del enum si quieres aquí mismo
ALTER TABLE aplicacion
DROP CONSTRAINT IF EXISTS aplicacion_tipo_proyecto_check;

ALTER TABLE aplicacion
    ADD CONSTRAINT aplicacion_tipo_proyecto_check
        CHECK (tipo_proyecto IS NULL OR tipo_proyecto IN ('DEMO', 'APP_PRINCIPAL', 'OTRO'));