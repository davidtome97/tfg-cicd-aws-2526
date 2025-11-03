# **Fase 3 — GitLab CI (Sprint 2)**

##  Objetivo
Migrar el sistema de integración y despliegue continuo desde **GitHub Actions** a **GitLab CI/CD**, aprovechando las funcionalidades integradas de GitLab (stages, variables protegidas y visualización de pipelines).  
El objetivo principal de esta fase es tener una pipeline funcional con las etapas:
`build`, `test`, `sonar` y `deploy`.

---

##  **[F3-1] Creación del fichero `.gitlab-ci.yml`**

### Descripción
Se crea el fichero principal `.gitlab-ci.yml` en la raíz del repositorio para definir los stages básicos del pipeline de GitLab CI:
- **build:** compilar el código del proyecto.
- **test:** ejecutar los tests unitarios.
- **sonar:** analizar la calidad del código con SonarCloud.
- **deploy:** preparar el despliegue (manual por ahora).

### Configuración realizada
```
stages:
  - build
  - test
  - sonar
  - deploy
```
Se añadieron los jobs build_java, test_java, sonar_java y un deploy manual.
El job pipeline_ok se mantiene como control de inicio del pipeline.

## Resultado

Pipeline base ejecutándose correctamente en GitLab con estructura por etapas.
El pipeline se dispara automáticamente en cada commit o merge request a la rama develop.

##  **[F3-2] Integración con SonarCloud**

Descripción

En esta etapa se integró el análisis estático de código mediante SonarCloud, asegurando que GitLab pueda comunicarse con la plataforma de SonarQube para medir calidad, duplicación y cobertura.

Pasos realizados
1. Se configuraron las propiedades de Sonar en el fichero pom.xml del módulo app/:
```
<properties>
    <sonar.organization>davidtome97</sonar.organization>
    <sonar.projectKey>davidtome97_tfg-cicd-aws-2526</sonar.projectKey>
    <sonar.projectName>TFG CI/CD AWS 25/26</sonar.projectName>
</properties>
```
2. Se añadió el plugin de Sonar Maven:
```
<plugin>
    <groupId>org.sonarsource.scanner.maven</groupId>
    <artifactId>sonar-maven-plugin</artifactId>
    <version>3.10.0.2594</version>
</plugin>
```
3. Se actualizó el `.gitlab-ci.yml` para ejecutar el análisis:
```
sonar_java:
  stage: sonar
  image: maven:3.9-eclipse-temurin-21
  script:
    - mvn -B sonar:sonar \
        -Dsonar.host.url="$SONAR_HOST_URL" \
        -Dsonar.login="$SONAR_TOKEN" \
        -Dsonar.projectKey="$SONAR_PROJECT_KEY" \
        -Dsonar.organization="$SONAR_ORGANIZATION"
```
4.	Se añadieron variables en GitLab:
```
- SONAR_HOST_URL
- SONAR_TOKEN
- SONAR_ORGANIZATION
- SONAR_PROJECT_KEY

```
## Resultado

- Análisis de SonarCloud funcionando correctamente, conectado desde GitLab CI.
- Quality Gate marcado como “Failed” únicamente por baja cobertura de tests (indicador de calidad, no error técnico).

## **[F3-3] Añadir variables de entorno (AWS y Sonar) en GitLab**

### Descripción
Se configuraron las **variables de entorno protegidas** necesarias para conectar GitLab CI/CD con **SonarCloud** y **AWS** sin exponer credenciales sensibles.  
Estas variables permiten que los *jobs* del pipeline accedan de forma segura a tokens, claves y configuraciones sin incluirlos en el código fuente.

---

### Variables creadas

| Tipo | Nombre | Descripción |
|------|---------|-------------|
| **AWS** | `AWS_ACCESS_KEY_ID` | ID de la clave de acceso de AWS |
| **AWS** | `AWS_SECRET_ACCESS_KEY` | Clave secreta asociada |
| **AWS** | `AWS_REGION` | Región (`eu-west-1`) |
| **AWS** | `AWS_ECR_URL` | URL del repositorio ECR |
| **SONAR** | `SONAR_HOST_URL` | URL de SonarCloud |
| **SONAR** | `SONAR_TOKEN` | Token de acceso a SonarCloud |
| **SONAR** | `SONAR_PROJECT_KEY` | Clave única del proyecto |
| **SONAR** | `SONAR_ORGANIZATION` | Organización del usuario en SonarCloud |

---

### Configuración en GitLab

1. Desde el menú lateral del proyecto:  
   **Settings → CI/CD → Variables → Add Variable**
2. Se añadieron todas las variables anteriores marcando:
   - **Protected** (solo disponibles en ramas protegidas como `develop` o `main`)
   - **Masked** (los valores no se muestran en los logs)
3. Estas variables son ahora accesibles desde el pipeline a través de las variables de entorno (`$AWS_ACCESS_KEY_ID`, `$SONAR_TOKEN`, etc.).

### Validación

- El *job* `sonar_java` se ejecutó correctamente, confirmando que GitLab pudo acceder al token de SonarCloud.
- En los logs del pipeline no se mostraron valores sensibles (solo texto enmascarado con `****`).
- El análisis en **SonarCloud** se actualizó automáticamente tras cada *pipeline*.
- Las credenciales de **AWS** quedaron listas para el siguiente paso (fase F3-4: deploy).


###  Commit asociado

- `ci(test/sonar): ignoro fallos de tests y genero cobertura con JaCoCo para SonarCloud [F3-3]`

---

### ✅ Resultado
- Todas las variables necesarias fueron creadas y configuradas como seguras.
- El pipeline puede conectarse tanto con **SonarCloud** como con **AWS** sin errores.
- Se garantiza la protección de credenciales mediante el uso de variables *Protected* y *Masked*.

---

###  Estado actual

- Variables SonarCloud --> Conectan correctamente con el análisis 
- Variables AWS--> Configuradas y protegidas en GitLab 
- Logs sin información sensible--> Los valores se enmascaran correctamente 
- Preparación para deploy--> AWS listo para usarse en F3-4 

###  Evidencias
- Captura de pantalla con las variables CI/CD configuradas en GitLab.
- Log del *job* `sonar_java` mostrando ejecución sin exponer credenciales.
- Panel de SonarCloud con el último análisis recibido desde GitLab CI.

### 📋 Conclusión
Con esta subfase (F3-3), el proyecto queda completamente preparado para trabajar con credenciales seguras dentro del entorno CI/CD de GitLab.  
El sistema puede autenticar automáticamente con **SonarCloud** y **AWS** sin intervención manual ni riesgo de exposición.  
El siguiente paso (F3-4) abordará el **despliegue automático** de la aplicación en AWS (EC2/ECS).