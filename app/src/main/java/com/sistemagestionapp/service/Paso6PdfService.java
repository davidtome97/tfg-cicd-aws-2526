package com.sistemagestionapp.service;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDType0Font;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * En este servicio genero el PDF de guía asociado a la configuración de base de datos.
 *
 * Utilizo este documento como salida descargable del asistente para indicar qué variables y secretos
 * deben definirse en el sistema de CI/CD. Normalizo los parámetros de entrada y aplico valores por
 * defecto para evitar errores cuando el usuario no proporciona modo, motor o puerto.
 *
 * Para la generación del PDF utilizo Apache PDFBox y aplico un renderizado sencillo por líneas,
 * con salto de página automático y un wrap básico para evitar que el texto se corte.
 *
 * @author David Tomé Arnaiz
 */
@Service
public class Paso6PdfService {

    /**
     * Genero un PDF con la guía de variables/secrets para el despliegue, en función de modo y motor de base de datos.
     *
     * Normalizo el modo y el motor para aceptar entradas con espacios o cambios de mayúsculas/minúsculas.
     * Si el puerto no se indica, utilizo el puerto por defecto del motor.
     *
     * @param appId identificador de la aplicación
     * @param modeRaw modo de base de datos (local o remote)
     * @param engineRaw motor de base de datos (postgres, mysql o mongo)
     * @param port puerto configurado por el usuario (opcional)
     * @return contenido del PDF en bytes
     */
    public byte[] generarPdf(Long appId, String modeRaw, String engineRaw, Integer port) {
        String mode = norm(modeRaw);
        String engine = norm(engineRaw);

        String modeEff = mode.isBlank() ? "local" : mode;
        String engineEff = engine.isBlank() ? "postgres" : engine;

        int defaultPort = defaultPort(engineEff);
        int effectivePort = (port == null) ? defaultPort : port;

        List<String> lines = new ArrayList<>();

        lines.add("Paso 6 - Configurar Base de Datos (Guía de Secrets)");
        lines.add("AppId: " + appId);
        lines.add("Fecha: " + LocalDateTime.now());
        lines.add("");
        lines.add("Selección:");
        lines.add(" - Modo: " + modeEff);
        lines.add(" - Motor: " + engineEff);
        lines.add(" - Puerto: " + (port == null
                ? (defaultPort + " (por defecto)")
                : (effectivePort + " (personalizado)")));
        lines.add("");

        lines.add("Secrets recomendados (GitHub -> Settings -> Secrets and variables -> Actions):");
        lines.add(" - DB_MODE=" + modeEff);
        lines.add(" - DB_ENGINE=" + engineEff);
        lines.add(" - DB_NAME=demo");
        lines.add(" - DB_PORT=" + effectivePort);
        lines.add("");

        if (!"remote".equals(modeEff)) {
            lines.add("Modo LOCAL: docker-compose levanta la BD dentro de la EC2.");
            switch (engineEff) {
                case "postgres" -> {
                    lines.add(" - DB_HOST=postgres");
                    lines.add(" - DB_USER=demo");
                    lines.add(" - DB_PASSWORD=demo");
                    lines.add(" - DB_SSLMODE=disable");
                }
                case "mysql" -> {
                    lines.add(" - DB_HOST=mysql");
                    lines.add(" - DB_USER=demo");
                    lines.add(" - DB_PASSWORD=demo");
                }
                case "mongo" -> {
                    lines.add(" - DB_HOST=mongo");
                    lines.add(" - DB_USER=demo");
                    lines.add(" - DB_PASSWORD=demo");
                }
                default -> {
                    lines.add(" - (motor no reconocido: " + engineEff + ")");
                    lines.add(" - DB_HOST=<servicio>");
                    lines.add(" - DB_USER=<usuario>");
                    lines.add(" - DB_PASSWORD=<password>");
                }
            }
            lines.add("");
            lines.add("Nota: en LOCAL normalmente no cambias el puerto interno del contenedor (5432/3306/27017).");
        } else {
            lines.add("Modo REMOTO: la BD está fuera (RDS/Atlas/otro). Debes crearla ANTES.");
            switch (engineEff) {
                case "postgres" -> {
                    lines.add(" - DB_HOST=<endpoint>");
                    lines.add(" - DB_USER=<usuario>");
                    lines.add(" - DB_PASSWORD=<password>");
                    lines.add(" - DB_SSLMODE=require");
                }
                case "mysql" -> {
                    lines.add(" - DB_HOST=<endpoint>");
                    lines.add(" - DB_USER=<usuario>");
                    lines.add(" - DB_PASSWORD=<password>");
                }
                case "mongo" -> {
                    lines.add(" - DB_URI=mongodb+srv://...");
                    lines.add("   (en Mongo remoto el puerto suele ir dentro de la URI)");
                }
                default -> {
                    lines.add(" - (motor no reconocido: " + engineEff + ")");
                    lines.add(" - DB_HOST=<endpoint>");
                    lines.add(" - DB_USER=<usuario>");
                    lines.add(" - DB_PASSWORD=<password>");
                }
            }
            lines.add("");
            lines.add("Aviso: si faltan variables reales en remoto, el deploy debe fallar para evitar despliegues incorrectos.");
        }

        lines.add("");
        lines.add("Consejo: revisa Security Group / puertos y credenciales antes de lanzar el deploy.");

        return renderPdf(lines);
    }

