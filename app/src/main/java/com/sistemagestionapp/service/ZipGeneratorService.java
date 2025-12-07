package com.sistemagestionapp.service;

import com.sistemagestionapp.model.Aplicacion;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.*;
import java.util.Comparator;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * Servicio que genera un ZIP con el proyecto demo-java
 * a partir de una Aplicacion.
 *
 * Ahora mismo:
 *  - Copia la carpeta demo-java completa a un directorio temporal
 *  - La comprime en un .zip
 *  - La envía al navegador como descarga
 *
 * Más adelante podremos personalizar ficheros según la Aplicacion.
 */
@Service
public class ZipGeneratorService {

    private final AplicacionService aplicacionService;

    public ZipGeneratorService(AplicacionService aplicacionService) {
        this.aplicacionService = aplicacionService;
    }

    /**
     * Genera el ZIP de la aplicación y lo escribe en la response HTTP.
     */
    public void generarZipAplicacion(Long aplicacionId, HttpServletResponse response) throws IOException {
        Aplicacion aplicacion = aplicacionService.obtenerPorId(aplicacionId);
        if (aplicacion == null) {
            throw new IllegalArgumentException("No existe la aplicación con id=" + aplicacionId);
        }

        // Ruta del repo (carpeta padre de "app")
        Path appDir = Paths.get("").toAbsolutePath();
        Path raizRepo = appDir.getParent();          // .../tfg-cicd-aws-2526
        Path origenDemoJava = raizRepo.resolve("demo-java");

        if (!Files.exists(origenDemoJava)) {
            throw new IllegalStateException(
                    "No se ha encontrado la carpeta demo-java en: " + origenDemoJava.toAbsolutePath()
            );
        }

        // Directorio temporal para copiar y comprimir
        Path tempDir = Files.createTempDirectory("tfg-demojava-");
        Path destinoDemoJava = tempDir.resolve("demo-java");
        copiarCarpeta(origenDemoJava, destinoDemoJava);

        // Nombre del ZIP basado en el nombre de la aplicación
        String baseNombre = (aplicacion.getNombre() == null || aplicacion.getNombre().isBlank())
                ? "demo-java-proyecto"
                : aplicacion.getNombre();

        String nombreSanitizado = baseNombre
                .toLowerCase()
                .replaceAll("[^a-z0-9\\-]", "-");

        Path zipPath = tempDir.resolve(nombreSanitizado + ".zip");

        // Comprimir carpeta en ZIP
        comprimirCarpetaEnZip(destinoDemoJava, zipPath);

        // Preparar la response HTTP
        response.setContentType("application/zip");
        response.setHeader(
                "Content-Disposition",
                "attachment; filename=\"" + zipPath.getFileName().toString() + "\""
        );
        response.setContentLengthLong(Files.size(zipPath));

        // Enviar el ZIP al navegador
        try (InputStream is = Files.newInputStream(zipPath);
             OutputStream os = response.getOutputStream()) {
            is.transferTo(os);
            os.flush();
        }

        // Limpieza del temporal (opcional)
        try {
            Files.walk(tempDir)
                    .sorted(Comparator.reverseOrder())
                    .forEach(path -> {
                        try {
                            Files.deleteIfExists(path);
                        } catch (IOException ignored) {
                        }
                    });
        } catch (IOException ignored) {
        }
    }

    /**
     * Copia recursivamente una carpeta de origen a destino,
     * ignorando la carpeta target (binarios generados).
     */
    private void copiarCarpeta(Path origen, Path destino) throws IOException {
        Files.walk(origen).forEach(sourcePath -> {
            try {
                Path relative = origen.relativize(sourcePath);

                // --- IGNORAR /target y todo lo que cuelga de ahí ---
                // relative empieza por "target" -> no copiamos
                if (!relative.toString().isEmpty()
                        && relative.iterator().next().toString().equals("target")) {
                    return;
                }
                // ---------------------------------------------------

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
     * Comprime una carpeta completa en un fichero ZIP.
     */
    private void comprimirCarpetaEnZip(Path carpeta, Path zipDestino) throws IOException {
        try (OutputStream fos = Files.newOutputStream(zipDestino);
             ZipOutputStream zos = new ZipOutputStream(fos)) {

            Files.walk(carpeta).forEach(path -> {
                try {
                    if (Files.isDirectory(path)) {
                        return; // no meto directorios vacíos
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
}