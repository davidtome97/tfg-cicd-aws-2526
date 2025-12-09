import os
from pymongo import MongoClient

# ============================
# SECRET KEY PARA FLASK
# ============================
SECRET_KEY = os.getenv("APP_SECRET_KEY", "clave-secreta-demo-productos")

# ============================
# VARIABLES DE ENTORNO GENERALES
# ============================
DB_ENGINE = os.getenv("DB_ENGINE", "sqlite").lower()   # sqlite | mysql | postgres | mongo
DB_HOST = os.getenv("DB_HOST", "localhost")
DB_PORT = os.getenv("DB_PORT", "3306")
DB_NAME = os.getenv("DB_NAME", "demo")
DB_USER = os.getenv("DB_USER", "root")
DB_PASSWORD = os.getenv("DB_PASSWORD", "root")

# ============================
# CONFIGURACIÓN DE MONGO
# ============================
MONGO_URI = os.getenv(
    "MONGO_URI",
    "mongodb://demo:demo@mongo:27017/demo?authSource=admin"
)


def get_mongo_client():
    """
    Cliente MongoDB listo para usar cuando el motor sea 'mongo'.
    """
    try:
        client = MongoClient(MONGO_URI, serverSelectionTimeoutMS=3000)
        client.server_info()  # fuerza verificación
        return client
    except Exception as e:
        print(f"[ERROR] No se pudo conectar a Mongo: {e}")
        return None


# ============================
# CONFIGURACIÓN SQL PARA FLASK
# ============================
def get_sqlalchemy_uri():
    """
    Devuelve el SQLALCHEMY_DATABASE_URI que Flask necesita,
    dependiendo del motor:
      - sqlite
      - mysql
      - postgres
    Si el motor es 'mongo', devolvemos None → no se usa SQL.
    """

    if DB_ENGINE == "sqlite":
        return "sqlite:///users.db"

    if DB_ENGINE == "mysql":
        return (
            f"mysql+pymysql://{DB_USER}:{DB_PASSWORD}"
            f"@{DB_HOST}:{DB_PORT}/{DB_NAME}"
        )

    if DB_ENGINE == "postgres":
        return (
            f"postgresql+psycopg2://{DB_USER}:{DB_PASSWORD}"
            f"@{DB_HOST}:{DB_PORT}/{DB_NAME}"
        )

    if DB_ENGINE == "mongo":
        # En este caso no usamos SQLAlchemy
        return None

    raise ValueError(f"[ERROR] Motor de base de datos no soportado: {DB_ENGINE}")