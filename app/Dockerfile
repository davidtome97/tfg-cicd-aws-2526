# Usamos una imagen de Java como base
FROM openjdk:17-jdk-slim

# Directorio de trabajo dentro del contenedor
WORKDIR /app

# Copiamos el jar de la aplicación al contenedor
COPY target/SistemaGestionApp-0.0.1-SNAPSHOT.jar app.jar

# Puerto que expone la aplicación
EXPOSE 8080

# Comando para ejecutar el jar
ENTRYPOINT ["java", "-jar", "app.jar"]