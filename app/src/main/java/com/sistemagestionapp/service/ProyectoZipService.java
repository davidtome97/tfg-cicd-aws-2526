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
 *  - Creo un fichero app-config.properties con los datos básicos de la aplicación.
 *  - Comprimo la carpeta en un fichero .zip.
 *
 * Más adelante:
 *  - Podré elegir plantillas distintas según el lenguaje (JAVA / PYTHON).
 */
@Service
public class ProyectoZipService {

    /**
     * Genero un ZIP con el proyecto demo-java personalizado con la configuración
     * de la aplicación.
     *
     * @param aplicacion aplicación que estoy usando como base.
     * @return ruta del fichero ZIP generado en el sistema de ficheros.
     * @throws IOException si hay cualquier problema de lectura/escritura.
     */
    public Path generarProyectoJava(Aplicacion aplicacion) throws IOException {
        System.out.println("⚙️ [ZIP] Iniciando generación del ZIP para aplicación: " + aplicacion.getNombre());

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
        String nombreSanitizado = (aplicacion.getNombre() == null || aplicacion.getNombre().isBlank())
                ? "demo-java-proyecto"
                : aplicacion.getNombre().toLowerCase().replaceAll("[^a-z0-9\\-]", "-");

        // Carpeta raíz del proyecto dentro del directorio temporal
        Path carpetaProyecto = tempDir.resolve(nombreSanitizado);

        // 1) Copio demo-java dentro de carpetaProyecto (ignorando target/)
        System.out.println("📦 Copiando contenido de demo-java a: " + carpetaProyecto);
        copiarCarpeta(origenDemoJava, carpetaProyecto);
        System.out.println("✅ Copia de demo-java completada");

        // 2) Creo un fichero de configuración con los datos de la Aplicacion
        crearFicheroConfiguracion(carpetaProyecto, aplicacion);

        // 3) Comprimir carpetaProyecto en un zip
        Path zipPath = tempDir.resolve(nombreSanitizado + ".zip");
        System.out.println("🗜️  Comprimiendo carpeta en ZIP: " + zipPath);
        comprimirCarpetaEnZip(carpetaProyecto, zipPath);
        System.out.println("✅ ZIP generado correctamente en: " + zipPath);

        return zipPath;
    }

    /**
     * Copio recursivamente una carpeta de origen a destino, ignorando la carpeta target/.
     */
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

    /**
     * Creo un fichero app-config.properties dentro de src/main/resources
     * con los datos más importantes de la Aplicacion.
     */
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

    /**
     * Comprime una carpeta completa en un fichero ZIP.
     */
    private void comprimirCarpetaEnZip(Path carpeta, Path zipDestino) throws IOException {
        try (OutputStream fos = Files.newOutputStream(zipDestino);
             ZipOutputStream zos = new ZipOutputStream(fos)) {

            Files.walk(carpeta).forEach(path -> {
                try {
                    if (Files.isDirectory(path)) {
                        return; // no añado entradas para directorios
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

    private String s(String value) {
        return value == null ? "" : value.replace("\n", " ").replace("\r", " ");
    }

    private String enumStr(Enum<?> e) {
        return e == null ? "" : e.name();
    }
}