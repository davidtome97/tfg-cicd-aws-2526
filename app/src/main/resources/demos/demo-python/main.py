# demo-python/main.py
from flask import Flask, render_template, request, url_for, redirect, flash
from werkzeug.security import generate_password_hash, check_password_hash
from flask_login import (
    login_user,
    LoginManager,
    login_required,
    current_user,
    logout_user,
)
from bson import ObjectId

from config import SECRET_KEY, DB_ENGINE
from models import (
    db,
    init_databases,
    User,
    Producto,
    MongoUser,
    ProductoView,
)

# ============================
# APP FLASK
# ============================
app = Flask(__name__)
app.config["SECRET_KEY"] = SECRET_KEY

# ============================
# BBDD (SQL + MONGO)
# ============================
sql_enabled, mongo_usuarios, mongo_productos = init_databases(app)

# ============================
# LOGIN MANAGER
# ============================
login_manager = LoginManager()
login_manager.login_view = "login"
login_manager.init_app(app)


# ============================
# CARGA DE USUARIO PARA LOGIN
# ============================
@login_manager.user_loader
def load_user(user_id: str):
    # MODO MONGO → buscamos por correo
    if DB_ENGINE == "mongo":
        if mongo_usuarios is None:
            return None
        doc = mongo_usuarios.find_one({"correo": user_id})
        if doc is None:
            return None
        return MongoUser(doc)

    # MODO SQL → usamos la PK numérica
    if not sql_enabled or User is None:
        return None
    try:
        return db.session.get(User, int(user_id))
    except Exception:
        return None


# ============================
# RUTAS DE AUTENTICACIÓN
# ============================
@app.route("/register", methods=["GET", "POST"])
def register():
    if request.method == "POST":
        correo = request.form.get("email")

        # ---------- MODO MONGO ----------
        if DB_ENGINE == "mongo":
            existente = (
                mongo_usuarios.find_one({"correo": correo})
                if mongo_usuarios is not None
                else None
            )
            if existente is not None:
                flash("Ya estás registrado. Inicia sesión.")
                return redirect(url_for("login"))

            password = request.form.get("password")
            hash_password = generate_password_hash(
                password, method="pbkdf2:sha256", salt_length=8
            )
            doc = {
                "correo": correo,
                "password": hash_password,
                "nombre": request.form.get("name"),
            }
            resultado = mongo_usuarios.insert_one(doc)
            doc["_id"] = resultado.inserted_id
            login_user(MongoUser(doc))
            return redirect(url_for("lista_productos"))

        # ---------- MODO SQL ----------
        existente = db.session.execute(
            db.select(User).where(User.correo == correo)
        ).scalar()
        if existente is not None:
            flash("Ya estás registrado. Inicia sesión.")
            return redirect(url_for("login"))

        password = request.form.get("password")
        hash_password = generate_password_hash(
            password, method="pbkdf2:sha256", salt_length=8
        )

        nuevo = User(
            correo=correo,
            password=hash_password,
            nombre=request.form.get("name"),
        )
        db.session.add(nuevo)
        db.session.commit()

        login_user(nuevo)
        return redirect(url_for("lista_productos"))

    return render_template("register.html", logged_in=current_user.is_authenticated)


@app.route("/login", methods=["GET", "POST"])
def login():
    if request.method == "POST":
        correo = request.form.get("email")
        password = request.form.get("password")

        # ---------- MODO MONGO ----------
        if DB_ENGINE == "mongo":
            if mongo_usuarios is None:
                flash("Error de configuración en Mongo.")
                return redirect(url_for("login"))

            doc = mongo_usuarios.find_one({"correo": correo})
            if doc is None:
                flash("Ese correo no existe.")
                return redirect(url_for("login"))

            if not str(doc.get("password", "")).startswith("pbkdf2:"):
                flash(
                    "Este usuario fue creado en otra aplicación. "
                    "Por favor, regístrate de nuevo en la demo de Python."
                )
                return redirect(url_for("register"))

            if not check_password_hash(doc["password"], password):
                flash("Contraseña incorrecta.")
                return redirect(url_for("login"))

            login_user(MongoUser(doc))
            return redirect(url_for("lista_productos"))

        # ---------- MODO SQL ----------
        resultado = db.session.execute(
            db.select(User).where(User.correo == correo)
        ).scalar()

        if resultado is None:
            flash("Ese correo no existe.")
            return redirect(url_for("login"))

        if not str(resultado.password).startswith("pbkdf2:"):
            flash(
                "Este usuario fue creado en otra aplicación. "
                "Por favor, regístrate de nuevo en la demo de Python."
            )
            return redirect(url_for("register"))

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


