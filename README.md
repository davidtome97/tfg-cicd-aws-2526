# TFG CI/CD AWS 25/26

Este repositorio forma parte de mi Trabajo de Fin de Grado. El objetivo principal es diseñar e implementar un sistema completo de Integración Continua y Despliegue Continuo (CI/CD) que automatice el ciclo de vida de una aplicación web, desde la compilación y los tests hasta el análisis de calidad, la generación de imágenes Docker y el despliegue automático en AWS.

Además del sistema CI/CD, he desarrollado un generador automático que crea workflows completos tanto para GitHub Actions como para GitLab CI, incluyendo también plantillas para Jenkins.

---

## Estructura del repositorio

### `.github/workflows/`
Contiene los pipelines utilizados durante el proyecto:
- **ci.yml** – Pipeline principal con build, tests y análisis.
- **deploy-ecs.yml** – Despliegue automático a AWS EC2/ECR.
- **generated-ci.yml** – Workflow generado automáticamente por el generador.
- **only-develop-into-main.yml** – Regla para proteger la rama `main` y permitir PR solo desde `develop`.

Además, la carpeta `.github/` incluye:
- **ISSUE_TEMPLATE.md**
- **PULL_REQUEST_TEMPLATE.md**

### `app/`
Aplicación principal del proyecto (SistemaGestionApp), basada en Java/Spring Boot, incluyendo todo el código fuente, dependencias, Maven Wrapper, Dockerfile y configuración Sonar.

Archivos relevantes:
- **Dockerfile**
- **docker-compose.yml**
- **docker-compose-sonar.yml**
- **Jenkinsfile**
- **pom.xml**
- **sonar-project.properties**

### `generator/`
Herramienta desarrollada en Python que genera workflows CI/CD y un PDF con la lista de variables necesarias.

Contiene:
- `github_generator.py` – Generador específico de GitHub Actions.
- `gitlab_generator.py` – Generador específico de GitLab CI.
- `compartido.py` – Lógica común (Sonar, AWS, base de datos, creación de PDFs).
- `main.py` – Punto de entrada del generador.
- `templates/` – Plantillas Jinja de los CI.
- `requirements.txt` – Dependencias del generador.
- `workflow-github.pdf` – Ejemplo de PDF generado.

### `docs/`
Documentación del TFG organizada por fases:
- **00_EntornoTecnico.md** – Descripción del entorno y herramientas.
- **01_Fase1_Preparacion.md**
- **02_Fase2_GitHubActions.md**
- **03_Fase3_GitLabCI.md**
- **img/** – Imágenes que se incluyen en la documentación.

---

## Tecnologías principales

- Docker y docker-compose
- AWS (ECR, EC2, IAM)
- GitHub Actions
- GitLab CI/CD
- Jenkins
- SonarCloud
- Python (Jinja2, ReportLab)
- Java / Spring Boot

---

## Objetivos del proyecto

- Diseñar un pipeline CI/CD completo.
- Automatizar build, tests y análisis de calidad.
- Generar y subir imágenes Docker a ECR.
- Desplegar la aplicación en una EC2.
- Comparar GitHub, GitLab y Jenkins.
- Crear un generador automático que facilite la configuración de pipelines.

---

## Autor

David Tomé  
Grado en Ingeniería Informática  
Curso 2025/2026