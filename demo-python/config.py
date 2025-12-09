import os
from pymongo import MongoClient

# ======================
# SECRET KEY DE FLASK
# ======================
SECRET_KEY = os.getenv("APP_SECRET_KEY", "clave-secreta-demo-productos")

# ======================
# VARIABLES DE ENTORNO COMPARTIDAS CON JAVA Y PYTHON
# ======================
DB_ENGINE = os.getenv("DB_ENGINE", "sqlite")   # mysql | postgres | sqlite (Flask SOLO usa estos)
DB_HOST = os.getenv("DB_HOST", "localhost")
DB_PORT = os.getenv("DB_PORT", "3306")
DB_NAME = os.getenv("DB_NAME", "demo")
DB_USER = os.getenv("DB_USER", "root")
DB_PASSWORD = os.getenv("DB_PASSWORD", "root")

# ======================
# PARA MONGO ATLAS (usado SOLO fuera de Flask)
# ======================
MONGO_URI = os.getenv("MONGO_URI")   # Puede venir del .env o de GitHub Secrets


# ======================
# FUNCIÓN PARA OBTENER LA BASE DE DATOS SQL
# ======================
def get_database():
    """
    Devuelve:
      - SQLALCHEMY_DATABASE_URI (para sqlite/mysql/postgres)
      - None para mongo
    Flask SOLO utiliza SQL.
    Mongo se maneja fuera del main mediante MONGO_URI.
    """

    # SQLITE (local)
    if DB_ENGINE == "sqlite":
        return "sqlite:///users.db", None

    # MYSQL
    if DB_ENGINE == "mysql":
        uri = f"mysql+pymysql://{DB_USER}:{DB_PASSWORD}@{DB_HOST}:{DB_PORT}/{DB_NAME}"
        return uri, None

    # POSTGRES
    if DB_ENGINE == "postgres":
        uri = f"postgresql+psycopg2://{DB_USER}:{DB_PASSWORD}@{DB_HOST}:{DB_PORT}/{DB_NAME}"
        return uri, None

    raise ValueError(f"Motor SQL no soportado por Flask: {DB_ENGINE}")