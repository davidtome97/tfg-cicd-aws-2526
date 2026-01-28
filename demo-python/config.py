import os
from pymongo import MongoClient

# ============================
# SECRET KEY PARA FLASK
# ============================
SECRET_KEY = os.getenv("APP_SECRET_KEY", "clave-secreta-demo-productos")

# ============================
# VARIABLES DE ENTORNO GENERALES
# ============================
# IMPORTANTE: para tu objetivo, default = mongo
DB_ENGINE = os.getenv("DB_ENGINE", "mongo").lower()  # mongo | mysql | postgres | sqlite

DB_HOST = os.getenv("DB_HOST", "localhost")

# Default ports razonables según motor
_default_port = {"mongo": "27017", "mysql": "3306", "postgres": "5432", "sqlite": ""}.get(DB_ENGINE, "27017")
DB_PORT = os.getenv("DB_PORT", _default_port)

DB_NAME = os.getenv("DB_NAME", "demo")
DB_USER = os.getenv("DB_USER", "demo")
DB_PASSWORD = os.getenv("DB_PASSWORD", "demo")

# Remote-friendly: URI completa (Atlas, etc.)
DB_URI = os.getenv("DB_URI")       # la usarás en profile remote
MONGO_URI_ENV = os.getenv("MONGO_URI")  # compatibilidad si alguien la usa

# ============================
# CONFIGURACIÓN DE MONGO
# ============================
def get_mongo_uri() -> str:
    """
    Prioridad:
      1) DB_URI (remote)
      2) MONGO_URI (compat)
      3) construir desde DB_HOST/DB_PORT/DB_USER/DB_PASSWORD/DB_NAME (local)
    """
    if DB_URI and DB_URI.strip():
        return DB_URI.strip()

    if MONGO_URI_ENV and MONGO_URI_ENV.strip():
        return MONGO_URI_ENV.strip()

    return (
        f"mongodb://{DB_USER}:{DB_PASSWORD}@{DB_HOST}:{DB_PORT}/{DB_NAME}"
        f"?authSource=admin"
    )

MONGO_URI = get_mongo_uri()


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
    dependiendo del motor.
    Si el motor es 'mongo', devolvemos None → no se usa SQL.
    """
    if DB_ENGINE == "sqlite":
        return "sqlite:///users.db"

    if DB_ENGINE == "mysql":
        return f"mysql+pymysql://{DB_USER}:{DB_PASSWORD}@{DB_HOST}:{DB_PORT}/{DB_NAME}"

    if DB_ENGINE == "postgres":
        return f"postgresql+psycopg2://{DB_USER}:{DB_PASSWORD}@{DB_HOST}:{DB_PORT}/{DB_NAME}"

    if DB_ENGINE == "mongo":
        return None

    raise ValueError(f"[ERROR] Motor de base de datos no soportado: {DB_ENGINE}")