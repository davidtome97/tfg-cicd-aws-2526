package com.sistemagestionapp.service;

import com.sistemagestionapp.model.Aplicacion;
import com.sistemagestionapp.model.Lenguaje;
import com.sistemagestionapp.model.TipoBaseDatos;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.Comparator;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@Service
public class ZipGeneratorService {

    private final AplicacionService aplicacionService;

    public ZipGeneratorService(AplicacionService aplicacionService) {
        this.aplicacionService = aplicacionService;
    }

    /**
     * Genera el ZIP de la aplicación y lo escribe en la response HTTP.
     * - Copia demo-java o demo-python según aplicacion.lenguaje
     * - Inserta pipeline (GitHub/GitLab) según proveedor y lenguaje
     * - Inserta docker-compose según BD
     * - Crea app-config.properties
     */
    public void generarZipAplicacion(Long aplicacionId, HttpServletResponse response) throws IOException {
        Aplicacion aplicacion = aplicacionService.obtenerPorId(aplicacionId);
        if (aplicacion == null) {
            throw new IllegalArgumentException("No existe la aplicación con id=" + aplicacionId);
        }

        Lenguaje lenguaje = aplicacion.getLenguaje();
        if (lenguaje == null) lenguaje = Lenguaje.JAVA;

        // Raíz del repo (carpeta padre de "app")
        Path appDir = Paths.get("").toAbsolutePath();
        Path raizRepo = appDir.getParent();
        if (raizRepo == null) {
            throw new IllegalStateException("No se ha podido resolver la raíz del repositorio (parent de " + appDir + ")");
        }

        String demoFolderName = (lenguaje == Lenguaje.PYTHON) ? "demo-python" : "demo-java";
        Path origenDemo = raizRepo.resolve(demoFolderName);

        if (!Files.exists(origenDemo) || !Files.isDirectory(origenDemo)) {
            throw new IllegalStateException("No se ha encontrado la carpeta " + demoFolderName + " en: " + origenDemo.toAbsolutePath());
        }

        // Directorio temporal para montar el proyecto
        Path tempDir = Files.createTempDirectory("tfg-demo-");

        // Nombre del ZIP basado en el nombre de la app
        String baseNombre = (aplicacion.getNombre() == null || aplicacion.getNombre().isBlank())
                ? demoFolderName + "-proyecto"
                : aplicacion.getNombre();

        String nombreSanitizado = sanitizarNombre(baseNombre, demoFolderName + "-proyecto");
        Path carpetaProyecto = tempDir.resolve(nombreSanitizado);

        // 1) Copiar demo (ignorando target/ y __pycache__)
        copiarCarpeta(origenDemo, carpetaProyecto);

        // 2) Pipeline CI/CD
        copiarPipelineCiCd(carpetaProyecto, aplicacion, lenguaje);

        // 3) docker-compose según BD
        copiarDockerCompose(carpetaProyecto, aplicacion, lenguaje);

        // 4) app-config.properties
        crearFicheroConfiguracion(carpetaProyecto, aplicacion);

        // 5) ZIP
        Path zipPath = tempDir.resolve(nombreSanitizado + ".zip");
        comprimirCarpetaEnZip(carpetaProyecto, zipPath);

        // 6) Response HTTP
        response.setContentType("application/zip");
        response.setHeader("Content-Disposition", "attachment; filename=\"" + zipPath.getFileName() + "\"");
        response.setContentLengthLong(Files.size(zipPath));

        try (InputStream is = Files.newInputStream(zipPath);
             OutputStream os = response.getOutputStream()) {
            is.transferTo(os);
            os.flush();
        }

        // 7) Limpieza
        limpiarTemp(tempDir);
    }

