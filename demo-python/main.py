from flask import Flask, render_template, request, url_for, redirect, flash
from werkzeug.security import generate_password_hash, check_password_hash
from flask_sqlalchemy import SQLAlchemy
from sqlalchemy.orm import DeclarativeBase, Mapped, mapped_column
from sqlalchemy import Integer, String, Float
from flask_login import (
    UserMixin,
    login_user,
    LoginManager,
    login_required,
    current_user,
    logout_user,
)

from config import SECRET_KEY, DB_URL   #  importo la config

app = Flask(__name__)
app.config["SECRET_KEY"] = SECRET_KEY

# ------------ BBDD Y MODELOS ------------ #

class Base(DeclarativeBase):
    pass


#  viene de APP_DB_URL o sqlite por defecto
app.config["SQLALCHEMY_DATABASE_URI"] = DB_URL
db = SQLAlchemy(model_class=Base)
db.init_app(app)

login_manager = LoginManager()
login_manager.login_view = "login"  # si no está logueado, manda a /login
login_manager.init_app(app)


class User(UserMixin, db.Model):
    __tablename__ = "usuarios"

    id: Mapped[int] = mapped_column(Integer, primary_key=True)
    email: Mapped[str] = mapped_column(String(100), unique=True)
    password: Mapped[str] = mapped_column(String(100))
    name: Mapped[str] = mapped_column(String(100))


class Producto(db.Model):
    __tablename__ = "productos"

    id: Mapped[int] = mapped_column(Integer, primary_key=True)
    nombre: Mapped[str] = mapped_column(String(100))
    precio: Mapped[float] = mapped_column(Float)


with app.app_context():
    db.create_all()


@login_manager.user_loader
def load_user(user_id: str):
    return db.session.get(User, int(user_id))


# ------------ RUTAS DE AUTENTICACIÓN ------------ #

@app.route("/register", methods=["GET", "POST"])
def register():
    if request.method == "POST":
        email = request.form.get("email")

        existente = db.session.execute(
            db.select(User).where(User.email == email)
        ).scalar()
        if existente:
            flash("Ya estás registrado. Inicia sesión.")
            return redirect(url_for("login"))

        password = request.form.get("password")
        hash_password = generate_password_hash(
            password, method="pbkdf2:sha256", salt_length=8
        )

        nuevo = User(
            email=email,
            password=hash_password,
            name=request.form.get("name"),
        )
        db.session.add(nuevo)
        db.session.commit()

        login_user(nuevo)
        return redirect(url_for("lista_productos"))

    return render_template("register.html", logged_in=current_user.is_authenticated)


@app.route("/login", methods=["GET", "POST"])
def login():
    if request.method == "POST":
        email = request.form.get("email")
        password = request.form.get("password")

        resultado = db.session.execute(
            db.select(User).where(User.email == email)
        ).scalar()

        if not resultado:
            flash("Ese correo no existe.")
            return redirect(url_for("login"))

        if not check_password_hash(resultado.password, password):
            flash("Contraseña incorrecta.")
            return redirect(url_for("login"))

        login_user(resultado)
        return redirect(url_for("lista_productos"))

    return render_template("login.html", logged_in=current_user.is_authenticated)


@app.route("/logout")
@login_required
def logout():
    logout_user()
    return redirect(url_for("login"))


# ------------ RUTAS CRUD DE PRODUCTOS ------------ #

@app.route("/")
@login_required
def lista_productos():
    productos = db.session.execute(db.select(Producto)).scalars().all()
    return render_template(
        "index.html",
        productos=productos,
        logged_in=current_user.is_authenticated,
    )


@app.route("/producto/nuevo", methods=["POST"])
@login_required
def crear_producto():
    nombre = request.form.get("nombre")
    precio_raw = request.form.get("precio") or "0"

    try:
        precio = float(precio_raw)
    except ValueError:
        precio = 0.0

    prod = Producto(nombre=nombre, precio=precio)
    db.session.add(prod)
    db.session.commit()

    return redirect(url_for("lista_productos"))


@app.route("/producto/editar/<int:producto_id>", methods=["GET", "POST"])
@login_required
def editar_producto(producto_id: int):
    prod = db.session.get(Producto, producto_id)
    if not prod:
        flash("El producto no existe.")
        return redirect(url_for("lista_productos"))

    if request.method == "POST":
        prod.nombre = request.form.get("nombre") or prod.nombre
        precio_raw = request.form.get("precio") or str(prod.precio)
        try:
            prod.precio = float(precio_raw)
        except ValueError:
            flash("Precio inválido. No se ha actualizado el producto.")
            return redirect(url_for("editar_producto", producto_id=producto_id))

        db.session.commit()
        flash("Producto actualizado correctamente.")
        return redirect(url_for("lista_productos"))

    return render_template(
        "producto_form.html",
        producto=prod,
        logged_in=current_user.is_authenticated,
    )


@app.route("/producto/eliminar/<int:producto_id>")
@login_required
def eliminar_producto(producto_id: int):
    prod = db.session.get(Producto, producto_id)
    if prod:
        db.session.delete(prod)
        db.session.commit()
        flash("Producto eliminado.")
    return redirect(url_for("lista_productos"))


if __name__ == "__main__":
    app.run(debug=True)