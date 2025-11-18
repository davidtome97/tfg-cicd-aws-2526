# Fase 3 — GitLab CI (Sprint 2)

## Objetivo

En esta fase migré el sistema de integración y despliegue continuo desde GitHub Actions a GitLab CI/CD.
El objetivo principal fue crear una pipeline completa y modular compuesta por las etapas:
```
stages:
•	build
•	test
•	sonar
•	deploy
```
Además, configuré variables protegidas, integré SonarCloud y dejé preparada la base para el despliegue automático en AWS (fase F3-4).

# [F3-1] Creación del fichero .gitlab-ci.yml y pipeline base

## Descripción

Comencé creando el fichero principal .gitlab-ci.yml en la raíz del repositorio. En él definí la estructura base del pipeline: los stages, las reglas del workflow y varios jobs iniciales.

El pipeline está configurado para ejecutarse tanto en pushes como en merge requests. Para evitar errores en SonarCloud, restringí los análisis de Sonar únicamente a commits en ramas principales y tags.

Configuración realizada

Declaración de etapas:
```
stages:
•	build
•	test
•	sonar
•	deploy
```

## Workflow global

En el bloque workflow: definí las reglas para controlar cuándo se ejecuta la pipeline:
•	Se ejecuta en merge requests (solo con build y test).
•	Se ejecuta en cualquier push a una rama.
•	Si no se cumple ninguna condición, no se ejecuta nada.

## Job sentinela

Añadí un pequeño job inicial para validar que el pipeline arranca correctamente:

```
     - pipeline_ok:
     - stage: build
     script:
     - echo "Pipeline iniciado correctamente"
```

## Build y test para proyectos Java

Implementé:
•	build_java
•	test_java
•	sonar_java

Estos jobs detectan automáticamente si existe un pom.xml y ejecutan Maven en función del stage.

## Build y test para proyectos Node.js

Creé los jobs:
•	build_node
•	test_node
•	sonar_node

Cada uno se ejecuta únicamente si existe un package.json.

## Deploy manual placeholder

Incluí un job deploy manual como punto inicial antes de integrar AWS:
```
deploy:
stage: deploy
when: manual
script:
     - echo "Deploy pendiente — se implementará en F3-4"
```
## Resultado

Con esta configuración logré tener una pipeline totalmente funcional y ejecutándose en GitLab.
•	Ejecución automática en develop, main y merge requests.
•	Compilación y test automatizados para Java y Node.js.
•	Jobs organizados por etapas y dependencias.
•	Soporte multi-lenguaje sin duplicación de pipelines.

# [F3-2] Integración con SonarCloud

## Descripción

Integré el análisis estático de código con SonarCloud para evaluar métricas de calidad, seguridad y cobertura.

La configuración de SonarCloud se realizó tanto desde el fichero Maven pom.xml como desde las variables de entorno de GitLab.

## Pasos realizados

### 1. Añadí las propiedades de Sonar en el pom.xml:

```
<properties>
    <sonar.organization>davidtome97</sonar.organization>
    <sonar.projectKey>davidtome97_tfg-cicd-aws-2526</sonar.projectKey>
    <sonar.projectName>TFG CI/CD AWS 25/26</sonar.projectName>
</properties>
```

### 2. Incluí el plugin de Sonar para Maven:

```
<plugin>
    <groupId>org.sonarsource.scanner.maven</groupId>
    <artifactId>sonar-maven-plugin</artifactId>
    <version>3.10.0.2594</version>
</plugin>
```
### 3. Añadí el job sonar_java en .gitlab-ci.yml:

Lo configuré para ejecutarse únicamente en pushes a ramas principales (develop y main) o en tags, evitando su ejecución en merge requests.
```
      sonar_java:
      stage: sonar
      image: maven:3.9-eclipse-temurin-21
      rules:
      - if: '$CI_PIPELINE_SOURCE == "push" && ($CI_COMMIT_BRANCH == "develop" || $CI_COMMIT_BRANCH == "main")'
      exists: [pom.xml]
        - if: '$CI_COMMIT_TAG'
        exists: [pom.xml]
        - when: never
        script:
        - mvn -B clean verify -DfailIfNoTests=false -Dmaven.test.failure.ignore=true
        - mvn -B sonar:sonar \
        -Dsonar.host.url="$SONAR_HOST_URL" \
        -Dsonar.login="$SONAR_TOKEN" \
        -Dsonar.organization="$SONAR_ORGANIZATION" \
        -Dsonar.projectKey="$SONAR_PROJECT_KEY"
```

## Resultado
•	El análisis de SonarCloud funciona correctamente desde GitLab CI.
•	La comunicación entre GitLab y SonarCloud quedó establecida mediante variables protegidas.
•	El Quality Gate falló únicamente por baja cobertura de tests, pero no por errores técnicos.

# [F3-3] Configuración de variables de entorno (AWS y SonarCloud)

## Descripción

Para garantizar un pipeline seguro y sin exposición de credenciales, configuré todas las variables necesarias en GitLab CI/CD como secretos protegidos y enmascarados.

Estas variables permiten:
•	Autenticación automática con AWS.
•	Conexión segura con SonarCloud.
•	Configuración del pipeline sin almacenar información sensible en el repositorio.

## Variables Creadas

