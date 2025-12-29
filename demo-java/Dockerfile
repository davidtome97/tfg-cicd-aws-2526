# syntax=docker/dockerfile:1.7

# =====================
# BUILD (Maven)
# =====================
FROM maven:3.9.9-eclipse-temurin-17 AS build
WORKDIR /app

# 1) Copia pom primero para aprovechar cache
COPY pom.xml ./

# 2) Pre-descarga dependencias (no rompas el build si un repo falla en go-offline)
RUN --mount=type=cache,target=/root/.m2 \
    mvn -B -e dependency:go-offline || true

# 3) Copia código
COPY src ./src

# 4) Compila de verdad (aquí sí debe fallar si hay un problema real)
RUN --mount=type=cache,target=/root/.m2 \
    mvn -B -e -DskipTests clean package

# =====================
# RUNTIME (Java)
# =====================
FROM eclipse-temurin:17-jre
WORKDIR /app

# CA certs en RUNTIME (donde corre Java)
RUN apt-get update && apt-get install -y --no-install-recommends ca-certificates \
  && update-ca-certificates \
  && rm -rf /var/lib/apt/lists/*

COPY --from=build /app/target/*.jar /app/app.jar

EXPOSE 8081
ENTRYPOINT ["java", "-jar", "/app/app.jar"]