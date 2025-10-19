# syntax=docker/dockerfile:1

########## Etapa de build (Maven) ##########
FROM maven:3.9.9-eclipse-temurin-17 AS build
WORKDIR /workspace

# Copiamos el repo
COPY . .

# Por si el wrapper no es ejecutable
RUN chmod +x mvnw || true

# Compila el modulo "app" y sus dependencias
# anado el repo local explicitamente (solo dentro del contenedor)
RUN ./mvnw -B -DskipTests -Dmaven.repo.local=/root/.m2/repository -pl app -am package

########## Etapa de runtime (JRE ligero) ##########
FROM eclipse-temurin:17-jre
WORKDIR /app

# Copio el JAR ya construido desde la etapa anterior
COPY --from=build /workspace/app/target/*.jar /app/app.jar

# Puerto por defecto de Spring Boot
EXPOSE 8080

ENTRYPOINT ["java", "-jar", "/app/app.jar"]