# 00 — Entorno Técnico

## 🐳 Docker
He instalado y configurado correctamente **Docker Desktop** en mi equipo (macOS).  
La versión que tengo instalada es:

```
Docker version 28.0.4, build b8034c0
```

Después de iniciar Docker, comprobé que todo funcionaba correctamente ejecutando:

```
docker run hello-world
```

El comando `docker ps` muestra los contenedores activos, y en mi caso aparece el contenedor de **SonarQube** funcionando sin problemas.

---

## 🔍 SonarQube
Para realizar el análisis de calidad del código utilicé **SonarQube**, que desplegué fácilmente con **Docker Compose** usando el siguiente comando:

```
docker compose -f docker-compose-sonar.yml up -d
```

El servicio queda disponible de forma local en la dirección:  
👉 [http://localhost:9000](http://localhost:9000)

Comprobé que el servicio se inicia correctamente y que el panel de administración carga sin errores.  
Guardé una captura de referencia en:  
`docs/img/sonarqube_dashboard.png`

---

## 📊 SonarScanner
El análisis del proyecto lo realicé con **SonarScanner**, configurado para conectarse automáticamente a mi instancia local de SonarQube.

Para lanzar el análisis utilicé el comando:
```
sonar-scanner
```

El proceso se ejecutó correctamente y el resultado final fue:  
✅ **EXECUTION SUCCESS**

También guardé la evidencia de la ejecución en:  
`docs/img/sonar_scanner_ok.png`

---

## 💻 IDE
Estoy utilizando **Visual Studio Code** como entorno de desarrollo, con las extensiones necesarias para trabajar de forma cómoda y organizada.  
Entre las más importantes tengo:
- Extensión de Git para control de versiones.
- Integración con Docker.
- Soporte para YAML y Markdown.
- Herramientas para análisis estático y formateo automático de código.

---

## 🧩 Notas
- Configuré el **token de autenticación** de SonarQube de forma permanente en:
  ```
  /opt/homebrew/Cellar/sonar-scanner/7.1.0.4889/libexec/conf/sonar-scanner.properties
  ```
- De esta forma, ya no tengo que exportar el token manualmente cada vez que ejecuto `sonar-scanner`.
- Todo el entorno técnico ha quedado configurado, probado y documentado correctamente, cumpliendo los objetivos de la **FASE 1** del proyecto.