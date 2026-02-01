import os
from pymongo import MongoClient
from config import MONGO_URI

if not MONGO_URI:
    raise RuntimeError("❌ No existe la variable MONGO_URI. Defínela en el .env o en GitHub/GitLab.")

# Conexión con Mongo Atlas
client = MongoClient(MONGO_URI)

# Base de datos (ej: "demo")
db = client["demo"]

# Colección
usuarios = db["usuarios"]

# Datos de ejemplo
documentos = [
    {"nombre": "admin", "correo": "admin@mongo.com", "password": "admin"},
    {"nombre": "david", "correo": "david@david.com", "password": "holamundo"},
]

# Insertar
result = usuarios.insert_many(documentos)

print("✅ Datos insertados en Mongo Atlas:")
print(result.inserted_ids)