package com.sistemagestionapp.service;

import com.sistemagestionapp.model.Aplicacion;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * Servicio que utilizo para generar un ZIP con un proyecto Java de ejemplo
 * (la carpeta demo-java) a partir de los datos de una {@link Aplicacion}.
 *
 * Ahora:
 *  - Copio la carpeta demo-java a un directorio temporal, ignorando target/.
 *  - Genero el pipeline de CI/CD según el proveedor (GitHub desde plantilla / GitLab desde fichero).
 *  - Copio el docker-compose adecuado según el motor de base de datos y lo renombro a docker-compose.yml.
 *  - Creo un fichero app-config.properties con los datos básicos de la aplicación.
 *  - Comprimo la carpeta en un fichero .zip.
 *
 * Plantillas esperadas en classpath:
 *  - static/ficherosjava/docker-compose-mysql.yml
 *  - static/ficherosjava/docker-compose-postgres.yml
 *  - static/ficherosjava/docker-compose-mongo.yml
 *  - static/ficherosjava/github-workflow.template.yml
 */
@Service
public class ProyectoZipService {

    public Path generarProyectoJava(Aplicacion aplicacion) throws IOException {
        System.out.println("⚙️ [ZIP] Iniciando generación del ZIP para aplicación: " + aplicacion.getNombre());
        System.out.println("🔥🔥🔥 [ZIP] Estoy usando ProyectoZipService NUEVO (con workflow plantilla) 🔥🔥🔥");
        // Estoy en el módulo "app". La raíz del repo es el padre.
        Path dirActual = Paths.get("").toAbsolutePath();
        System.out.println("📁 Directorio actual (módulo app): " + dirActual);

        Path raizRepo = dirActual.getParent();
        if (raizRepo == null) {
            throw new IllegalStateException("No se ha podido resolver la raíz del repositorio (parent de " + dirActual + ")");
        }

        Path origenDemoJava = raizRepo.resolve("demo-java");
        System.out.println("📁 Buscando carpeta demo-java en: " + origenDemoJava);

        if (!Files.exists(origenDemoJava) || !Files.isDirectory(origenDemoJava)) {
            throw new IllegalStateException("No se ha encontrado la carpeta demo-java en: " + origenDemoJava);
        }

        // Directorio temporal donde montaré el proyecto antes de comprimirlo
        Path tempDir = Files.createTempDirectory("tfg-demojava-");
        System.out.println("📂 Directorio temporal de trabajo: " + tempDir);

        // Nombre de carpeta/zip basado en el nombre de la aplicación
        String nombreSanitizado = sanitizarNombre(aplicacion.getNombre(), "demo-java-proyecto");

        // Carpeta raíz del proyecto dentro del directorio temporal
        Path carpetaProyecto = tempDir.resolve(nombreSanitizado);

        // 1) Copio demo-java dentro de carpetaProyecto (ignorando target/)
        System.out.println("📦 Copiando contenido de demo-java a: " + carpetaProyecto);
        copiarCarpeta(origenDemoJava, carpetaProyecto);
        System.out.println("✅ Copia de demo-java completada");

        // 2) Pipeline CI/CD (GitHub desde plantilla / GitLab desde fichero)
        copiarPipelineCiCd(carpetaProyecto, aplicacion, raizRepo);

        // 3) docker-compose.yml según motor BD (desde classpath)
        copiarDockerCompose(carpetaProyecto, aplicacion);

        // 4) app-config.properties
        crearFicheroConfiguracion(carpetaProyecto, aplicacion);

        // 5) Comprimir carpetaProyecto en un zip
        Path zipPath = tempDir.resolve(nombreSanitizado + ".zip");
        System.out.println("🗜️  Comprimiendo carpeta en ZIP: " + zipPath);
        comprimirCarpetaEnZip(carpetaProyecto, zipPath);
        System.out.println("✅ ZIP generado correctamente en: " + zipPath);

        return zipPath;
    }

    // ==========================================================
    // COPIA PROYECTO BASE (IGNORANDO target/)
    // ==========================================================
    private void copiarCarpeta(Path origen, Path destino) throws IOException {
        Path targetDir = origen.resolve("target");

        Files.walk(origen).forEach(sourcePath -> {
            try {
                // Ignoro todo lo que cuelga de target/
                if (sourcePath.startsWith(targetDir)) {
                    return;
                }

                Path relative = origen.relativize(sourcePath);
                Path targetPath = destino.resolve(relative);

                if (Files.isDirectory(sourcePath)) {
                    Files.createDirectories(targetPath);
                } else {
                    Files.createDirectories(targetPath.getParent());
                    Files.copy(sourcePath, targetPath, StandardCopyOption.REPLACE_EXISTING);
                }
            } catch (IOException e) {
                throw new RuntimeException("Error copiando fichero: " + sourcePath, e);
            }
        });
    }

