# demo-python/config.py
import os

# Clave secreta de Flask
SECRET_KEY = os.getenv("APP_SECRET_KEY", "clave-secreta-demo-productos")

# Cadena de conexión a la base de datos
# En producción vendrá de un secret (APP_DB_URL).
# En local, por defecto usa SQLite.
DB_URL = os.getenv("APP_DB_URL", "sqlite:///users.db")