| Categoría   | Variable              | Descripción               |
|-------------|-----------------------|---------------------------|
| AWS         | AWS_ACCESS_KEY_ID     | ID de acceso a AWS        |
| AWS         | AWS_SECRET_ACCESS_KEY | Clave secreta             |
| AWS         | AWS_REGION            | Región (eu-west-1)        |
| AWS         | AWS_ECR_URL           | URL del repositorio ECR   |
| SonarCloud  | SONAR_HOST_URL        | URL de SonarCloud         |
| SonarCloud  | SONAR_TOKEN           | Token de acceso           |
| SonarCloud  | SONAR_PROJECT_KEY     | Clave única del proyecto  |
| SonarCloud  | SONAR_ORGANIZATION    | Organización del usuario  |

## Configuración aplicada

Todas las variables fueron creadas desde:

### Settings → CI/CD → Variables

Y se configuraron como:
•	Masked
•	Protected

## Validación
•	Los análisis de SonarCloud se ejecutaron correctamente.
•	Las credenciales de AWS quedaron listas para la fase de despliegue.
•	En los logs no aparece ninguna información sensible.

## Resultado

El pipeline quedó completamente preparado para trabajar con SonarCloud y AWS sin exponer datos sensibles.

# [F3-4] Build & Push a ECR + Deploy automático en EC2

## Descripción

En esta subfase extendí el pipeline para que GitLab pudiera construir la imagen Docker del proyecto, subirla al repositorio de Amazon ECR y desplegar automáticamente la aplicación en una instancia EC2 mediante SSH y Docker Compose.

Desarrollé dos jobs nuevos:
•	build_and_push → Compila la imagen Docker y la publica en ECR.
•	deploy_ec2 → Despliega la aplicación en la instancia EC2 usando docker-compose.


# 1. Build & Push a ECR

## Objetivo

Construir la imagen Docker del proyecto y publicarla en Amazon ECR utilizando el commit hash como tag.

## Configuración destacada

### Imagen Docker + Docker-in-Docker

Utilicé la imagen oficial docker:24 junto al servicio docker:dind para poder ejecutar comandos Docker dentro del pipeline.

### Instalación dinámica de AWS CLI

Para evitar imágenes personalizadas, instalé AWS CLI dentro del job:
```
apk add --no-cache bash curl jq python3 py3-pip unzip
python3 -m venv /tmp/awscli && . /tmp/awscli/bin/activate
pip install awscli
```


### Creación automática del repositorio ECR

Agregué un bloque que crea el repositorio si no existe, evitando fallos en la primera ejecución:
```
if ! aws ecr describe-repositories --repository-names "$ECR_REPOSITORY"; then
aws ecr create-repository --repository-name "$ECR_REPOSITORY"
fi
```

### Build y push de Docker
```
docker build -t "$REGISTRY_HOST/$ECR_REPOSITORY:$IMAGE_TAG" .
docker tag "$REGISTRY_HOST/$ECR_REPOSITORY:$IMAGE_TAG" "$REGISTRY_HOST/$ECR_REPOSITORY:latest"
docker push "$REGISTRY_HOST/$ECR_REPOSITORY:$IMAGE_TAG"
docker push "$REGISTRY_HOST/$ECR_REPOSITORY:latest"
```

## Resultado

El pipeline ahora es capaz de:
•	Autenticarse contra ECR
•	Crear el repo automáticamente si no existe
•	Construir la imagen Docker del proyecto
•	Subir la imagen con dos tags: commit y latest

Esto habilita un flujo de CI estable y autónomo.

# 2. Deploy automático en EC2

## Objetivo

Desplegar la última imagen subida a ECR dentro de una instancia EC2.

## Pasos realizados

### Instalación de AWS CLI y herramientas SSH
```
apk add --no-cache bash curl jq openssh-client python3 py3-pip
python3 -m venv /tmp/awscli && . /tmp/awscli/bin/activate
pip install awscli
```

### Carga segura de la clave SSH desde variables GitLab

Añadí compatibilidad para claves almacenadas como archivo o texto plano.
```
printf "%s" "$EC2_SSH_KEY" > ~/.ssh/id_rsa
chmod 600 ~/.ssh/id_rsa
```

### Transferencia del docker-compose

Uso de SCP para enviar el fichero actualizado al servidor:
```
scp docker-compose.yml user@host:/home/user/app/
```

### Ejecución remota del despliegue

En la instancia EC2:
•	Inicio de sesión en ECR
•	Pull de la nueva imagen
•	Levantado del servicio con docker-compose

```
docker compose pull
docker compose up -d --remove-orphans
docker image prune -f
```

## Resultado

Conseguí un despliegue automatizado, seguro y totalmente integrado en el pipeline de GitLab.

## Conclusión de la Fase 3

En esta fase dejé completamente migrado el sistema CI/CD a GitLab. Logré:
•	Crear un pipeline robusto dividido por etapas.
•	Añadir soporte tanto para proyectos Java como Node.js.
•	Integrar análisis de calidad con SonarCloud.
•	Gestionar credenciales de forma segura mediante variables protegidas.
•	Configurar build y publicación de imágenes Docker en Amazon ECR.
•	Automatizar totalmente el despliegue en una instancia EC2.

La Fase 3 finaliza con una pipeline completa, funcional y preparada para trabajar en un entorno de despliegue continuo real.




