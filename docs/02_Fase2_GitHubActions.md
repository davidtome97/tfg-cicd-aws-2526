# FASE 2 — GitHub Actions (Sprint 1)

## Introducción general

En esta segunda fase del proyecto he creado y configurado los **pipelines de integración y despliegue continuos (CI/CD)** utilizando **GitHub Actions**.  
El objetivo principal era automatizar el proceso de compilación, pruebas, análisis de calidad y despliegue de la aplicación, para que cada cambio en el código pasara por un flujo controlado y reproducible antes de llegar a producción.

Con esta fase conseguí que el proyecto se construyera, analizara con **SonarQube** y se desplegara automáticamente en **AWS**, sin necesidad de hacerlo manualmente cada vez.  
Esto mejora la calidad del software, esto hace que podamos evitar errores humanos y permite mantener un mejor flujo de desarrollo mucho más ligero.

Dentro de esta fase cree y configuré tres workflows dentro de la carpeta `.github/workflows/`:
- **ci.yml** → encargado de la **integración continua (CI)**, donde se realizan las etapas de *build*, *test* y *análisis con SonarQube*.
- **deploy-ecs.yml** → encargado del **despliegue automático (CD)** en el entorno de **AWS ECS/EC2**.
- **only-develop-into-main.yml** → protección que asegura que solo se pueda fusionar la rama `develop` en `main`, garantizando un flujo de trabajo seguro y controlado.

Gracias a estos pipelines, mi repositorio quedó totalmente preparado para realizar todo el proceso de **CI/CD** sin hacerlo manualmente.  
Al terminar esta fase alcancé la **release v0.1**, que señaló el momento en que el sistema de **GitHub Actions** era completamente funcional y estable.

## Pipeline CI — Archivo `ci.yml`

En esta parte de la fase cree y configuré el pipeline de **Integración Continua (CI)** dentro del archivo `.github/workflows/ci.yml`.  
El objetivo de este workflow era automatizar las tareas principales del desarrollo: **compilar, ejecutar pruebas y analizar la calidad del código con SonarQube**.  
De esta forma, cada vez que subía un cambio o hacía un *push* a la rama `develop`, GitHub Actions ejecutaba el pipeline automáticamente.

### Estructura del workflow

El archivo `ci.yml` está dividido en varias partes o *stages* que se ejecutan en orden:

1. **Build** → En esta parte se prepara el entorno, se instala el proyecto y se realiza la compilación.  
   Aquí GitHub crea un contenedor con una imagen base e instala todas las dependencias necesarias con los comandos de instalación correspondientes.

2. **Test** → En este paso se ejecutan las pruebas automáticas del proyecto.  
   Esto sirve para comprobar que el código sigue funcionando correctamente y que no se ha roto nada con los últimos cambios.

3. **Sonar (Análisis de calidad)** → en esta parte, se envía el código a **SonarQube** para analizar la calidad, buscar errores, malas prácticas y medir la cobertura de los tests.  
   Gracias a este análisis podemos mantener un código más limpio y detectar posibles fallos antes del despliegue.

### Configuración de Secrets

Para que GitHub Actions pudiera conectarse con **SonarQube**, **AWS** y el servidor **EC2**, cree las variables secretas desde el apartado *Settings → Secrets and variables → Actions*.  
Los *Secrets* que añadí fueron los siguientes:

- `SONAR_HOST_URL` → URL del servidor de SonarQube.
- `SONAR_TOKEN` → Token de autenticación para poder subir los resultados del análisis.
- `AWS_ACCESS_KEY_ID` → Clave de acceso a AWS.
- `AWS_SECRET_ACCESS_KEY` → Clave secreta asociada.
- `AWS_REGION` → Región donde está desplegado el proyecto en AWS.
- `EC2_HOST` → Dirección pública o IP del servidor EC2.
- `EC2_USUARIO` → Usuario que se usa para conectarse al servidor.
- `EC2_LLAVE_SSH` → Clave privada SSH que permite el acceso al servidor remoto.

Gracias a estas variables, el pipeline puede autenticarse de forma segura sin mostrar credenciales en el código.

### Ejecución del workflow

Cada vez que hacía un commit o un *push* a la rama `develop`, el pipeline se activaba automáticamente.  
En GitHub, podía ver el progreso desde la pestaña **Actions**, donde aparecían los pasos *build*, *test* y *sonar* con su estado (✅ o ❌).  
Cuando todos los pasos pasaban correctamente, sabía que el código estaba listo para integrarse o desplegarse.

Este pipeline fue el primer gran paso hacia la automatización del proyecto, ya que garantizaba que todo el código subido al repositorio cumplía con los estándares de calidad antes de continuar con la siguiente etapa: el **despliegue (CD)**.

## Pipeline CD — Archivo `deploy-ecs.yml`

En esta parte hice el **despliegue automático (CD)** para que, cuando el código estuviera listo, se publicara una nueva imagen en **AWS ECR** y se actualizara el servicio en **AWS ECS** de forma segura y reproducible.