    // ==========================================================
    // PIPELINE CI/CD
    // ==========================================================
    private void copiarPipelineCiCd(Path carpetaProyecto, Aplicacion aplicacion, Path raizRepo) throws IOException {
        if (aplicacion.getProveedorCiCd() == null) {
            System.out.println("ℹ️ [ZIP] Proveedor CI/CD no definido. No se copiará/generará ningún pipeline.");
            return;
        }

        String proveedor = aplicacion.getProveedorCiCd().name();
        System.out.println("📦 [ZIP] Preparando pipeline para proveedor: " + proveedor);

        if ("GITHUB".equalsIgnoreCase(proveedor)) {
            // ✅ Generar workflow GitHub desde plantilla (Opción A)
            generarWorkflowGithubDesdePlantilla(carpetaProyecto, aplicacion);

        } else if ("GITLAB".equalsIgnoreCase(proveedor)) {
            // (Por ahora) copiamos el fichero existente del repo
            Path origenGitlab = raizRepo.resolve(".gitlab-ci.yml");
            if (!Files.exists(origenGitlab)) {
                throw new IllegalStateException("No se ha encontrado el fichero .gitlab-ci.yml en: " + origenGitlab);
            }

            Path destinoGitlab = carpetaProyecto.resolve(".gitlab-ci.yml");
            Files.copy(origenGitlab, destinoGitlab, StandardCopyOption.REPLACE_EXISTING);
            System.out.println("✅ [ZIP] Pipeline de GitLab copiado a: " + destinoGitlab);

        } else {
            System.out.println("ℹ️ [ZIP] Proveedor CI/CD no soportado todavía: " + proveedor);
        }
    }

    /**
     * Genera .github/workflows/generated-ci.yml a partir de una plantilla en resources:
     *   static/ficherosjava/github-workflow.template.yml
     *
     * Placeholders soportados:
     *  - __APP_NAME__
     */
    private void generarWorkflowGithubDesdePlantilla(Path carpetaProyecto, Aplicacion aplicacion) throws IOException {
        String appName = sanitizarNombre(aplicacion.getNombre(), "mi-proyecto");

        String template;
        try {
            template = leerRecursoClasspathComoString("static/ficherosjava/github-workflow.template.yml");
        } catch (Exception e) {
            // fallback por si el empaquetado lo deja sin "static/"
            template = leerRecursoClasspathComoString("ficherosjava/github-workflow.template.yml");
        }

        String workflow = template
                .replace("__APP_NAME__", appName);

        Path workflowsDir = carpetaProyecto.resolve(".github").resolve("workflows");
        Files.createDirectories(workflowsDir);

        Path destinoWorkflow = workflowsDir.resolve("generated-ci.yml");
        Files.writeString(destinoWorkflow, workflow, StandardCharsets.UTF_8,
                StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);

        System.out.println("✅ [ZIP] Workflow GitHub generado en: " + destinoWorkflow);
    }

    // ==========================================================
    // DOCKER-COMPOSE (desde classpath) -> docker-compose.yml
    // ==========================================================
    private void copiarDockerCompose(Path carpetaProyecto, Aplicacion aplicacion) throws IOException {
        if (aplicacion.getTipoBaseDatos() == null) {
            System.out.println("ℹ️ [ZIP] Tipo de base de datos no definido. No se copiará docker-compose.yml.");
            return;
        }

        String tipoBd = aplicacion.getTipoBaseDatos().name();
        System.out.println("📦 [ZIP] Seleccionando docker-compose para tipo BD: " + tipoBd);

        String nombrePlantilla;
        switch (tipoBd.toUpperCase()) {
            case "MYSQL":
                nombrePlantilla = "docker-compose-mysql.yml";
                break;
            case "POSTGRES":
            case "POSTGRESQL":
                nombrePlantilla = "docker-compose-postgres.yml";
                break;
            case "MONGO":
            case "MONGODB":
                nombrePlantilla = "docker-compose-mongo.yml";
                break;
            default:
                System.out.println("ℹ️ [ZIP] Tipo de BD no soportado todavía: " + tipoBd + ". No se copiará docker-compose.yml.");
                return;
        }

        String resourcePath = "static/ficherosjava/" + nombrePlantilla;

        // Puerto externo elegido por el usuario (si no hay, default 8081)
        String appPort = (aplicacion.getPuertoAplicacion() == null || aplicacion.getPuertoAplicacion() <= 0)
                ? "8081"
                : String.valueOf(aplicacion.getPuertoAplicacion());

        // Leer plantilla
        String template;
        try (InputStream is = getClass().getClassLoader().getResourceAsStream(resourcePath)) {
            if (is == null) {
                throw new IllegalStateException("No se ha encontrado la plantilla de docker-compose en el classpath: " + resourcePath);
            }
            template = new String(is.readAllBytes(), StandardCharsets.UTF_8);
        }

        // Reemplazar placeholders
        String finalCompose = template.replace("__APP_PORT__", appPort);

        // Escribir docker-compose.yml ya final
        Path destinoCompose = carpetaProyecto.resolve("docker-compose.yml");
        Files.writeString(destinoCompose, finalCompose, StandardCharsets.UTF_8,
                StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);

        System.out.println("✅ [ZIP] docker-compose generado con APP_PORT=" + appPort + " en: " + destinoCompose);
    }

