-- Convertir sonar_token a TEXT si aún fuese OID/LOB (y preservar contenido si existe)
ALTER TABLE aplicacion
ALTER COLUMN sonar_token TYPE TEXT;

-- Convertir aws_secret_access_key a TEXT (igual)
ALTER TABLE aplicacion
ALTER COLUMN aws_secret_access_key TYPE TEXT;