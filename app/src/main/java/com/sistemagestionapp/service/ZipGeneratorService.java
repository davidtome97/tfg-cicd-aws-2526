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

@Service
public class ZipGeneratorService {

    // Aquí inyecto el servicio que uso para cargar la aplicación desde base de datos (por id).
    private final AplicacionService aplicacionService;

    public ZipGeneratorService(AplicacionService aplicacionService) {
        this.aplicacionService = aplicacionService;
    }

    /**
     * En este método genero un ZIP único con el contenido que el usuario se descarga.
     * Yo decido si el ZIP contiene una demo completa o solo ficheros de configuración, según app.getTipoProyecto().
     *
     * Qué meto dentro del ZIP:
     * - Si es DEMO: copio la carpeta demo-java o demo-python al ZIP.
     * - Si es CONFIG: creo una estructura mínima con README y .gitignore.
     *
     * En ambos casos, además:
     * - Genero el pipeline CI/CD según proveedor (GitHub/GitLab/Jenkins) y lenguaje.
     * - Genero el docker-compose.yml según el motor de base de datos elegido.
     * - Genero un app-config.properties para dejar documentada la configuración seleccionada.
     *
     * Finalmente escribo el ZIP en la respuesta HTTP como fichero descargable.
     */
    public void generarZipAplicacion(Long aplicacionId, HttpServletResponse response) throws IOException {
        // Aquí recupero la aplicación a partir del id. Si no existe, corto el proceso.
        Aplicacion app = aplicacionService.obtenerPorId(aplicacionId);
        if (app == null) {
            throw new IllegalArgumentException("No existe la aplicación con id=" + aplicacionId);
        }

        // Aquí aplico defaults por si algún campo de la aplicación viene vacío.
        Lenguaje lenguaje = (app.getLenguaje() != null) ? app.getLenguaje() : Lenguaje.JAVA;
        TipoProyecto tipoProyecto = (app.getTipoProyecto() != null) ? app.getTipoProyecto() : TipoProyecto.CONFIG;

        // Aquí calculo el directorio base desde el que se está ejecutando el proyecto.
        // Lo uso para localizar la carpeta demo-java o demo-python.
        Path baseDir = Paths.get("").toAbsolutePath();

        // Aquí decido qué carpeta demo voy a buscar, según el lenguaje.
        String demoFolderName = (lenguaje == Lenguaje.PYTHON) ? "demo-python" : "demo-java";

        // Aquí intento localizar la carpeta demo de forma robusta:
        // 1) ../demo-xxx (si ejecuto desde el módulo /app, que es lo normal)
        // 2) ./demo-xxx (fallback si la estructura del repo cambia)
        Path origenDemo = (baseDir.getParent() != null) ? baseDir.getParent().resolve(demoFolderName) : null;
        if (origenDemo == null || !Files.isDirectory(origenDemo)) {
            origenDemo = baseDir.resolve(demoFolderName);
        }

        // Aquí creo un directorio temporal donde monto el proyecto antes de comprimirlo.
        Path tempDir = Files.createTempDirectory("tfg-zip-");

        // Aquí calculo el nombre del proyecto que verá el usuario dentro del ZIP.
        // Si no hay nombre de app, uso un nombre por defecto basado en demoFolderName.
        String baseNombre = (app.getNombre() == null || app.getNombre().isBlank())
                ? demoFolderName + "-proyecto"
                : app.getNombre();

        // Aquí genero un nombre seguro (sin tildes, sin espacios raros) para evitar problemas en rutas.
        String nombreProyecto = slug(baseNombre, demoFolderName + "-proyecto");

        // Aquí creo la carpeta raíz del proyecto que irá dentro del ZIP.
        Path carpetaProyecto = tempDir.resolve(nombreProyecto);
        Files.createDirectories(carpetaProyecto);

        try {
            // 1) Aquí decido si copio la DEMO o creo una estructura mínima de CONFIG.
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

            // 2) Aquí genero el pipeline de CI/CD según el proveedor seleccionado por el usuario.
            generarPipelineCiCd(carpetaProyecto, app, lenguaje);

            // 3) Aquí genero el docker-compose.yml según el motor de base de datos elegido.
            generarDockerCompose(carpetaProyecto, app, lenguaje);

            // 4) Aquí genero un fichero app-config.properties para dejar la configuración documentada.
            generarAppConfigProperties(carpetaProyecto, app);

            // 5) Aquí comprimo la carpeta del proyecto a un ZIP.
            Path zipPath = tempDir.resolve(nombreProyecto + ".zip");
            comprimirCarpetaEnZip(carpetaProyecto, zipPath);

            // 6) Aquí devuelvo el ZIP al navegador como descarga.
            response.setContentType("application/zip");
            response.setHeader("Content-Disposition", "attachment; filename=\"" + zipPath.getFileName() + "\"");
            response.setContentLengthLong(Files.size(zipPath));

            try (InputStream is = Files.newInputStream(zipPath);
                 OutputStream os = response.getOutputStream()) {
                is.transferTo(os);
                os.flush();
            }
        } finally {
            // Aquí limpio el directorio temporal para no dejar basura en el servidor.
            limpiarTemp(tempDir);
        }
    }

