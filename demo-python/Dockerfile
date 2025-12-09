# Python base
FROM python:3.10-slim

WORKDIR /app

# Instalar dependencias del sistema (psycopg2 necesita gcc)
RUN apt-get update && apt-get install -y \
    gcc \
    libpq-dev \
    && rm -rf /var/lib/apt/lists/*

# Copiar requirements
COPY requirements.txt .

# Instalar dependencias Python
RUN pip install --no-cache-dir -r requirements.txt

# Copiar código de la app
COPY . .

EXPOSE 5000

CMD ["python", "main.py"]