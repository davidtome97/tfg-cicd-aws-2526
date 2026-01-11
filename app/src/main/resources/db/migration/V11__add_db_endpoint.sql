-- V11__add_db_endpoint.sql
ALTER TABLE aplicacion
    ADD COLUMN IF NOT EXISTS db_endpoint VARCHAR(255);