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

@Service
public class Paso6PdfService {

    // En este método genero el PDF del asistente para explicar qué secrets debe configurar el usuario.
    // Normalizo mode y engine para aceptar entradas como " Postgres " o "REMOTE" sin romper nada.
    // Si port viene vacío, uso el puerto por defecto del motor.
    public byte[] generarPdf(Long appId, String modeRaw, String engineRaw, Integer port) {
        String mode = norm(modeRaw);
        String engine = norm(engineRaw);

        // Aquí decido valores por defecto si el usuario no envía nada.
        String modeEff = mode.isBlank() ? "local" : mode;
        String engineEff = engine.isBlank() ? "postgres" : engine;

        // Aquí calculo el puerto final que aparecerá en el PDF.
        int defaultPort = defaultPort(engineEff);
        int effectivePort = (port == null) ? defaultPort : port;

        // En esta lista voy construyendo el contenido del PDF línea a línea.
        List<String> lines = new ArrayList<>();

        lines.add("Paso 6 - Configurar Base de Datos (Guía de Secrets)");
        lines.add("AppId: " + appId);
        lines.add("Fecha: " + LocalDateTime.now());
        lines.add("");
        lines.add("Selección:");
        lines.add(" - Modo: " + modeEff);
        lines.add(" - Motor: " + engineEff);
        lines.add(" - Puerto: " + (port == null ? (defaultPort + " (por defecto)") : (effectivePort + " (personalizado)")));
        lines.add("");

        // Aquí explico los secrets “mínimos” que se configuran en GitHub/GitLab/Jenkins para el deploy.
        // (Estos valores son los que luego consume el pipeline y el docker-compose en la EC2).
        lines.add("Secrets recomendados (GitHub -> Settings -> Secrets and variables -> Actions):");
        lines.add(" - DB_MODE=" + modeEff);
        lines.add(" - DB_ENGINE=" + engineEff);
        lines.add(" - DB_NAME=demo");
        lines.add(" - DB_PORT=" + effectivePort);
        lines.add("");

        // Aquí diferencio entre modo local y remoto, porque cambian las variables necesarias.
        if (!"remote".equals(modeEff)) {
            // En local asumo que docker-compose levanta el motor dentro de la EC2.
            // Por eso DB_HOST suele ser el nombre del servicio del compose.
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
                    // Si llega un engine no contemplado, dejo un mensaje genérico para no generar un PDF vacío.
                    lines.add(" - (motor no reconocido: " + engineEff + ")");
                    lines.add(" - DB_HOST=<servicio>");
                    lines.add(" - DB_USER=<usuario>");
                    lines.add(" - DB_PASSWORD=<password>");
                }
            }
            lines.add("");
            // En local normalmente el puerto del motor es el interno del contenedor y no se suele tocar.
            lines.add("Nota: en LOCAL normalmente no cambias el puerto interno del contenedor (5432/3306/27017).");
        } else {
            // En remoto la base de datos está fuera (RDS/Atlas, etc.) y el pipeline solo se conecta.
            // Aquí pido variables reales para evitar desplegar con valores “demo”.
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
                    // En Mongo remoto suelo recomendar URI porque incluye credenciales/replica set/params.
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
            // Esta frase deja claro el criterio de seguridad: si faltan secrets reales, el deploy debe fallar.
            lines.add("Aviso: si faltan variables reales en remoto, el deploy debe fallar para evitar despliegues incorrectos.");
        }

        lines.add("");
        // Cierro el PDF con una recomendación práctica para evitar los fallos típicos de red.
        lines.add("Consejo: revisa Security Group / puertos y credenciales antes de lanzar el deploy.");

        // Finalmente convierto las líneas a un PDF real usando PDFBox.
        return renderPdf(lines);
    }

    // En este método devuelvo el puerto por defecto según el motor elegido.
    private int defaultPort(String engineEff) {
        return switch (engineEff) {
            case "postgres" -> 5432;
            case "mysql" -> 3306;
            case "mongo" -> 27017;
            default -> 5432;
        };
    }

    // En este método renderizo la lista de líneas en un PDF.
    // Creo páginas A4 y salto a página nueva cuando me quedo sin espacio.
    private byte[] renderPdf(List<String> lines) {
        try (PDDocument doc = new PDDocument();
             ByteArrayOutputStream baos = new ByteArrayOutputStream()) {

            // Intento cargar una fuente Unicode para que se vean bien acentos y símbolos.
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

            // Hago un wrap simple por longitud para que no se corten líneas muy largas.
            List<String> wrapped = wrapAll(lines, 95);

            for (String line : wrapped) {
                // Si no queda espacio en la página, creo una página nueva y continúo.
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

                // Limpio saltos de línea para que PDFBox no falle al imprimir el texto.
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

    // En este método intento cargar una fuente TTF (DejaVuSans) desde resources.
    // Si no está disponible, uso Helvetica para no romper la generación del PDF.
    private PDFont loadUnicodeFontOrFallback(PDDocument doc) {
        try (InputStream is = getClass().getResourceAsStream("/fonts/DejaVuSans.ttf")) {
            if (is != null) {
                return PDType0Font.load(doc, is, true);
            }
        } catch (Exception ignored) {
        }
        return PDType1Font.HELVETICA;
    }

    // En este método aplico el wrap a todas las líneas.
    private List<String> wrapAll(List<String> lines, int maxLen) {
        List<String> out = new ArrayList<>();
        for (String l : lines) out.addAll(wrapLine(l, maxLen));
        return out;
    }

    // En este método divido una línea en varias si supera maxLen.
    // Corto por el último espacio posible para no partir palabras (si no hay, corto a lo bruto).
    private List<String> wrapLine(String line, int maxLen) {
        List<String> out = new ArrayList<>();
        String s = (line == null) ? "" : line;

        while (s.length() > maxLen) {
            int cut = s.lastIndexOf(' ', maxLen);
            if (cut <= 0) cut = maxLen;
            out.add(s.substring(0, cut).trim());
            s = s.substring(cut).trim();
        }
        out.add(s);
        return out;
    }

    // En este método normalizo entradas: null -> "", trim y a minúsculas.
    private String norm(String s) {
        if (s == null) return "";
        return s.trim().toLowerCase(Locale.ROOT);
    }
}