### ¿Qué hace este workflow?

1. **Se activa** cuando hay cambios en `main` (o cuando creo una release).
2. **Construye** la imagen Docker de la app y la **etiqueta** con el `GITHUB_SHA` (el commit).
3. **Inicia sesión** en **ECR**, **sube** la imagen al repositorio de contenedores.
4. **Actualiza** el servicio de **ECS** para que empiece a usar la nueva imagen.
5. **Espera** a que el servicio quede sin interrupciones.

De esta forma, cada cambio que llega a `main` pasa a producción sin pasos manuales y con trazabilidad.

### Variables y Secrets usados

En este workflow se utilizan principalmente los **Secrets de AWS y EC2**, que permiten autenticar la conexión al entorno remoto y desplegar la aplicación de forma segura.

- **AWS (Secrets)**
    - `AWS_ACCESS_KEY_ID`, `AWS_SECRET_ACCESS_KEY`, `AWS_REGION` → credenciales y región necesarias para usar la CLI de AWS desde GitHub Actions y desde la propia instancia EC2.

- **EC2 (Secrets)**
    - `EC2_HOST` → dirección pública o IP del servidor EC2 donde se hace el despliegue.
    - `EC2_USUARIO` → usuario SSH para acceder al servidor (por ejemplo, `ubuntu` o `ec2-user`).
    - `EC2_LLAVE_SSH` → clave privada SSH que permite la conexión segura desde el runner de GitHub a la instancia EC2.

Además, dentro del workflow se obtiene automáticamente algunas variables como:

- `REGISTRO` → se obtiene del paso `aws-actions/amazon-ecr-login`, que devuelve la URL del registro de ECR.
- `REPO` → usa el nombre del repositorio actual (`${{ github.repository }}`) para construir el nombre completo de la imagen Docker.
- `IMAGEN` → combina los valores anteriores para etiquetar y desplegar la imagen final en EC2.

Esto permite desplegar la aplicación directamente en el servidor EC2 mediante **Docker Compose**.  
Todo el proceso queda automatizado con GitHub Actions, manteniendo la seguridad gracias al uso de Secrets.

### Resumen de los pasos clave del job

1. **Checkout del repositorio**  
   Se descarga el código del proyecto con la acción oficial `actions/checkout@v4`, para que el runner tenga acceso a todos los archivos necesarios para construir la imagen Docker.

2. **Configurar credenciales de AWS**  
   Uso la acción `aws-actions/configure-aws-credentials@v4` para autenticarme en AWS con los Secrets (`AWS_ACCESS_KEY_ID`, `AWS_SECRET_ACCESS_KEY`, `AWS_REGION`).

3. **Login en Amazon ECR**  
   Con la acción `aws-actions/amazon-ecr-login@v2`, el runner inicia sesión en el registro de contenedores de AWS (ECR) para poder subir imágenes Docker.

4. **Verificar o crear el repositorio en ECR**  
   Antes de subir la imagen, el workflow comprueba si el repositorio existe y, si no, lo crea con `aws ecr create-repository`.  
   Esto garantiza que el entorno esté preparado para recibir la nueva imagen.

5. **Build & Push de la imagen Docker**  
   Se construye y sube la nueva imagen al repositorio ECR usando los siguientes comandos:
   ```
   docker build -t "$IMAGEN" .
   docker push "$IMAGEN"
   ```
    De esta forma, la versión más reciente del proyecto queda almacenada en ECR.

6.	**Copiar docker-compose.yml a la EC2**
   
   Con la acción `appleboy/scp-action@v0.1.7`, se transfiere el archivo infra/ec2/docker-compose.yml al directorio ~/app/ de la instancia EC2.
   Este archivo sirve para cómo se levantará la aplicación dentro del servidor.

7.	**Conectarse por SSH al servidor EC2 y desplegar**

    Usando `appleboy/ssh-action@v1.0.3`, el workflow accede al servidor EC2 y ejecuta los siguientes pasos:
    •	Instalar AWS CLI si no está disponible.
    •	Hacer login en ECR.
    •	Actualizar el archivo .env con la nueva imagen.
    •	Ejecutar docker compose pull y docker compose up -d.
    •	Eliminar imágenes antiguas con docker image prune -f.

8.	**Finalización del despliegue**

    Una vez completados todos los pasos, la aplicación queda actualizada y corriendo en el servidor EC2.
    Todo este proceso se ejecuta automáticamente desde GitHub Actions, sin necesidad de intervención manual.

### comprobar si funciona

- Revisé la pestaña **Actions** para ver cada paso ✅.
- Verifiqué en **ECR** que la imagen nueva estaba subida.
- Comprobé en **EC2** que el servicio hizo un **deployment** nuevo y quedó **STABLE**.
- También Con los datos de `EC2_HOST/USUARIO/LLAVE_SSH`, pude entrar por SSH y ver logs del agente ECS o de Docker si necesitaba revisar algo.

