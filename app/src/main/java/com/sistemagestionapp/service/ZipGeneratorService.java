package com.sistemagestionapp.service;

import com.sistemagestionapp.model.Aplicacion;
import com.sistemagestionapp.model.Lenguaje;
import com.sistemagestionapp.model.ProveedorCiCd;
import com.sistemagestionapp.model.TipoBaseDatos;
import com.sistemagestionapp.model.TipoProyecto;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.text.Normalizer;
import java.util.Comparator;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * En este servicio genero el ZIP descargable asociado a una aplicación.
 *
 * Construyo un paquete con una estructura lista para usar en un repositorio, que puede contener:
 * - una demo completa (Java o Python) si el tipo de proyecto es {@link TipoProyecto#DEMO}
 * - una estructura mínima de configuración si el tipo de proyecto es {@link TipoProyecto#CONFIG}
 *
 * Además, incluyo los ficheros de automatización necesarios para CI/CD y despliegue:
 * - pipeline de CI/CD según {@link ProveedorCiCd}
 * - {@code docker-compose.yml} según {@link TipoBaseDatos}
 * - {@code app-config.properties} con los parámetros seleccionados
 *
 * Finalmente escribo el ZIP en la respuesta HTTP para que el navegador lo descargue.
 *
 * @author David Tomé Arnaiz
 */
@Service
public class ZipGeneratorService {

    private final AplicacionService aplicacionService;

    public ZipGeneratorService(AplicacionService aplicacionService) {
        this.aplicacionService = aplicacionService;
    }

    /**
     * Genero un ZIP con el contenido asociado a una aplicación y lo devuelvo como descarga HTTP.
     *
     * Creo un directorio temporal, monto dentro la estructura final y la comprimo.
     * Al finalizar, elimino el directorio temporal para no dejar residuos en el servidor.
     *
     * @param aplicacionId identificador de la aplicación
     * @param response respuesta HTTP sobre la que escribo el ZIP
     * @throws IOException si ocurre un error de E/S al crear o enviar el ZIP
     */
    public void generarZipAplicacion(Long aplicacionId, HttpServletResponse response) throws IOException {
        Aplicacion app = aplicacionService.obtenerPorId(aplicacionId);
        if (app == null) {
            throw new IllegalArgumentException("No existe la aplicación con id=" + aplicacionId);
        }

        Lenguaje lenguaje = (app.getLenguaje() != null) ? app.getLenguaje() : Lenguaje.JAVA;
        TipoProyecto tipoProyecto = (app.getTipoProyecto() != null) ? app.getTipoProyecto() : TipoProyecto.CONFIG;

        Path baseDir = Paths.get("").toAbsolutePath();
        String demoFolderName = (lenguaje == Lenguaje.PYTHON) ? "demo-python" : "demo-java";

        Path origenDemo = (baseDir.getParent() != null) ? baseDir.getParent().resolve(demoFolderName) : null;
        if (origenDemo == null || !Files.isDirectory(origenDemo)) {
            origenDemo = baseDir.resolve(demoFolderName);
        }

        Path tempDir = Files.createTempDirectory("tfg-zip-");

        String baseNombre = (app.getNombre() == null || app.getNombre().isBlank())
                ? demoFolderName + "-proyecto"
                : app.getNombre();

        String nombreProyecto = slug(baseNombre, demoFolderName + "-proyecto");

        Path carpetaProyecto = tempDir.resolve(nombreProyecto);
        Files.createDirectories(carpetaProyecto);

        try {
            if (tipoProyecto == TipoProyecto.DEMO) {
                if (!Files.isDirectory(origenDemo)) {
                    throw new IllegalStateException(
                            "No se ha encontrado la carpeta " + demoFolderName + " en: " + origenDemo.toAbsolutePath()
                    );
                }
                copiarCarpeta(origenDemo, carpetaProyecto);
            } else {
                crearEstructuraMinimaConfig(carpetaProyecto, lenguaje);
            }

            generarPipelineCiCd(carpetaProyecto, app, lenguaje);
            generarDockerCompose(carpetaProyecto, app, lenguaje);
            generarAppConfigProperties(carpetaProyecto, app);

            Path zipPath = tempDir.resolve(nombreProyecto + ".zip");
            comprimirCarpetaEnZip(carpetaProyecto, zipPath);

            response.setContentType("application/zip");
            response.setHeader("Content-Disposition", "attachment; filename=\"" + zipPath.getFileName() + "\"");
            response.setContentLengthLong(Files.size(zipPath));

            try (InputStream is = Files.newInputStream(zipPath);
                 OutputStream os = response.getOutputStream()) {
                is.transferTo(os);
                os.flush();
            }
        } finally {
            limpiarTemp(tempDir);
        }
    }

    /**
     * Creo una estructura mínima cuando el usuario solicita un ZIP de tipo CONFIG.
     *
     * Incluyo un {@code README.md} y un {@code .gitignore} básico orientado al lenguaje seleccionado.
     *
     * @param carpetaProyecto carpeta raíz del proyecto que estoy generando
     * @param lenguaje lenguaje seleccionado en la aplicación
     * @throws IOException si ocurre un error al escribir ficheros
     */
    private void crearEstructuraMinimaConfig(Path carpetaProyecto, Lenguaje lenguaje) throws IOException {
        Path readme = carpetaProyecto.resolve("README.md");
        if (!Files.exists(readme)) {
            String contenido = """
                    # Proyecto de configuración CI/CD
                    
                    Este proyecto se ha generado en modo **Solo configuración**.
                    
                    Incluye:
                    - Pipeline (GitHub Actions / GitLab CI / Jenkins)
                    - docker-compose.yml según base de datos
                    - app-config.properties con parámetros seleccionados
                    
                    > Puedes copiar estos ficheros dentro de tu repositorio real o usarlos como base.
                    """;
            Files.writeString(readme, contenido, StandardCharsets.UTF_8, StandardOpenOption.CREATE);
        }

        Path gitignore = carpetaProyecto.resolve(".gitignore");
        if (!Files.exists(gitignore)) {
            String gi = (lenguaje == Lenguaje.PYTHON)
                    ? """
                      __pycache__/
                      *.pyc
                      .venv/
                      .env
                      """
                    : """
                      target/
                      .idea/
                      *.iml
                      .classpath
                      .project
                      .settings/
                      """;
            Files.writeString(gitignore, gi, StandardCharsets.UTF_8, StandardOpenOption.CREATE);
        }
    }

    /**
     * Copio recursivamente el contenido de una carpeta hacia otra ruta.
     *
     * Excluyo directorios habituales de artefactos (por ejemplo {@code target/} y {@code __pycache__/})
     * para evitar aumentar el tamaño del ZIP con contenido generado.
     *
     * @param origen carpeta origen a copiar
     * @param destino carpeta destino donde replico la estructura
     * @throws IOException si ocurre un error de E/S al copiar
     */
    private void copiarCarpeta(Path origen, Path destino) throws IOException {
        Path targetDir = origen.resolve("target");

        Files.walk(origen).forEach(sourcePath -> {
            try {
                if (Files.exists(targetDir) && sourcePath.startsWith(targetDir)) return;

                if (sourcePath.getFileName() != null && "__pycache__".equals(sourcePath.getFileName().toString())) return;
                if (sourcePath.toString().contains(FileSystems.getDefault().getSeparator() + "__pycache__" + FileSystems.getDefault().getSeparator()))
                    return;

                Path relative = origen.relativize(sourcePath);
                Path targetPath = destino.resolve(relative);

                if (Files.isDirectory(sourcePath)) {
                    Files.createDirectories(targetPath);
                } else {
                    if (targetPath.getParent() != null) Files.createDirectories(targetPath.getParent());
                    Files.copy(sourcePath, targetPath, StandardCopyOption.REPLACE_EXISTING);
                }
            } catch (IOException e) {
                throw new RuntimeException("Error copiando fichero: " + sourcePath, e);
            }
        });
    }

    /**
     * Genero o copio el pipeline de CI/CD según el proveedor seleccionado en la aplicación.
     *
     * Para GitHub genero un workflow desde plantilla; para GitLab y Jenkins copio ficheros ya preparados.
     *
     * @param carpetaProyecto carpeta raíz del proyecto que estoy montando
     * @param app aplicación con la configuración de CI/CD
     * @param lenguaje lenguaje del proyecto (para resolver la ruta de plantillas)
     * @throws IOException si ocurre un error al leer o escribir recursos
     */
    private void generarPipelineCiCd(Path carpetaProyecto, Aplicacion app, Lenguaje lenguaje) throws IOException {
        ProveedorCiCd proveedor = app.getProveedorCiCd();
        if (proveedor == null) return;

        String base = resourceBase(lenguaje);

        switch (proveedor) {
            case GITHUB -> generarGithubWorkflow(carpetaProyecto, app, base);
            case GITLAB -> copiarRecursoClasspathAPath(base + ".gitlab-ci.yml", carpetaProyecto.resolve(".gitlab-ci.yml"));
            case JENKINS -> copiarRecursoClasspathAPath(base + "Jenkinsfile", carpetaProyecto.resolve("Jenkinsfile"));
            default -> {
            }
        }
    }

    /**
     * Genero un workflow de GitHub Actions a partir de una plantilla y sustituyo el nombre de aplicación.
     *
     * @param carpetaProyecto carpeta raíz del proyecto
     * @param app aplicación con el nombre a inyectar en la plantilla
     * @param base ruta base de recursos para el lenguaje
     * @throws IOException si ocurre un error al leer la plantilla o escribir el workflow
     */
    private void generarGithubWorkflow(Path carpetaProyecto, Aplicacion app, String base) throws IOException {
        String appNameSafe = slug(app.getNombre(), "mi-proyecto");

        String template = leerRecursoClasspathComoString(base + "github-workflow.template.yml");
        String workflow = template.replace("__APP_NAME__", appNameSafe);

        Path workflowsDir = carpetaProyecto.resolve(".github").resolve("workflows");
        Files.createDirectories(workflowsDir);

        Path destino = workflowsDir.resolve("generated-ci.yml");
        Files.writeString(destino, workflow, StandardCharsets.UTF_8,
                StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
    }

    /**
     * Genero el {@code docker-compose.yml} según el motor de base de datos elegido.
     *
     * Cargo la plantilla asociada al motor y sustituyo el placeholder {@code __APP_PORT__} por el puerto final
     * de la aplicación. Si no existe plantilla específica del lenguaje, hago fallback a la ruta de Java.
     *
     * @param carpetaProyecto carpeta raíz del proyecto
     * @param app aplicación con el motor de BD y el puerto
     * @param lenguaje lenguaje del proyecto
     * @throws IOException si ocurre un error al leer o escribir el compose
     */
    private void generarDockerCompose(Path carpetaProyecto, Aplicacion app, Lenguaje lenguaje) throws IOException {
        TipoBaseDatos tipo = app.getTipoBaseDatos();
        if (tipo == null) return;

        String nombrePlantilla = dockerComposePlantilla(tipo);
        if (nombrePlantilla == null) return;

        String defaultPort = (lenguaje == Lenguaje.PYTHON) ? "8082" : "8081";
        String appPort = (app.getPuertoAplicacion() == null || app.getPuertoAplicacion() <= 0)
                ? defaultPort
                : String.valueOf(app.getPuertoAplicacion());

        String resourceLang = resourceBase(lenguaje) + nombrePlantilla;
        String template = existsOnClasspath(resourceLang)
                ? leerRecursoClasspathComoString(resourceLang)
                : leerRecursoClasspathComoString("static/ficherosjava/" + nombrePlantilla);

        String finalCompose = template.replace("__APP_PORT__", appPort);

        Path destino = carpetaProyecto.resolve("docker-compose.yml");
        Files.writeString(destino, finalCompose, StandardCharsets.UTF_8,
                StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
    }

    /**
     * Traduzco el tipo de base de datos a la plantilla de docker-compose correspondiente.
     *
     * @param tipo tipo de base de datos
     * @return nombre del fichero de plantilla o {@code null} si no existe correspondencia
     */
    private String dockerComposePlantilla(TipoBaseDatos tipo) {
        if (tipo == null) return null;
        return switch (tipo.name().toUpperCase()) {
            case "MYSQL" -> "docker-compose-mysql.yml";
            case "POSTGRES", "POSTGRESQL" -> "docker-compose-postgres.yml";
            case "MONGO", "MONGODB" -> "docker-compose-mongo.yml";
            default -> null;
        };
    }

    /**
     * Genero un fichero {@code app-config.properties} con la configuración seleccionada.
     *
     * Si detecto una estructura típica de proyecto Java, lo guardo en {@code src/main/resources};
     * en caso contrario lo guardo en la raíz del proyecto.
     *
     * @param carpetaProyecto carpeta raíz del proyecto
     * @param app aplicación con los valores de configuración
     * @throws IOException si ocurre un error al escribir el fichero
     */
    private void generarAppConfigProperties(Path carpetaProyecto, Aplicacion app) throws IOException {
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

        sb.append("app.nombre=").append(s(app.getNombre())).append("\n");
        sb.append("app.descripcion=").append(s(app.getDescripcion())).append("\n");
        sb.append("app.lenguaje=").append(enumStr(app.getLenguaje())).append("\n");
        sb.append("app.tipoProyecto=").append(app.getTipoProyecto() != null ? app.getTipoProyecto().name() : "CONFIG").append("\n");

        sb.append("\n# Base de datos\n");
        sb.append("app.baseDatos.tipo=").append(enumStr(app.getTipoBaseDatos())).append("\n");
        sb.append("app.baseDatos.modo=local\n");
        sb.append("app.baseDatos.nombre=").append(s(app.getNombreBaseDatos())).append("\n");
        sb.append("app.baseDatos.usuario=").append(s(app.getUsuarioBaseDatos())).append("\n");
        sb.append("app.baseDatos.password=").append(s(app.getPasswordBaseDatos())).append("\n");

        sb.append("\n# CI/CD\n");
        sb.append("app.cicd.proveedor=").append(enumStr(app.getProveedorCiCd())).append("\n");
        sb.append("app.cicd.repoGit=").append(s(app.getRepositorioGit())).append("\n");
        sb.append("app.cicd.sonarProjectKey=").append(s(app.getSonarProjectKey())).append("\n");

        sb.append("\n# Despliegue\n");
        sb.append("app.despliegue.puertoInterno=").append(app.getPuertoAplicacion() != null ? app.getPuertoAplicacion() : "").append("\n");
        sb.append("app.despliegue.nombreImagenEcr=").append(s(app.getNombreImagenEcr())).append("\n");

        Files.writeString(configFile, sb.toString(), StandardCharsets.UTF_8,
                StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
    }

    /**
     * Comprimo una carpeta completa en un fichero ZIP manteniendo la estructura de rutas relativa.
     *
     * @param carpeta carpeta a comprimir
     * @param zipDestino ruta del ZIP final
     * @throws IOException si ocurre un error al leer ficheros o escribir el ZIP
     */
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

            zos.finish();
        }
    }

    /**
     * Devuelvo la ruta base del classpath donde busco plantillas según el lenguaje del proyecto.
     *
     * @param lenguaje lenguaje del proyecto
     * @return ruta base dentro de resources
     */
    private String resourceBase(Lenguaje lenguaje) {
        return (lenguaje == Lenguaje.PYTHON) ? "static/ficherospython/" : "static/ficherosjava/";
    }

    /**
     * Compruebo si un recurso existe en el classpath.
     *
     * @param resourcePath ruta del recurso
     * @return {@code true} si existe, {@code false} en caso contrario
     */
    private boolean existsOnClasspath(String resourcePath) {
        return getClass().getClassLoader().getResource(resourcePath) != null;
    }

    /**
     * Leo un recurso del classpath y lo devuelvo como texto.
     *
     * @param resourcePath ruta del recurso
     * @return contenido del recurso como String
     * @throws IOException si ocurre un error al leer el recurso
     */
    private String leerRecursoClasspathComoString(String resourcePath) throws IOException {
        ClassLoader cl = getClass().getClassLoader();
        try (InputStream is = cl.getResourceAsStream(resourcePath)) {
            if (is == null) {
                throw new IllegalStateException("No se ha encontrado el recurso en el classpath: " + resourcePath);
            }
            return new String(is.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    /**
     * Copio un recurso del classpath a una ruta del sistema de ficheros.
     *
     * @param resourcePath recurso en classpath
     * @param destino ruta destino dentro del proyecto generado
     * @throws IOException si ocurre un error al copiar el recurso
     */
    private void copiarRecursoClasspathAPath(String resourcePath, Path destino) throws IOException {
        ClassLoader cl = getClass().getClassLoader();
        try (InputStream is = cl.getResourceAsStream(resourcePath)) {
            if (is == null) {
                throw new IllegalStateException("No se ha encontrado el recurso en el classpath: " + resourcePath);
            }
            if (destino.getParent() != null) Files.createDirectories(destino.getParent());
            Files.copy(is, destino, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    /**
     * Elimino un directorio temporal y todo su contenido.
     *
     * @param tempDir directorio temporal a eliminar
     */
    private void limpiarTemp(Path tempDir) {
        try {
            Files.walk(tempDir)
                    .sorted(Comparator.reverseOrder())
                    .forEach(p -> {
                        try {
                            Files.deleteIfExists(p);
                        } catch (IOException ignored) {
                        }
                    });
        } catch (IOException ignored) {
        }
    }

    /**
     * Normalizo un texto para guardarlo en un fichero de propiedades evitando saltos de línea.
     *
     * @param value valor original
     * @return valor normalizado
     */
    private String s(String value) {
        return value == null ? "" : value.replace("\n", " ").replace("\r", " ");
    }

    /**
     * Convierto un enum a String de forma segura.
     *
     * @param e enum
     * @return nombre del enum o cadena vacía si es {@code null}
     */
    private String enumStr(Enum<?> e) {
        return e == null ? "" : e.name();
    }

    /**
     * Genero un identificador seguro para nombres de carpeta y fichero.
     *
     * Elimino tildes, normalizo a minúsculas y sustituyo caracteres no válidos por guiones.
     *
     * @param value valor de entrada
     * @param defaultValue valor por defecto si la entrada está vacía
     * @return slug seguro para rutas
     */
    private String slug(String value, String defaultValue) {
        String base = (value == null || value.isBlank()) ? defaultValue : value.trim().toLowerCase();
        base = Normalizer.normalize(base, Normalizer.Form.NFD).replaceAll("\\p{M}", "");
        base = base.replaceAll("[^a-z0-9_-]+", "-").replaceAll("^-+", "").replaceAll("-+$", "");
        return base.isBlank() ? defaultValue : base;
    }
}