    // ==========================================================
    // COPIA
    // ==========================================================
    private void copiarCarpeta(Path origen, Path destino) throws IOException {
        Path targetDir = origen.resolve("target");

        Files.walk(origen).forEach(sourcePath -> {
            try {
                // Ignorar target/
                if (Files.exists(targetDir) && sourcePath.startsWith(targetDir)) return;

                // Ignorar __pycache__
                if (sourcePath.getFileName() != null && "__pycache__".equals(sourcePath.getFileName().toString())) return;
                if (sourcePath.toString().contains(FileSystems.getDefault().getSeparator() + "__pycache__" + FileSystems.getDefault().getSeparator())) return;

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
    // PIPELINE
    // ==========================================================
    private void copiarPipelineCiCd(Path carpetaProyecto, Aplicacion aplicacion, Lenguaje lenguaje) throws IOException {
        if (aplicacion.getProveedorCiCd() == null) return;

        String proveedor = aplicacion.getProveedorCiCd().name();
        String base = resourceBase(lenguaje);

        if ("GITHUB".equalsIgnoreCase(proveedor)) {
            generarWorkflowGithubDesdePlantilla(carpetaProyecto, aplicacion, base);

        } else if ("GITLAB".equalsIgnoreCase(proveedor)) {
            Path destinoGitlab = carpetaProyecto.resolve(".gitlab-ci.yml");
            copiarRecursoClasspathAPath(base + ".gitlab-ci.yml", destinoGitlab);
        }
    }

    private void generarWorkflowGithubDesdePlantilla(Path carpetaProyecto, Aplicacion aplicacion, String base) throws IOException {
        String appName = sanitizarNombre(aplicacion.getNombre(), "mi-proyecto");

        String template = leerRecursoClasspathComoString(base + "github-workflow.template.yml");
        String workflow = template.replace("__APP_NAME__", appName);

        Path workflowsDir = carpetaProyecto.resolve(".github").resolve("workflows");
        Files.createDirectories(workflowsDir);

        Path destinoWorkflow = workflowsDir.resolve("generated-ci.yml");
        Files.writeString(destinoWorkflow, workflow, StandardCharsets.UTF_8,
                StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
    }

    // ==========================================================
    // DOCKER COMPOSE (con fallback a ficherosjava si no existe en python)
    // ==========================================================
    private void copiarDockerCompose(Path carpetaProyecto, Aplicacion aplicacion, Lenguaje lenguaje) throws IOException {
        if (aplicacion.getTipoBaseDatos() == null) return;

        String nombrePlantilla = dockerComposePlantilla(aplicacion.getTipoBaseDatos());
        if (nombrePlantilla == null) return;

        String appPort = (aplicacion.getPuertoAplicacion() == null || aplicacion.getPuertoAplicacion() <= 0)
                ? "8081"
                : String.valueOf(aplicacion.getPuertoAplicacion());

        String baseLang = resourceBase(lenguaje);
        String resourceLang = baseLang + nombrePlantilla;

        String template;
        if (existsOnClasspath(resourceLang)) {
            template = leerRecursoClasspathComoString(resourceLang);
        } else {
            template = leerRecursoClasspathComoString("static/ficherosjava/" + nombrePlantilla);
        }

        String finalCompose = template.replace("__APP_PORT__", appPort);

        Path destinoCompose = carpetaProyecto.resolve("docker-compose.yml");
        Files.writeString(destinoCompose, finalCompose, StandardCharsets.UTF_8,
                StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
    }

    private String dockerComposePlantilla(TipoBaseDatos tipo) {
        if (tipo == null) return null;
        return switch (tipo.name().toUpperCase()) {
            case "MYSQL" -> "docker-compose-mysql.yml";
            case "POSTGRES", "POSTGRESQL" -> "docker-compose-postgres.yml";
            case "MONGO", "MONGODB" -> "docker-compose-mongo.yml";
            default -> null;
        };
    }

    // ==========================================================
    // CONFIG (app-config.properties)
    // ==========================================================
    private void crearFicheroConfiguracion(Path carpetaProyecto, Aplicacion aplicacion) throws IOException {
        Path resourcesDir = carpetaProyecto.resolve("src/main/resources");
        Path configFile;

        if (Files.exists(resourcesDir) || Files.exists(carpetaProyecto.resolve("src"))) {
            Files.createDirectories(resourcesDir);
            configFile = resourcesDir.resolve("app-config.properties");
        } else {
            configFile = carpetaProyecto.resolve("app-config.properties");
        }

        StringBuilder sb = new StringBuilder();
        sb.append("# Configuración generada por el Sistema de Gestión de Aplicaciones\n\n");
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

        sb.append("app.despliegue.puertoInterno=").append(aplicacion.getPuertoAplicacion() != null ? aplicacion.getPuertoAplicacion() : "").append("\n");
        sb.append("app.despliegue.nombreImagenEcr=").append(s(aplicacion.getNombreImagenEcr())).append("\n");

        Files.writeString(configFile, sb.toString(), StandardCharsets.UTF_8,
                StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
    }

    // ==========================================================
    // ZIP
    // ==========================================================
    private void comprimirCarpetaEnZip(Path carpeta, Path zipDestino) throws IOException {
        try (OutputStream fos = Files.newOutputStream(zipDestino);
             ZipOutputStream zos = new ZipOutputStream(fos)) {

            Files.walk(carpeta).forEach(path -> {
                try {
                    if (Files.isDirectory(path)) return;

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
    // RESOURCES
    // ==========================================================
    private String resourceBase(Lenguaje lenguaje) {
        return (lenguaje == Lenguaje.PYTHON) ? "static/ficherospython/" : "static/ficherosjava/";
    }

    private boolean existsOnClasspath(String resourcePath) {
        return getClass().getClassLoader().getResource(resourcePath) != null;
    }

    private String leerRecursoClasspathComoString(String resourcePath) throws IOException {
        ClassLoader cl = getClass().getClassLoader();
        try (InputStream is = cl.getResourceAsStream(resourcePath)) {
            if (is == null) throw new IllegalStateException("No se ha encontrado el recurso en el classpath: " + resourcePath);
            return new String(is.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    private void copiarRecursoClasspathAPath(String resourcePath, Path destino) throws IOException {
        ClassLoader cl = getClass().getClassLoader();
        try (InputStream is = cl.getResourceAsStream(resourcePath)) {
            if (is == null) throw new IllegalStateException("No se ha encontrado el recurso en el classpath: " + resourcePath);

            if (destino.getParent() != null) Files.createDirectories(destino.getParent());
            Files.copy(is, destino, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    // ==========================================================
    // HELPERS
    // ==========================================================
    private void limpiarTemp(Path tempDir) {
        try {
            Files.walk(tempDir)
                    .sorted(Comparator.reverseOrder())
                    .forEach(p -> {
                        try { Files.deleteIfExists(p); } catch (IOException ignored) {}
                    });
        } catch (IOException ignored) {}
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