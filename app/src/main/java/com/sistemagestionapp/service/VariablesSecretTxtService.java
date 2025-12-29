package com.sistemagestionapp.service;

import com.sistemagestionapp.model.Aplicacion;
import org.springframework.stereotype.Service;

@Service
public class VariablesSecretTxtService {

    public String nombreFichero(Aplicacion aplicacion) {
        String base = (aplicacion.getNombre() != null && !aplicacion.getNombre().isBlank())
                ? aplicacion.getNombre()
                : "app";

        base = base.trim().replaceAll("\\s+", "_").toLowerCase();
        return "secrets_" + base + ".txt";
    }

    public String generarTxt(Aplicacion aplicacion, String dbUri) {

        // --- APP ---
        String appPort = (aplicacion.getPuertoAplicacion() != null)
                ? String.valueOf(aplicacion.getPuertoAplicacion())
                : "8081";

        // --- DB ENGINE ---
        String dbEngine = (aplicacion.getTipoBaseDatos() != null)
                ? aplicacion.getTipoBaseDatos().name().toLowerCase()
                : "mysql";

        // --- DB MODE (local | remote) -> SIEMPRE DESDE ENTIDAD ---
        String modo = normalizeModo(aplicacion.getDbModo());

        // --- DB PORT por defecto ---
        String dbPort = defaultDbPort(dbEngine);

        // --- DB NAME ---
        String dbName = "remote".equals(modo)
                ? valueOrPlaceholder(aplicacion.getNombreBaseDatos(), "<RELLENAR>")
                : valueOrPlaceholder(aplicacion.getNombreBaseDatos(), "demo");

        // --- Credenciales (SQL) ---
        String dbUser = "remote".equals(modo)
                ? valueOrPlaceholder(aplicacion.getUsuarioBaseDatos(), "<RELLENAR>")
                : valueOrPlaceholder(aplicacion.getUsuarioBaseDatos(), "demo");

        String dbPassword = "remote".equals(modo)
                ? valueOrPlaceholder(aplicacion.getPasswordBaseDatos(), "<RELLENAR>")
                : valueOrPlaceholder(aplicacion.getPasswordBaseDatos(), "demo");

        // --- ECR (SIEMPRE desde entidad) ---
        String ecrRepo = valueOrPlaceholder(aplicacion.getNombreImagenEcr(), "<RELLENAR>");

        StringBuilder sb = new StringBuilder();

        sb.append("# ==============================================\n");
        sb.append("# Variables Secretas (generadas por el sistema)\n");
        sb.append("# Copia/pega en GitHub/GitLab -> Secrets/Variables\n");
        sb.append("# ==============================================\n\n");

        sb.append("# --- Generadas / derivadas del formulario ---\n");
        sb.append("APP_PORT=").append(appPort).append("\n");
        sb.append("DB_ENGINE=").append(dbEngine).append("\n");
        sb.append("DB_MODE=").append(modo).append("\n");
        sb.append("DB_PORT=").append(dbPort).append("\n");
        sb.append("DB_NAME=").append(dbName).append("\n");

        // --- DB_URI ---
        // mongo+remote: obligatorio
        if ("mongo".equals(dbEngine) && "remote".equals(modo)) {
            sb.append("DB_URI=").append(valueOrPlaceholder(dbUri, "<RELLENAR_ATLAS_URI>")).append("\n");
        } else {
            sb.append("DB_URI=\n");
        }

        // --- DB_HOST ---
        if ("remote".equals(modo)) {

            // Mongo remoto usa DB_URI, no DB_HOST
            if ("mongo".equals(dbEngine)) {
                sb.append("DB_HOST=\n");
            } else {
                sb.append("DB_HOST=<RELLENAR>\n");
            }

        } else {
            // LOCAL → nombre del servicio Docker
            sb.append("DB_HOST=").append(dbEngine).append("\n");
        }

        // --- DB_USER / DB_PASSWORD ---
        // mongo+remote: mejor vacío (van en la URI)
        if ("mongo".equals(dbEngine) && "remote".equals(modo)) {
            sb.append("DB_USER=\n");
            sb.append("DB_PASSWORD=\n");
        } else {
            sb.append("DB_USER=").append(dbUser).append("\n");
            sb.append("DB_PASSWORD=").append(dbPassword).append("\n");
        }

        sb.append("ECR_REPOSITORY=").append(ecrRepo).append("\n");

        sb.append("\n# --- AWS / EC2 (obligatorias para CI/CD + deploy) ---\n");
        sb.append("AWS_REGION=<RELLENAR>\n");
        sb.append("AWS_ACCOUNT_ID=<RELLENAR>\n");
        sb.append("AWS_ACCESS_KEY_ID=<RELLENAR>\n");
        sb.append("AWS_SECRET_ACCESS_KEY=<RELLENAR>\n");
        sb.append("EC2_HOST=<RELLENAR>\n");
        sb.append("EC2_USER=<RELLENAR>\n");
        sb.append("EC2_LLAVE_SSH=<RELLENAR>\n");
        sb.append("EC2_KNOWN_HOSTS=<OPCIONAL>\n");

        sb.append("\n# --- Sonar (opcional) ---\n");
        sb.append("SONAR_TOKEN=<OPCIONAL>\n");
        sb.append("SONAR_PROJECT_KEY=<OPCIONAL>\n");
        sb.append("SONAR_ORGANIZATION=<OPCIONAL>\n");
        sb.append("SONAR_HOST_URL=<OPCIONAL>\n");

        return sb.toString();
    }

    private String normalizeModo(String modo) {
        if (modo == null) return "local";
        String m = modo.trim().toLowerCase();
        return ("remote".equals(m) || "local".equals(m)) ? m : "local";
    }


    private String defaultDbPort(String dbEngine) {
        return switch (dbEngine) {
            case "mongo" -> "27017";
            case "postgres" -> "5432";
            case "mysql" -> "3306";
            default -> "3306";
        };
    }

    private String valueOrPlaceholder(String value, String placeholder) {
        if (value == null) return placeholder;
        String v = value.trim();
        return v.isEmpty() ? placeholder : v;
    }
}