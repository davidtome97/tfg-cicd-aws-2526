# =====================
# BUILD (Maven)
# =====================
FROM maven:3.9.9-eclipse-temurin-17 AS build
WORKDIR /app

COPY pom.xml .
RUN mvn -B -q dependency:go-offline

COPY src ./src
RUN mvn -B -q -DskipTests clean package

# =====================
# RUNTIME (Java)
# =====================
FROM eclipse-temurin:17-jre
WORKDIR /app

# ✅ CA certs en RUNTIME (donde corre Java y conecta a Atlas)
RUN apt-get update && apt-get install -y --no-install-recommends ca-certificates \
  && update-ca-certificates \
  && rm -rf /var/lib/apt/lists/*

COPY --from=build /app/target/*.jar app.jar

EXPOSE 8081
ENTRYPOINT ["java", "-jar", "/app/app.jar"]