# Fase 1 — Preparación y estructura del proyecto

## Introducción

En esta primera fase me centré en dejar todo el proyecto correctamente estructurado y configurado a nivel técnico. Mi objetivo principal era establecer una buena fase sobre la que poder construir, en fases posteriores, los pipelines de integración y despliegue continuo (CI/CD).
Durante esta etapa trabajé tanto en la organización del repositorio y las ramas, como en la configuración de herramientas esenciales como Docker, SonarQube, AWS CLI y el entorno de desarrollo con IntelliJ IDEA. También planifiqué el flujo de trabajo mediante un tablero de sprints que me ayuda a controlar las tareas y el progreso del TFG.

## 1. Estructura del repositorio

Decidí organizar el proyecto de forma modular para separar claramente la aplicación, la automatización, la documentación y las utilidades. La estructura que definí en esta fase fue la siguiente:
```
/
├── app/                # Aplicación principal (Spring Boot)
├── cicd/               # Configuración y recursos para CI/CD
│   ├── github-actions/
│   ├── gitlab/
│   └── jenkins/
├── docs/               # Documentación del TFG y evidencias
│   ├── 00_EntornoTecnico.md
│   ├── 01_Fase1_Preparacion.md
│   └── img/
├── generator/          # Generador automático de workflows CI/CD
├── infra/              # Configuración de infraestructura (AWS EC2)
├── .github/            # Plantillas de issues y pull requests
├── docker-compose.yml
├── docker-compose-sonar.yml
├── README.md
└── .gitignore
```
Esta estructura me permite mantener una organización clara del proyecto y facilita el trabajo en las siguientes fases, ya que cada entorno (GitHub Actions, GitLab CI, Jenkins) tiene su propio espacio dentro de cicd/.
Además, la carpeta generator/ servirá más adelante para generar automáticamente los archivos de configuración de CI/CD, mientras que infra/ se destina a la infraestructura de AWS (EC2, ECS, etc.).
La carpeta docs/ se centra en toda la documentación técnica, lo que me permite tener un control total de cada fase y facilitar la trazabilidad del proyecto.

## 2. Flujo de ramas

Para gestionar el desarrollo, adopté un flujo de trabajo basado en Git Flow, ya que me permite trabajar con seguridad y mantener controladas las versiones del proyecto.
Creé las siguientes ramas principales:
```
•	main: contiene el código estable y listo para producción.
•	develop: se utiliza como rama de integración donde se fusionan las nuevas funcionalidades antes de una release.
•	feature/*: para el desarrollo de nuevas funcionalidades.
•	fix/*: para correcciones o ajustes puntuales.
•	chore/*: para tareas de mantenimiento o configuración.
```
Los commits y ramas iniciales que configuré fueron los siguientes:
```
git init
git add .
git commit -m "chore: estructura inicial TFG (app/, cicd/, docs/, generator/, .github, .gitignore)"
git branch -M main
git checkout -b develop
```
Y los primeros commits registrados en el repositorio fueron:
```
e157008 chore: estructura inicial TFG (app/, cicd/, docs/, generator/, .github, .gitignore)
a904a00 chore: elimina entorno virtual local del repo y lo ignora
d21ec65 añado carpetas base cicd/, docs/ y generator/ con .gitkeep
7f48f7c  añado README base en raíz y módulos del proyecto
225dd52 chore: inicializa rama develop
```

Con este flujo de ramas puedo controlar de forma ordenada los cambios y garantizar que las versiones que se integren en main pasen primero por la fase de desarrollo y validación. Este sistema será clave cuando los pipelines de CI/CD automaticen el testeo y despliegue de cada rama.

## 3. Planificación de sprints

Para organizar las tareas y el avance del proyecto utilicé Trello como herramienta principal de planificación.
Durante esta primera fase, el tablero se estructuró en cuatro columnas que representan el flujo de trabajo:

•	Por hacer: incluye las tareas planificadas pero aún no iniciadas.

•	En proceso: recoge las tareas que están actualmente en desarrollo.

•	Revisión: tiene las tareas que ya se han completado y están pendientes de validación o documentación.

•	Hecho: muestra las tareas finalizadas y verificadas correctamente.

Cada tarjeta del tablero representa una tarea concreta del proyecto. En la Fase 1, las más relevantes fueron la creación de la estructura del repositorio, la configuración inicial de Git, la instalación de Docker, la puesta en marcha de SonarQube y la configuración de la AWS CLI.
Todas las tarjetas incluyen checklists con subtareas específicas que permiten llevar un control detallado del progreso. Por ejemplo, en la configuración del entorno técnico se añadieron pasos como “instalar Docker Desktop y probar contenedor”, “levantar SonarQube en Docker”, o “verificar instalación de AWS CLI”.

Además, para clasificar y visualizar fácilmente el tipo de trabajo de cada tarea, utilicé etiquetas de color que agrupan las actividades por su naturaleza o área técnica.
Las etiquetas definidas en el tablero de Trello son:

•	🟩 Infraestructuras: tareas relacionadas con la configuración del entorno y servicios base.

•	🟨 Planificación: gestión de sprints, organización del trabajo y seguimiento del proyecto.

•	🟧 AWS: configuración y pruebas iniciales de herramientas de Amazon Web Services.

