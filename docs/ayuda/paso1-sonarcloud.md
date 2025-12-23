# Paso 1 – Configuración de SonarCloud

## 1. Objetivo de este paso

En este primer paso del asistente se configura la integración de la aplicación con SonarCloud. El objetivo es que el pipeline de CI/CD pueda:

- Analizar la calidad del código.
- Enviar los resultados al proyecto correspondiente en SonarCloud.
- Mostrar el estado de calidad en GitHub y GitLab.

Tras completar este paso quedarán definidas las siguientes variables secretas:

- `SONAR_HOST_URL`
- `SONAR_ORGANIZATION`
- `SONAR_PROJECT_KEY`
- `SONAR_TOKEN`

---

## 2. Requisitos previos

Antes de rellenar el formulario del Paso 1 es necesario disponer de:

1. Una cuenta en SonarCloud.
2. Una organización creada en SonarCloud (o utilizar una ya existente).
3. Un proyecto creado para el repositorio.
4. Permisos para generar tokens personales.

Si aún no se tiene un proyecto creado en SonarCloud, este paso puede completarse más adelante. No es obligatorio para continuar utilizando el asistente.

---

## 3. Datos necesarios de SonarCloud

### 3.1. `SONAR_HOST_URL`

- **Descripción:** URL de la instancia de SonarCloud.
- **Valor habitual:**
    - En la versión SaaS: `https://sonarcloud.io`
- **Uso:** Indica al scanner del pipeline a qué servidor debe conectarse.

En la mayoría de los casos se utiliza la URL `https://sonarcloud.io`.

---

### 3.2. `SONAR_ORGANIZATION`

- **Descripción:** identificador de la organización en SonarCloud.
- **Cómo obtenerlo:**
    1. Acceder a SonarCloud.
    2. Seleccionar la organización en la esquina superior izquierda.
    3. Revisar la URL, que tendrá un formato similar a:  
       `https://sonarcloud.io/organizations/mi-organizacion/projects`
    4. El valor ubicado entre `/organizations/` y `/projects` corresponde a `SONAR_ORGANIZATION`.

- **Ejemplo:**  
  `mi-organizacion`

---

### 3.3. `SONAR_PROJECT_KEY`

- **Descripción:** clave única del proyecto dentro de la organización.
- **Cómo obtenerlo:**
    1. Acceder al proyecto en SonarCloud.
    2. En la parte superior se muestra un texto similar a:  
       `Project key: mi-organizacion_mi-proyecto`
    3. Copiar el valor completo.

- **Ejemplo:**  
  `mi-organizacion_mi-proyecto`

---

### 3.4. `SONAR_TOKEN`

- **Descripción:** token utilizado por el pipeline para autenticarse en SonarCloud.
- **Cómo generarlo:**
    1. Acceder al perfil personal de SonarCloud (avatar en la parte superior derecha).
    2. Abrir la sección **My Account** y luego **Security** o **Tokens**.
    3. Seleccionar **Generate Token**.
    4. Introducir un nombre (por ejemplo `ci-cd-tfg`) y generar el token.
    5. Guardarlo en un lugar seguro, ya que solo se muestra una vez.

- **Ejemplo:**  
  `sqa_1234567890abcdef1234567890abcdef12345678`

Este token debe mantenerse en secreto y nunca incluirse en el repositorio. Debe definirse exclusivamente como variable secreta en GitHub o GitLab.

---

## 4. Rellenado del Paso 1 en el asistente

En la pantalla correspondiente al “Paso 1 – SonarCloud” se muestra un formulario con los siguientes campos:

- `SONAR_HOST_URL`
- `SONAR_ORGANIZATION`
- `SONAR_PROJECT_KEY`
- `SONAR_TOKEN`

Cada campo debe completarse con la información obtenida en los apartados anteriores.

Si falta algún dato, es posible dejar ese campo vacío y completarlo más adelante. El asistente solo emplea estos valores cuando el pipeline tenga activado el análisis con SonarCloud.

Aquí se debe incluir la captura del formulario del Paso 1 en el asistente.

---

## 5. Registro de estos datos en GitHub y GitLab

### 5.1. En GitHub (Repository Secrets)

1. Acceder al repositorio.
2. Ir a **Settings → Secrets and variables → Actions**.
3. En la sección **Repository secrets**, seleccionar **New repository secret**.
4. Crear las siguientes variables:

    - `SONAR_HOST_URL`
    - `SONAR_ORGANIZATION`
    - `SONAR_PROJECT_KEY`
    - `SONAR_TOKEN`

Aquí se debe incluir la captura de la pantalla de GitHub con las variables creadas.

---

### 5.2. En GitLab (CI/CD Variables)

1. Acceder al proyecto en GitLab.
2. Ir a **Settings → CI/CD → Variables**.
3. Seleccionar **Add variable**.
4. Crear las variables:

    - `SONAR_HOST_URL`
    - `SONAR_ORGANIZATION`
    - `SONAR_PROJECT_KEY`
    - `SONAR_TOKEN`

5. Activar las opciones:
    - **Protected**, si se desea limitar su uso a ramas protegidas.
    - **Masked**, para ocultar su valor en los logs.

Aquí se debe incluir la captura correspondiente de GitLab.

---

## 6. Relación con el pipeline CI/CD

Durante la ejecución de los pipelines tanto en GitHub Actions como en GitLab CI, estas variables se exponen como variables de entorno. El scanner de SonarCloud hace uso de ellas de la siguiente forma:

- Host: `${SONAR_HOST_URL}`
- Organización: `${SONAR_ORGANIZATION}`
- Clave de proyecto: `${SONAR_PROJECT_KEY}`
- Token: `${SONAR_TOKEN}`

Mientras los nombres coincidan con los utilizados en el pipeline, no es necesario realizar modificaciones adicionales en el código. Únicamente se deben definir correctamente las variables.

---

## 7. Lista de comprobación del Paso 1

Antes de avanzar al Paso 2 conviene revisar lo siguiente:

- [ ] La organización está creada en SonarCloud.
- [ ] El proyecto está creado en SonarCloud.
- [ ] Se dispone de `SONAR_ORGANIZATION`.
- [ ] Se dispone de `SONAR_PROJECT_KEY`.
- [ ] Se ha generado un `SONAR_TOKEN` válido.
- [ ] Las variables `SONAR_*` están definidas en GitHub y/o GitLab.
- [ ] Se ha completado el formulario del Paso 1 en el asistente.

Con todo lo anterior completado se puede continuar con el Paso 2, dedicado a la integración de SonarCloud con Git.