    // ==========================================================
    // CONFIG APP (app-config.properties)
    // ==========================================================
    private void crearFicheroConfiguracion(Path carpetaProyecto, Aplicacion aplicacion) throws IOException {
        Path resourcesDir = carpetaProyecto.resolve("src/main/resources");
        Files.createDirectories(resourcesDir);

        Path configFile = resourcesDir.resolve("app-config.properties");
        System.out.println("📝 [CONFIG] Generando app-config.properties en: " + configFile.toAbsolutePath());

        StringBuilder sb = new StringBuilder();
        sb.append("# Configuración generada por el Sistema de Gestión de Aplicaciones\n");
        sb.append("# Esta información viene de la tabla APLICACION de la app principal\n\n");

        sb.append("app.nombre=").append(s(aplicacion.getNombre())).append("\n");
        sb.append("app.descripcion=").append(s(aplicacion.getDescripcion())).append("\n");
        sb.append("app.lenguaje=").append(enumStr(aplicacion.getLenguaje())).append("\n");

        sb.append("app.baseDatos.tipo=").append(enumStr(aplicacion.getTipoBaseDatos())).append("\n");
        sb.append("app.baseDatos.nombre=").append(s(aplicacion.getNombreBaseDatos())).append("\n");
        sb.append("app.baseDatos.usuario=").append(s(aplicacion.getUsuarioBaseDatos())).append("\n");
        sb.append("app.baseDatos.password=").append(s(aplicacion.getPasswordBaseDatos())).append("\n");

        sb.append("app.cicd.proveedor=").append(enumStr(aplicacion.getProveedorCiCd())).append("\n");
        sb.append("app.cicd.repoGit=").append(s(aplicacion.getRepositorioGit())).append("\n");
        sb.append("app.cicd.sonarProjectKey=").append(s(aplicacion.getSonarProjectKey())).append("\n");

        sb.append("app.despliegue.puertoInterno=")
                .append(aplicacion.getPuertoAplicacion() != null ? aplicacion.getPuertoAplicacion() : "")
                .append("\n");
        sb.append("app.despliegue.nombreImagenEcr=").append(s(aplicacion.getNombreImagenEcr())).append("\n");

        Files.writeString(
                configFile,
                sb.toString(),
                StandardCharsets.UTF_8,
                StandardOpenOption.CREATE,
                StandardOpenOption.TRUNCATE_EXISTING
        );

        System.out.println("✅ [CONFIG] app-config.properties creado correctamente");
    }

    // ==========================================================
    // ZIP
    // ==========================================================
    private void comprimirCarpetaEnZip(Path carpeta, Path zipDestino) throws IOException {
        try (OutputStream fos = Files.newOutputStream(zipDestino);
             ZipOutputStream zos = new ZipOutputStream(fos)) {

            Files.walk(carpeta).forEach(path -> {
                try {
                    if (Files.isDirectory(path)) {
                        return;
                    }

                    Path relative = carpeta.relativize(path);
                    ZipEntry entry = new ZipEntry(relative.toString().replace("\\", "/"));
                    zos.putNextEntry(entry);

                    try (InputStream is = Files.newInputStream(path)) {
                        is.transferTo(zos);
                    }

                    zos.closeEntry();
                } catch (IOException e) {
                    throw new RuntimeException("Error comprimiendo fichero: " + path, e);
                }
            });
        }
    }

    // ==========================================================
    // HELPERS
    // ==========================================================
    private String leerRecursoClasspathComoString(String resourcePath) throws IOException {
        ClassLoader cl = getClass().getClassLoader();

        // Debug: ver si está empaquetado en el classpath
        System.out.println("📄 [ZIP] Buscando recurso en classpath: " + resourcePath);
        System.out.println("📄 [ZIP] Resource URL: " + cl.getResource(resourcePath));

        try (InputStream is = cl.getResourceAsStream(resourcePath)) {
            if (is == null) {
                throw new IllegalStateException("No se ha encontrado el recurso en el classpath: " + resourcePath);
            }
            return new String(is.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    private String sanitizarNombre(String value, String defaultValue) {
        String base = (value == null || value.isBlank()) ? defaultValue : value;
        return base.toLowerCase().replaceAll("[^a-z0-9\\-]", "-");
    }

    private String s(String value) {
        return value == null ? "" : value.replace("\n", " ").replace("\r", " ");
    }

    private String enumStr(Enum<?> e) {
        return e == null ? "" : e.name();
    }
}