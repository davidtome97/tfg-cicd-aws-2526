# Demo Python – Flask CRUD + Login

Esta aplicación es una demo ligera en Python/Flask que incluye:

✔ Registro de usuarios  
✔ Inicio y cierre de sesión  
✔ CRUD de productos (crear, editar, eliminar)  
✔ SQLite por defecto  
✔ Plantillas Jinja2 y estilos CSS  
✔ Preparada para ser parametrizada por el generador de proyectos del TFG

## Requisitos

Python 3.10+
pip instalado

## Instalación

pip install -r requirements.txt

## Ejecutar

python main.py

## Estructura del proyecto

demo-python/
│── main.py
│── requirements.txt
│── config.py (en la versión parametrizable)
│── static/
│── templates/

## Variables de entorno (versión parametrizable)

SECRET_KEY
DB_URL