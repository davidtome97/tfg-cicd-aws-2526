# demo-python/models.py
from flask_sqlalchemy import SQLAlchemy
from sqlalchemy.orm import DeclarativeBase, Mapped, mapped_column
from sqlalchemy import Integer, String, Float, ForeignKey
from flask_login import UserMixin

from config import DB_NAME, DB_ENGINE, get_sqlalchemy_uri, get_mongo_client


# ============================
# BASE Y SQLALCHEMY
# ============================
class Base(DeclarativeBase):
    pass


db = SQLAlchemy(model_class=Base)


def init_databases(app):
    """
    Inicializa:
      - SQLAlchemy (sqlite/mysql/postgres) si aplica
      - Cliente de Mongo si DB_ENGINE == 'mongo'

    Devuelve:
      sql_enabled (bool), mongo_usuarios, mongo_productos
    """

    # ---------- SQL ----------
    sql_uri = get_sqlalchemy_uri()
    sql_enabled = False

    if sql_uri is not None:
        app.config["SQLALCHEMY_DATABASE_URI"] = sql_uri
        app.config["SQLALCHEMY_TRACK_MODIFICATIONS"] = False

        db.init_app(app)
        with app.app_context():
            db.create_all()

        sql_enabled = True
    else:
        app.config["SQLALCHEMY_DATABASE_URI"] = None

    # ---------- MONGO ----------
    mongo_client = get_mongo_client() if DB_ENGINE == "mongo" else None
    mongo_db = mongo_client.get_database(DB_NAME) if mongo_client is not None else None
    mongo_usuarios = mongo_db["usuarios"] if mongo_db is not None else None
    mongo_productos = mongo_db["productos"] if mongo_db is not None else None

    return sql_enabled, mongo_usuarios, mongo_productos


# ============================
# MODELOS SQL
# ============================
class User(UserMixin, db.Model):
    """
    Modelo SQL para usuarios.
    Solo se usa cuando DB_ENGINE != 'mongo'.
    """
    __tablename__ = "usuarios"

    id: Mapped[int] = mapped_column(Integer, primary_key=True)
    correo: Mapped[str] = mapped_column(String(255), unique=True, nullable=False)  # ✅
    password: Mapped[str] = mapped_column(String(255), nullable=False)             # ✅
    nombre: Mapped[str] = mapped_column(String(255), nullable=False)               # ✅


class Producto(db.Model):
    """
    Modelo SQL para productos.
    Solo se usa cuando DB_ENGINE != 'mongo'.
    """
    __tablename__ = "productos"

    id: Mapped[int] = mapped_column(Integer, primary_key=True)
    nombre: Mapped[str] = mapped_column(String(100), nullable=False)  # ✅
    precio: Mapped[float] = mapped_column(Float, nullable=False)      # ✅

    # ✅ dueño (usuario logueado)
    user_id: Mapped[int] = mapped_column(ForeignKey("usuarios.id"), nullable=False)


# ============================
# MODELOS "VISTA" PARA MONGO
# ============================
class MongoUser(UserMixin):
    """
    Wrapper para un documento de Mongo para que funcione con Flask-Login.
    Usamos 'correo' como ID lógico.
    """

    def __init__(self, doc: dict):
        self.doc = doc

    @property
    def correo(self) -> str:
        return self.doc.get("correo", "")

    @property
    def password(self) -> str:
        return self.doc.get("password", "")

    @property
    def nombre(self) -> str:
        return self.doc.get("nombre", "")

    def get_id(self) -> str:
        # Flask-Login usará esto como identificador
        return self.correo


class ProductoView:
    """
    Objeto sencillo para pasar productos (SQL o Mongo) a las plantillas.
    """
    def __init__(self, _id, nombre, precio):
        self.id = _id
        self.nombre = nombre
        self.precio = precio