    /**
     * Devuelvo el puerto por defecto asociado al motor indicado.
     *
     * @param engineEff motor normalizado (postgres, mysql o mongo)
     * @return puerto por defecto del motor
     */
    private int defaultPort(String engineEff) {
        return switch (engineEff) {
            case "postgres" -> 5432;
            case "mysql" -> 3306;
            case "mongo" -> 27017;
            default -> 5432;
        };
    }

    /**
     * Renderizo una lista de líneas en un PDF A4.
     *
     * Utilizo salto de página automático cuando se agota el espacio disponible
     * y aplico wrap por longitud para evitar que el texto se corte.
     *
     * @param lines líneas de texto a imprimir
     * @return PDF en bytes
     */
    private byte[] renderPdf(List<String> lines) {
        try (PDDocument doc = new PDDocument();
             ByteArrayOutputStream baos = new ByteArrayOutputStream()) {

            PDFont font = loadUnicodeFontOrFallback(doc);

            float fontSize = 11f;
            float leading = 14f;
            float margin = 50f;

            PDPage page = new PDPage(PDRectangle.A4);
            doc.addPage(page);

            PDRectangle media = page.getMediaBox();
            float height = media.getHeight();
            float y = height - margin;

            PDPageContentStream cs = new PDPageContentStream(doc, page);
            cs.beginText();
            cs.setFont(font, fontSize);
            cs.newLineAtOffset(margin, y);

            List<String> wrapped = wrapAll(lines, 95);

            for (String line : wrapped) {
                if (y - leading < margin) {
                    cs.endText();
                    cs.close();

                    page = new PDPage(PDRectangle.A4);
                    doc.addPage(page);
                    media = page.getMediaBox();
                    height = media.getHeight();
                    y = height - margin;

                    cs = new PDPageContentStream(doc, page);
                    cs.beginText();
                    cs.setFont(font, fontSize);
                    cs.newLineAtOffset(margin, y);
                }

                String safe = (line == null) ? "" : line.replace("\r", "").replace("\n", "");
                cs.showText(safe);
                cs.newLineAtOffset(0, -leading);
                y -= leading;
            }

            cs.endText();
            cs.close();

            doc.save(baos);
            return baos.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("Error generando PDF Paso 6", e);
        }
    }

    /**
     * Intento cargar una fuente TrueType Unicode desde recursos para soportar acentos y símbolos.
     *
     * Si la fuente no está disponible, utilizo una fuente estándar para no impedir la generación del PDF.
     *
     * @param doc documento PDFBox en el que cargo la fuente
     * @return fuente Unicode o fuente de reserva
     */
    private PDFont loadUnicodeFontOrFallback(PDDocument doc) {
        try (InputStream is = getClass().getResourceAsStream("/fonts/DejaVuSans.ttf")) {
            if (is != null) {
                return PDType0Font.load(doc, is, true);
            }
        } catch (Exception ignored) {
        }
        return PDType1Font.HELVETICA;
    }

    /**
     * Aplico wrap a todas las líneas para evitar que el texto se corte en el margen.
     *
     * @param lines líneas originales
     * @param maxLen longitud máxima por línea
     * @return líneas con wrap aplicado
     */
    private List<String> wrapAll(List<String> lines, int maxLen) {
        List<String> out = new ArrayList<>();
        for (String l : lines) {
            out.addAll(wrapLine(l, maxLen));
        }
        return out;
    }

    /**
     * Divido una línea en varias cuando supera un máximo de caracteres.
     *
     * Priorizo cortar por el último espacio disponible para no partir palabras.
     *
     * @param line línea original
     * @param maxLen longitud máxima permitida
     * @return lista de líneas resultantes
     */
    private List<String> wrapLine(String line, int maxLen) {
        List<String> out = new ArrayList<>();
        String s = (line == null) ? "" : line;

        while (s.length() > maxLen) {
            int cut = s.lastIndexOf(' ', maxLen);
            if (cut <= 0) {
                cut = maxLen;
            }
            out.add(s.substring(0, cut).trim());
            s = s.substring(cut).trim();
        }

        out.add(s);
        return out;
    }

    /**
     * Normalizo una cadena para su uso como parámetro de entrada.
     *
     * Convierto nulos a cadena vacía, aplico trim y paso a minúsculas.
     *
     * @param s valor de entrada
     * @return valor normalizado
     */
    private String norm(String s) {
        if (s == null) {
            return "";
        }
        return s.trim().toLowerCase(Locale.ROOT);
    }
}