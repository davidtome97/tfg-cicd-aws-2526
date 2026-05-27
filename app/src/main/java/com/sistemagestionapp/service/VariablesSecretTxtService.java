package com.sistemagestionapp.service;

import com.sistemagestionapp.model.Aplicacion;
import com.sistemagestionapp.model.DbModo;
import org.springframework.stereotype.Service;

/**
 * En este servicio genero el fichero TXT con variables y secretos que el usuario debe configurar
 * en su plataforma de CI/CD (por ejemplo, GitHub Actions, GitLab CI o Jenkins).
 *
 * A partir de los datos guardados en la entidad {@link Aplicacion}, construyo un listado de claves
 * con valores derivados (cuando puedo) y con placeholders cuando el valor depende del entorno del
 * usuario y no puedo inferirlo de forma segura.
 *
 * @author David Tomé Arnaiz
 */
@Service
public class VariablesSecretTxtService {

    /**
     * Genero el nombre del fichero TXT que el usuario descargará.
     *
     * Construyo el nombre a partir del nombre de la aplicación y lo normalizo para evitar espacios,
     * caracteres conflictivos y diferencias de mayúsculas/minúsculas.
     *
     * @param aplicacion aplicación de la que genero el nombre del fichero
     * @return nombre del fichero TXT de secrets
     */
    public String nombreFichero(Aplicacion aplicacion) {
        String base = (aplicacion.getNombre() != null && !aplicacion.getNombre().isBlank())
                ? aplicacion.getNombre()
                : "app";

        base = base.trim().replaceAll("\\s+", "_").toLowerCase();
        return "secrets_" + base + ".txt";
    }

    /**
     * Genero el contenido del TXT con variables de configuración y secretos para CI/CD.
     *
     * Mezclo valores obtenidos de {@link Aplicacion} con placeholders para forzar que el usuario complete
     * aquellos parámetros que no puedo conocer (por ejemplo, credenciales AWS, datos de EC2 o endpoints remotos).
     *
     * Para bases de datos en modo remoto, priorizo placeholders con el objetivo de evitar despliegues con valores
     * por defecto inseguros. Para modo local, utilizo valores razonables que permitan ejecutar el despliegue con
     * docker-compose de forma inmediata.
     *
     * @param aplicacion aplicación desde la que obtengo configuración y valores
     * @param dbUri URI de base de datos (principalmente para MongoDB remoto); puede ser {@code null}
     * @return contenido del TXT listo para copiar/pegar en el sistema de CI/CD
     */
    public String generarTxt(Aplicacion aplicacion, String dbUri) {

        String appPort = (aplicacion.getPuertoAplicacion() != null)
                ? String.valueOf(aplicacion.getPuertoAplicacion())
                : "8081";

        String dbEngine = (aplicacion.getTipoBaseDatos() != null)
                ? aplicacion.getTipoBaseDatos().name().toLowerCase()
                : "mysql";

        String modo = normalizarModo(aplicacion.getDbModo());
        String dbPort = defaultDbPort(dbEngine);

        String dbName = "remote".equals(modo)
                ? valueOrPlaceholder(aplicacion.getNombreBaseDatos(), "<RELLENAR>")
                : valueOrPlaceholder(aplicacion.getNombreBaseDatos(), "demo");

        String dbUser = "remote".equals(modo)
                ? valueOrPlaceholder(aplicacion.getUsuarioBaseDatos(), "<RELLENAR>")
                : valueOrPlaceholder(aplicacion.getUsuarioBaseDatos(), "demo");

        String dbPassword = "remote".equals(modo)
                ? valueOrPlaceholder(aplicacion.getPasswordBaseDatos(), "<RELLENAR>")
                : valueOrPlaceholder(aplicacion.getPasswordBaseDatos(), "demo");

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

        if ("mongo".equals(dbEngine) && "remote".equals(modo)) {
            sb.append("DB_URI=").append(valueOrPlaceholder(dbUri, "<RELLENAR_ATLAS_URI>")).append("\n");
        } else {
            sb.append("DB_URI=\n");
        }

        if ("remote".equals(modo)) {
            if ("mongo".equals(dbEngine)) {
                sb.append("DB_HOST=\n");
            } else {
                sb.append("DB_HOST=<RELLENAR>\n");
            }
        } else {
            sb.append("DB_HOST=").append(dbEngine).append("\n");
        }

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

    /**
     * Normalizo el modo de base de datos para su uso en los pipelines.
     *
     * @param modo modo en forma de enum
     * @return {@code "local"} o {@code "remote"}
     */
    private String normalizarModo(DbModo modo) {
        if (modo == null) {
            return "local";
        }
        return (modo == DbModo.REMOTE) ? "remote" : "local";
    }

    /**
     * Devuelvo el puerto por defecto según el motor de base de datos.
     *
     * @param dbEngine motor normalizado (mongo, postgres, mysql)
     * @return puerto por defecto como cadena
     */
    private String defaultDbPort(String dbEngine) {
        return switch (dbEngine) {
            case "mongo" -> "27017";
            case "postgres" -> "5432";
            case "mysql" -> "3306";
            default -> "3306";
        };
    }

    /**
     * Devuelvo el valor indicado o un placeholder si el valor está vacío.
     *
     * @param value valor real
     * @param placeholder texto a utilizar cuando el valor sea {@code null} o vacío
     * @return valor real o placeholder
     */
    private String valueOrPlaceholder(String value, String placeholder) {
        if (value == null) {
            return placeholder;
        }
        String v = value.trim();
        return v.isEmpty() ? placeholder : v;
    }
}