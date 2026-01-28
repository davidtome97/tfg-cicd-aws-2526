# Sistema de Gestión y Asistente de Despliegues CI/CD en AWS

Proyecto desarrollado como **Trabajo Fin de Grado (TFG)** orientado al **diseño, despliegue y automatización de aplicaciones en entornos Cloud**, aplicando buenas prácticas de **DevOps** y simulando un **entorno profesional real de producción**.

---

## 🚀 Descripción del proyecto

Esta aplicación web implementa un **asistente de despliegues** que permite **crear y configurar aplicaciones** y generar los recursos necesarios para su **despliegue automatizado en la nube**.

El objetivo del proyecto es integrar en un único sistema conceptos de:
- Cloud Computing
- Integración y despliegue continuo (CI/CD)
- Contenedores
- Gestión de configuración
- Calidad del código

El sistema está pensado con un **enfoque educativo y demostrativo**, simulando flujos reales utilizados en entornos profesionales.

---

## 🧭 Asistente de despliegues

El núcleo de la aplicación es un **wizard de despliegue por pasos**, que guía al usuario durante el proceso de creación y configuración de una aplicación.

Durante este proceso se pueden definir:
- Nombre y características de la aplicación
- Proveedor CI/CD
- Motor de base de datos
- Variables de entorno necesarias para el despliegue
- Recursos de configuración asociados

El asistente permite adaptar el despliegue a distintos escenarios sin modificar el código base del sistema.

---

## 🧩 Tecnologías utilizadas

### 🔧 Backend
- Java 17
- Spring Boot
- Spring MVC
- Spring Security
- Spring Data JPA
- Thymeleaf

### 🗄️ Bases de datos
- PostgreSQL
- MySQL
- MongoDB
- H2 (entorno de desarrollo)

### 🗄️ Migraciones
- Flyway (para bases de datos relacionales)

### 🐳 Contenedores y despliegue
- Docker
- Docker Compose
- AWS EC2

### 🔄 CI/CD
- GitHub Actions
- GitLab CI
- Jenkins

### 🔍 Calidad de código
- Sonar (análisis estático de código)

---

## 🗄️ Motores de base de datos soportados

Al crear una aplicación desde el asistente de despliegue, el usuario puede seleccionar el **motor de base de datos** que desea utilizar.

Motores disponibles:
- PostgreSQL
- MySQL
- MongoDB

Esta selección condiciona la configuración generada para el despliegue, tanto en entornos locales como remotos.

---

## 📦 Generación de recursos de despliegue

Una vez configurada la aplicación, el sistema permite obtener los recursos necesarios para su despliegue mediante dos opciones:

### 🔹 Proyecto demo
Se puede descargar un **proyecto de ejemplo**, que incluye:
- Estructura base de la aplicación
- Configuración de base de datos
- Archivos Docker
- Archivos de configuración CI/CD según el proveedor seleccionado

### 🔹 Archivos de configuración
Alternativamente, se pueden descargar **únicamente los archivos de configuración**, para integrarlos en una aplicación ya existente.

Esta opción permite reutilizar el asistente sin necesidad de utilizar una aplicación demo.

---

## 📐 Arquitectura del sistema

- Aplicación backend desarrollada con Spring Boot
- Arquitectura en capas (Controller, Service, Repository)
- Soporte para bases de datos relacionales y no relacionales
- Migraciones gestionadas con Flyway
- Contenedorización mediante Docker
- Despliegue en infraestructura Cloud (AWS EC2)
- Automatización mediante pipelines CI/CD
- Configuración separada por entornos

---

## 🔄 CI/CD y automatización

El sistema permite trabajar con distintos **proveedores de integración y despliegue continuo**, seleccionables durante el proceso de configuración.

Proveedores disponibles:
- GitHub Actions
- GitLab CI
- Jenkins

Los archivos generados permiten definir:
- Pipelines de despliegue
- Variables de entorno
- Uso de credenciales y secretos necesarios para el despliegue

---

## 🧬 Migraciones con Flyway

Para los motores de base de datos relacionales, el proyecto utiliza **Flyway** para la gestión del esquema de base de datos.

Flyway permite:
- Versionar cambios en la base de datos
- Mantener consistencia entre entornos
- Automatizar la creación y evolución del esquema

---

## 🔍 Calidad del código (Sonar)

El proyecto contempla la integración de **Sonar** dentro del flujo CI/CD para el análisis de calidad del código.

El análisis permite evaluar:
- Calidad del código
- Posibles errores
- Vulnerabilidades
- Deuda técnica

---

## ⚙️ Configuración por entornos

La aplicación utiliza distintos ficheros de configuración:

- `application.properties`
- `application-local.properties`
- `application-prod.properties`

Esto permite separar correctamente:
- Desarrollo local
- Producción
- Variables sensibles y credenciales

---

## 🐳 Ejecución con Docker

### Requisitos
- Docker
- Docker Compose

## 🔐 Seguridad

La aplicación implementa mecanismos básicos de seguridad orientados a proteger el acceso y la configuración de los despliegues gestionados por el sistema.

Las principales medidas de seguridad aplicadas son:

- Autenticación de usuarios mediante **Spring Security**
- Protección de rutas y recursos sensibles
- Separación de la configuración por entornos
- Gestión de variables sensibles mediante ficheros de configuración y variables de entorno
- Acceso controlado a las funcionalidades del asistente de despliegue

Estas medidas permiten simular un escenario real de seguridad habitual en aplicaciones backend desplegadas en entornos Cloud.

---

## 🎯 Objetivos del proyecto

Los objetivos principales del proyecto son:

- Diseñar un asistente de despliegues guiado
- Integrar herramientas reales de integración y despliegue continuo
- Soportar distintos motores de base de datos
- Automatizar procesos de despliegue en entornos Cloud
- Aplicar buenas prácticas DevOps
- Simular un entorno profesional de trabajo
- Consolidar conocimientos de Cloud Computing

---

## 📚 Contexto académico

Este proyecto se desarrolla como **Trabajo Fin de Grado**, con un enfoque práctico orientado a la aplicación de conceptos relacionados con:

- DevOps
- CI/CD
- Cloud Computing
- Contenedores
- Automatización

---

## 👨‍💻 Autor

David Tomé Arnaiz 
Proyecto desarrollado como **Trabajo Fin de Grado** en la Universidad de Burgos
Grado en Ingeniería Informática 

## License

This project is licensed under the MIT License.
See the LICENSE file for details.