    // En este método creo una estructura mínima cuando el usuario pide un ZIP de tipo CONFIG.
    // Lo dejo preparado para que se pueda copiar dentro de un repo real.
    private void crearEstructuraMinimaConfig(Path carpetaProyecto, Lenguaje lenguaje) throws IOException {
        // Aquí creo un README básico explicando qué incluye el ZIP.
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

        // Aquí creo un .gitignore distinto según sea Python o Java.
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

    // En este método copio una carpeta completa (la demo) a otra ruta.
    // Además ignoro carpetas que no quiero meter en el ZIP, como target/ y __pycache__.
    private void copiarCarpeta(Path origen, Path destino) throws IOException {
        Path targetDir = origen.resolve("target");

        Files.walk(origen).forEach(sourcePath -> {
            try {
                // Aquí ignoro target/ para no meter compilados de Maven/Gradle.
                if (Files.exists(targetDir) && sourcePath.startsWith(targetDir)) return;

                // Aquí ignoro __pycache__ para no meter caché de Python.
                if (sourcePath.getFileName() != null && "__pycache__".equals(sourcePath.getFileName().toString())) return;
                if (sourcePath.toString().contains(FileSystems.getDefault().getSeparator() + "__pycache__" + FileSystems.getDefault().getSeparator()))
                    return;

                // Aquí calculo la ruta relativa para replicar la estructura exacta.
                Path relative = origen.relativize(sourcePath);
                Path targetPath = destino.resolve(relative);

                // Aquí creo directorios o copio ficheros.
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

    // En este método genero o copio el fichero de pipeline según el proveedor CI/CD que eligió el usuario.
    private void generarPipelineCiCd(Path carpetaProyecto, Aplicacion app, Lenguaje lenguaje) throws IOException {
        ProveedorCiCd proveedor = app.getProveedorCiCd();
        if (proveedor == null) return;

        // Aquí calculo la ruta base de recursos según el lenguaje (carpeta de plantillas).
        String base = resourceBase(lenguaje);

        // Aquí genero o copio el pipeline concreto.
        switch (proveedor) {
            case GITHUB -> generarGithubWorkflow(carpetaProyecto, app, base);
            case GITLAB -> copiarRecursoClasspathAPath(base + ".gitlab-ci.yml", carpetaProyecto.resolve(".gitlab-ci.yml"));
            case JENKINS -> copiarRecursoClasspathAPath(base + "Jenkinsfile", carpetaProyecto.resolve("Jenkinsfile"));
            default -> {
                // Aquí no hago nada si el proveedor no está soportado.
            }
        }
    }

    // En este método creo un workflow de GitHub Actions a partir de una plantilla.
    // Reemplazo __APP_NAME__ por el nombre real de la aplicación, normalizado, para evitar errores en nombres.
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

    // En este método genero el docker-compose.yml en función del motor de base de datos elegido.
    // Uso plantillas distintas según el motor (mysql/postgres/mongo) y adapto el puerto expuesto de la app.
    private void generarDockerCompose(Path carpetaProyecto, Aplicacion app, Lenguaje lenguaje) throws IOException {
        TipoBaseDatos tipo = app.getTipoBaseDatos();
        if (tipo == null) return;

        String nombrePlantilla = dockerComposePlantilla(tipo);
        if (nombrePlantilla == null) return;

        // Aquí aplico un puerto por defecto diferente según lenguaje (por cómo tengo montadas las demos).
        String defaultPort = (lenguaje == Lenguaje.PYTHON) ? "8082" : "8081";
        String appPort = (app.getPuertoAplicacion() == null || app.getPuertoAplicacion() <= 0)
                ? defaultPort
                : String.valueOf(app.getPuertoAplicacion());

        // Aquí primero intento cargar una plantilla específica del lenguaje; si no existe, hago fallback a java.
        String resourceLang = resourceBase(lenguaje) + nombrePlantilla;
        String template = existsOnClasspath(resourceLang)
                ? leerRecursoClasspathComoString(resourceLang)
                : leerRecursoClasspathComoString("static/ficherosjava/" + nombrePlantilla);

        // Aquí reemplazo el placeholder del puerto para que el compose quede listo.
        String finalCompose = template.replace("__APP_PORT__", appPort);

        Path destino = carpetaProyecto.resolve("docker-compose.yml");
        Files.writeString(destino, finalCompose, StandardCharsets.UTF_8,
                StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
    }

    // En este método traduzco el enum TipoBaseDatos a la plantilla concreta de docker-compose.
    private String dockerComposePlantilla(TipoBaseDatos tipo) {
        if (tipo == null) return null;
        return switch (tipo.name().toUpperCase()) {
            case "MYSQL" -> "docker-compose-mysql.yml";
            case "POSTGRES", "POSTGRESQL" -> "docker-compose-postgres.yml";
            case "MONGO", "MONGODB" -> "docker-compose-mongo.yml";
            default -> null;
        };
    }

    // En este método genero un fichero app-config.properties para documentar la configuración elegida.
    // Lo intento meter en src/main/resources si existe estructura de proyecto; si no, lo dejo en la raíz.
    private void generarAppConfigProperties(Path carpetaProyecto, Aplicacion app) throws IOException {
        Path resourcesDir = carpetaProyecto.resolve("src/main/resources");
        Path configFile;

        // Aquí decido dónde guardarlo según exista la estructura típica de Java.
        if (Files.exists(resourcesDir) || Files.exists(carpetaProyecto.resolve("src"))) {
            Files.createDirectories(resourcesDir);
            configFile = resourcesDir.resolve("app-config.properties");
        } else {
            configFile = carpetaProyecto.resolve("app-config.properties");
        }

        // Aquí escribo la configuración en formato propiedades.
        StringBuilder sb = new StringBuilder();
        sb.append("# Configuración generada por el Sistema de Gestión de Aplicaciones\n\n");

        sb.append("app.nombre=").append(s(app.getNombre())).append("\n");
        sb.append("app.descripcion=").append(s(app.getDescripcion())).append("\n");
        sb.append("app.lenguaje=").append(enumStr(app.getLenguaje())).append("\n");
        sb.append("app.tipoProyecto=").append(app.getTipoProyecto() != null ? app.getTipoProyecto().name() : "CONFIG").append("\n");

        sb.append("\n# Base de datos\n");
        sb.append("app.baseDatos.tipo=").append(enumStr(app.getTipoBaseDatos())).append("\n");

        // Aquí dejo el modo local por defecto si todavía no lo guardo en la entidad.
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

    // En este método comprimo una carpeta completa en un ZIP, manteniendo la estructura de subcarpetas.
    private void comprimirCarpetaEnZip(Path carpeta, Path zipDestino) throws IOException {
        try (OutputStream fos = Files.newOutputStream(zipDestino);
             ZipOutputStream zos = new ZipOutputStream(fos)) {

            Files.walk(carpeta).forEach(path -> {
                try {
                    if (Files.isDirectory(path)) return;

                    // Aquí calculo la ruta interna del ZIP y la normalizo a formato unix (/).
                    Path relative = carpeta.relativize(path);
                    ZipEntry entry = new ZipEntry(relative.toString().replace("\\", "/"));
                    zos.putNextEntry(entry);

                    // Aquí copio el contenido del fichero al ZIP.
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

    // Aquí devuelvo la ruta base del classpath donde guardo las plantillas, dependiendo del lenguaje.
    private String resourceBase(Lenguaje lenguaje) {
        return (lenguaje == Lenguaje.PYTHON) ? "static/ficherospython/" : "static/ficherosjava/";
    }

    // Aquí compruebo si existe un recurso en el classpath antes de intentar cargarlo.
    private boolean existsOnClasspath(String resourcePath) {
        return getClass().getClassLoader().getResource(resourcePath) != null;
    }

    // Aquí leo un recurso del classpath y lo convierto en String (para plantillas).
    private String leerRecursoClasspathComoString(String resourcePath) throws IOException {
        ClassLoader cl = getClass().getClassLoader();
        try (InputStream is = cl.getResourceAsStream(resourcePath)) {
            if (is == null) {
                throw new IllegalStateException("No se ha encontrado el recurso en el classpath: " + resourcePath);
            }
            return new String(is.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    // Aquí copio un recurso del classpath a un fichero real dentro de la carpeta del proyecto.
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

    // Aquí elimino el directorio temporal (y todo su contenido) para liberar espacio en el servidor.
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

    // Aquí limpio cadenas para meterlas en un properties sin saltos de línea raros.
    private String s(String value) {
        return value == null ? "" : value.replace("\n", " ").replace("\r", " ");
    }

    // Aquí convierto un enum a String de forma segura para el properties.
    private String enumStr(Enum<?> e) {
        return e == null ? "" : e.name();
    }

    // Aquí genero un "slug" seguro para nombres de carpetas y proyectos:
    // quito tildes, caracteres raros, dejo minúsculas y uso '-' como separador.
    private String slug(String value, String defaultValue) {
        String base = (value == null || value.isBlank()) ? defaultValue : value.trim().toLowerCase();
        base = Normalizer.normalize(base, Normalizer.Form.NFD).replaceAll("\\p{M}", "");
        base = base.replaceAll("[^a-z0-9_-]+", "-").replaceAll("^-+", "").replaceAll("-+$", "");
        return base.isBlank() ? defaultValue : base;
    }
}