•	🟥 Bloqueos: incidencias o dependencias que detienen temporalmente el progreso.

•	🟪 CI/CD: tareas orientadas a la automatización de integración y despliegue continuo.

•	🟦 Documentación: elaboración y actualización de ficheros dentro de la carpeta /docs.

Para considerar una tarea como completada, establecí un criterio de “hecho” que incluye:
•	Haber completado todos los elementos del checklist.

•	Haber documentado la evidencia correspondiente dentro de la carpeta /docs.

•	Haber realizado el commit asociado y subido los cambios al repositorio remoto.

Este sistema de planificación me permitió mantener una visión global del progreso del proyecto, priorizar las tareas según su importancia técnica y asegurar que todas las actividades de la Fase 1 se completaran de forma estructurada y verificable.

## 4. Configuración del entorno técnico y herramientas

Durante esta fase configuré el entorno de trabajo completo, necesario para poder compilar, analizar y desplegar la aplicación.

### 4.1 IntelliJ IDEA

Utilizo IntelliJ IDEA como entorno principal para el desarrollo de la aplicación Java con Spring Boot. Es un entorno que me permite integrar Maven, ejecutar tests, revisar logs y gestionar el control de versiones desde un mismo sitio.
También empleo Visual Studio Code de forma complementaria para editar los archivos de configuración (.yml, .md, Dockerfile) y la documentación técnica del proyecto.

### 4.2 Lenguaje y framework

El proyecto está desarrollado en Java 17 con el framework Spring Boot 3.4.5, gestionado por Maven.
La elección de Spring Boot se debe a su capacidad para integrar fácilmente dependencias, crear servicios REST y funcionar sin complicaciones dentro de contenedores Docker, algo esencial para el despliegue automatizado en AWS.

### 4.3 Docker y Docker Compose

Utilizo Docker para asegurar que todo el entorno sea reproducible en cualquier equipo.

•	En docker-compose-sonar.yml definí un servicio de SonarQube Community, accesible en http://localhost:9000, que me permite ejecutar los análisis de calidad de código localmente.

•	En docker-compose.yml configuré la aplicación Spring Boot junto con una base de datos PostgreSQL 15, de modo que ambos servicios se levanten automáticamente en contenedores.

Este enfoque me garantiza coherencia entre los entornos de desarrollo y producción, y simplifica el futuro despliegue en AWS ECS.

### 4.4 SonarQube

Desplegué SonarQube localmente mediante Docker para realizar el análisis estático del código. El servicio se ejecuta en http://localhost:9000.

Configuré un token de autenticación en sonar-scanner.properties para poder ejecutar los análisis con el comando sonar-scanner.

Esta herramienta me permite mantener la calidad del código desde el inicio del proyecto y detectar posibles errores o malas prácticas antes de integrar cambios.

### 4.5 AWS CLI

Instalé y validé la AWS Command Line Interface (CLI) en macOS para poder interactuar con los servicios de Amazon Web Services desde la terminal.
En esta fase únicamente confirmé la instalación y el correcto funcionamiento de los comandos básicos (aws --version, aws configure, aws sts get-caller-identity), dejando la configuración de credenciales para fases posteriores, cuando el despliegue en AWS esté activo.

## 5. Commit inicial y configuración base

El commit inicial del proyecto fue:
chore: estructura inicial TFG (app/, cicd/, docs/, generator/, .github, .gitignore)

En este commit incorporé:

•	La estructura completa de carpetas del proyecto.

•	El archivo .gitignore adaptado a proyectos Java, Maven e IntelliJ IDEA.

•	Un README.md con la descripción inicial del TFG.

•	Las carpetas app/, docs/, cicd/, generator/ y .github/.

En commits posteriores añadí un README.md dentro de cada módulo y un .gitkeep para mantener la estructura en Git.
Las plantillas de ISSUE_TEMPLATE.md y PULL_REQUEST_TEMPLATE.md se añadieron más adelante, durante la Fase 2, al configurar GitHub Actions y los flujos de trabajo de control de cambios.

Este conjunto de commits iniciales me permitió tener una base de proyecto limpia, clara y versionada desde el primer momento, algo fundamental para un entorno de desarrollo profesional y automatizado.

⸻

## 6. Objetivo de la Fase 1

Esta fase me ha servido para organizar y comenzar a enlazar las diferentes configuraciones técnicas necesarias para tener una base de proyecto sólida.
He definido la estructura del repositorio, las ramas de trabajo, el entorno técnico y las herramientas que utilizaré durante todo el desarrollo.
Cada una de estas acciones tiene un propósito concreto:

•	La estructura del proyecto facilita el mantenimiento y la integración con las distintas plataformas de CI/CD (GitHub, GitLab y Jenkins).

•	El flujo de ramas me garantiza control sobre la evolución del código y estabilidad en las versiones.

•	La planificación en Trello me permite gestionar de forma visual el progreso y priorizar tareas.

•	La configuración de Docker, SonarQube y AWS CLI sienta las bases para las fases posteriores de integración y despliegue.

En resumen, esta fase ha sido esencial para establecer el punto de partida del TFG, asegurando que todos los componentes del entorno de desarrollo estén alineados y listos para automatizar los procesos de construcción, análisis y despliegue en las siguientes fases.