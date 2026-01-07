-- Ver qué valores hay (opcional, útil para debug)
-- SELECT DISTINCT db_modo FROM aplicacion;

-- Normalizar a enum nuevo
UPDATE aplicacion SET db_modo = 'LOCAL'  WHERE db_modo IN ('local', 'LOCAL');
UPDATE aplicacion SET db_modo = 'REMOTE' WHERE db_modo IN ('remote','REMOTE');

-- Si hay nulls, pon LOCAL por defecto
UPDATE aplicacion SET db_modo = 'LOCAL' WHERE db_modo IS NULL;