### Resultado

Con este pipeline conseguí que el despliegue a AWS fuera **automático y repetible**.  
Ahora, cada vez que llega código a `main` o creo una release, se publica una nueva imagen y **ECS** la pone en marcha sin tener que tocar nada a mano.

## Commits realizados en la Fase 2

Durante esta fase fui realizando varios commits que me permitieron construir y perfeccionar toda la parte de **integración y despliegue continuo (CI/CD)** con GitHub Actions.  
Cada uno de ellos representó un avance importante hasta dejar el sistema completamente funcional.

### Commit principal — CI/CD con GitHub Actions
En este commit configuré el archivo `.github/workflows/ci.yml` con las etapas de **build**, **test** y **análisis de calidad con SonarQube**.  
Aquí también añadí las variables necesarias (`SONAR_HOST_URL`, `SONAR_TOKEN`, `AWS_ACCESS_KEY_ID`, `AWS_SECRET_ACCESS_KEY`, `AWS_REGION`), para que el pipeline pudiera conectarse tanto a **SonarQube** como a **AWS** sin enseñar credenciales.  
Este fue el punto en el que GitHub Actions comenzó a ejecutar automáticamente la pipeline al hacer *push* a `develop`.

**Ejemplo de comandos utilizados:**
```
git add .github/workflows/ci.yml
git commit -m "ci: configurar pipeline CI con build, test y SonarQube"
git push origin develop
```

### Commit — Configuración de despliegue en AWS ECS/EC2

Después, añadí el archivo `.github/workflows/deploy-ecs.yml` para automatizar el despliegue de la aplicación en **AWS**.  
En este workflow configuré el inicio de sesión en **ECR**, la subida de la nueva imagen **Docker** y la actualización del servicio en **ECS**.  

Uno de los commits más importantes fue:

> `ci: añadir paso de login a ECR en EC2 y pasar credenciales AWS`  
> *(tag: v0.1.7)*

Este commit fue el momento en el que el despliegue comenzó a funcionar de manera completamente automática, conectándose a **AWS** y actualizando la aplicación sin intervención manual.

**Ejemplo de comandos utilizados:**
```
git add .github/workflows/deploy-ecs.yml
git commit -m "ci: añadir paso de login a ECR en EC2 y pasar credenciales AWS"
git push origin develop
```

### Commits de mejora y ajuste

Después del primer despliegue, realicé algunos ajustes adicionales para mejorar el proceso y optimizar la configuración del entorno en AWS.  
Estos commits me ayudaron a dejar el pipeline más estable y preparado para producción.

Algunos de los cambios más importantes fueron:

- `ci: corregido paso copiar docker-compose correctamente al EC2`
- `deploy(ec2): añadir Postgres y apuntar datasource a db:5432`

Con estos commits conseguí que la aplicación se desplegara correctamente con su base de datos conectada, evitando errores en la fase de arranque y garantizando un entorno totalmente funcional.

**Ejemplo de comandos utilizados:**
```
git add .
git commit -m "ci: corregido paso copiar docker-compose correctamente al EC2"
git commit -m "deploy(ec2): añadir Postgres y apuntar datasource a db:5432"
git push origin develop
```

## Release v0.1 — GitHub Actions funcional (CI/CD completo)

Al finalizar esta fase publiqué la **release v0.1**, que marcó el momento en que todo el sistema de **CI/CD con GitHub Actions** era completamente funcional.  
En ese punto el proyecto podía:

- Compilarse y analizarse automáticamente con **SonarQube**.
- Construir su imagen **Docker** y subirla a **AWS ECR**.
- Desplegar la nueva versión en **ECS/EC2** de forma automática.

A partir de ahí hice alguna mejora el flujo, generando versiones posteriores (`v0.1.6`, `v0.1.7`, `v0.1.8`) por ejemplo para optimizar el despliegue, en la configuración de Postgres y en los workflows de GitHub Actions.

## Objetivo alcanzado al finalizar la Fase 2

El principal objetivo de esta fase era **implantar un sistema de integración y despliegue continuo (CI/CD) completamente funcional** utilizando **GitHub Actions**, garantizando que el ciclo de vida del proyecto fuera automático, seguro y reproducible.

Al finalizar la Fase 2, conseguí:

- Que cada cambio subido al repositorio se **compilara, probara y analizara automáticamente** con SonarQube.
- Que las nuevas versiones se **construyeran como imágenes Docker** y se enviaran de forma segura a **AWS ECR**.
- Que el servicio se **actualizara automáticamente en AWS ECS/EC2**, desplegando la última versión sin necesidad de hacerlo manualmente.
- Que todo el flujo de desarrollo quedara documentado, versionado y protegido (solo se fusiona `develop` → `main`).

Gracias a esta fase, el proyecto pasó de tener un proceso manual a contar con una **pipeline CI/CD completa**, que asegura calidad, velocidad y trazabilidad.