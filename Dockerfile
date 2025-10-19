# Etapa base: imagen ligera de Java
FROM openjdk:17-jdk-slim

# Carpeta de trabajo
WORKDIR /app

# Copiamos el jar del módulo app (lo compila Maven antes del deploy)
# Uso un wildcard para no depender del nombre exacto del jar
COPY app/target/*.jar app.jar

# Puerto de la aplicación (Spring Boot usa 8080)
EXPOSE 8080

# Comando de inicio
ENTRYPOINT ["java", "-jar", "app.jar"]