# ============================
# CRUD PRODUCTOS
# ============================
@app.route("/")
@login_required
def lista_productos():
    # Identidad del usuario logueado
    owner = current_user.get_id()  # email en Mongo; en SQL no lo usamos para filtrar

    # ---------- MODO MONGO ----------
    if DB_ENGINE == "mongo":
        docs = (
            list(mongo_productos.find({"owner": owner}))
            if mongo_productos is not None
            else []
        )
        productos = [
            ProductoView(
                _id=str(doc.get("_id")),
                nombre=doc.get("nombre", ""),
                precio=doc.get("precio", 0.0),
            )
            for doc in docs
        ]
        return render_template(
            "index.html",
            productos=productos,
            logged_in=current_user.is_authenticated,
        )

    # ---------- MODO SQL ----------
    productos_sql = (
        db.session.execute(
            db.select(Producto).where(Producto.user_id == current_user.id)
        )
        .scalars()
        .all()
    )
    return render_template(
        "index.html",
        productos=productos_sql,
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

    # ---------- MODO MONGO ----------
    if DB_ENGINE == "mongo":
        if mongo_productos is not None:
            mongo_productos.insert_one(
                {"nombre": nombre, "precio": precio, "owner": current_user.get_id()}
            )
        return redirect(url_for("lista_productos"))

    # ---------- MODO SQL ----------
    prod = Producto(nombre=nombre, precio=precio, user_id=current_user.id)
    db.session.add(prod)
    db.session.commit()

    return redirect(url_for("lista_productos"))


@app.route("/producto/editar/<producto_id>", methods=["GET", "POST"])
@login_required
def editar_producto(producto_id: str):
    # ---------- MODO MONGO ----------
    if DB_ENGINE == "mongo":
        if mongo_productos is None:
            flash("Error de configuración en Mongo.")
            return redirect(url_for("lista_productos"))

        owner = current_user.get_id()
        try:
            # ✅ solo si es tuyo
            doc = mongo_productos.find_one({"_id": ObjectId(producto_id), "owner": owner})
        except Exception:
            doc = None

        if doc is None:
            flash("No autorizado o el producto no existe.")
            return redirect(url_for("lista_productos"))

        if request.method == "POST":
            nuevo_nombre = request.form.get("nombre") or doc.get("nombre", "")
            precio_raw = request.form.get("precio") or str(doc.get("precio", 0.0))
            try:
                nuevo_precio = float(precio_raw)
            except ValueError:
                flash("Precio inválido.")
                return redirect(url_for("editar_producto", producto_id=producto_id))

            mongo_productos.update_one(
                {"_id": doc["_id"], "owner": owner},
                {"$set": {"nombre": nuevo_nombre, "precio": nuevo_precio}},
            )
            flash("Producto actualizado.")
            return redirect(url_for("lista_productos"))

        prod_view = ProductoView(
            _id=str(doc["_id"]),
            nombre=doc.get("nombre", ""),
            precio=doc.get("precio", 0.0),
        )
        return render_template(
            "producto_form.html",
            producto=prod_view,
            logged_in=current_user.is_authenticated,
        )

    # ---------- MODO SQL ----------
    try:
        pid = int(producto_id)
    except ValueError:
        flash("ID inválido.")
        return redirect(url_for("lista_productos"))

    # ✅ solo si es tuyo
    prod = (
        db.session.execute(
            db.select(Producto).where(
                Producto.id == pid,
                Producto.user_id == current_user.id,
                )
        )
        .scalar_one_or_none()
    )

    if prod is None:
        flash("No autorizado o el producto no existe.")
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


@app.route("/producto/eliminar/<producto_id>")
@login_required
def eliminar_producto(producto_id: str):
    # ---------- MODO MONGO ----------
    if DB_ENGINE == "mongo":
        if mongo_productos is not None:
            owner = current_user.get_id()
            try:
                res = mongo_productos.delete_one({"_id": ObjectId(producto_id), "owner": owner})
                if res.deleted_count == 0:
                    flash("No autorizado o el producto no existe.")
                else:
                    flash("Producto eliminado.")
            except Exception:
                flash("No se pudo eliminar el producto.")
        return redirect(url_for("lista_productos"))

    # ---------- MODO SQL ----------
    try:
        pid = int(producto_id)
    except ValueError:
        flash("ID inválido.")
        return redirect(url_for("lista_productos"))

    prod = (
        db.session.execute(
            db.select(Producto).where(
                Producto.id == pid,
                Producto.user_id == current_user.id,
                )
        )
        .scalar_one_or_none()
    )

    if prod is None:
        flash("No autorizado o el producto no existe.")
        return redirect(url_for("lista_productos"))

    db.session.delete(prod)
    db.session.commit()
    flash("Producto eliminado.")
    return redirect(url_for("lista_productos"))


# ============================
# ENTRYPOINT
# ============================
if __name__ == "__main__":
    app.run(host="0.0.0.0", port=5000, debug=True)