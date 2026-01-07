package com.sistemagestionapp.service;

import com.sistemagestionapp.model.Aplicacion;
import com.sistemagestionapp.model.DbModo;
import org.springframework.stereotype.Service;

@Service
public class VariablesSecretTxtService {

    // En este método genero el nombre del fichero .txt que el usuario descargará.
    // Lo baso en el nombre de la aplicación para que sea fácil identificarlo, y lo normalizo para evitar espacios y mayúsculas.
    public String nombreFichero(Aplicacion aplicacion) {
        String base = (aplicacion.getNombre() != null && !aplicacion.getNombre().isBlank())
                ? aplicacion.getNombre()
                : "app";

        base = base.trim().replaceAll("\\s+", "_").toLowerCase();
        return "secrets_" + base + ".txt";
    }

    // En este método construyo el contenido del TXT de "secrets" que el usuario copiará/pegará en GitHub/GitLab.
    // Aquí mezclo valores que ya conozco desde la entidad Aplicacion (puertos, motor, modo, repo ECR)
    // con placeholders <RELLENAR> para obligar a completar lo que el sistema no puede adivinar (AWS/EC2 o DB remota real).
    public String generarTxt(Aplicacion aplicacion, String dbUri) {

        // Aquí saco el puerto de la app. Si el usuario no lo ha definido, uso un valor por defecto.
        String appPort = (aplicacion.getPuertoAplicacion() != null)
                ? String.valueOf(aplicacion.getPuertoAplicacion())
                : "8081";

        // Aquí saco el motor de base de datos elegido en el formulario. Si faltase, pongo un default razonable.
        String dbEngine = (aplicacion.getTipoBaseDatos() != null)
                ? aplicacion.getTipoBaseDatos().name().toLowerCase()
                : "mysql";

        // Aquí saco el modo de base de datos (local o remote) siempre desde la entidad,
        // para que el TXT refleje exactamente lo que el usuario eligió en la aplicación.
        String modo = normalizeModo(aplicacion.getDbModo());

        // Aquí asigno el puerto por defecto según el motor. En este TXT lo dejo como referencia base.
        String dbPort = defaultDbPort(dbEngine);

        // Aquí defino el nombre de la base de datos.
        // Si es remote, normalmente quiero obligar a que lo rellenen (o al menos revisen), por eso uso <RELLENAR>.
        // Si es local, doy un default "demo" para que funcione de inmediato.
        String dbName = "remote".equals(modo)
                ? valueOrPlaceholder(aplicacion.getNombreBaseDatos(), "<RELLENAR>")
                : valueOrPlaceholder(aplicacion.getNombreBaseDatos(), "demo");

        // Aquí preparo usuario y contraseña para SQL.
        // Si es remote, marco <RELLENAR> para forzar a que se pongan credenciales reales.
        // Si es local, doy defaults (demo/demo) para simplificar el arranque.
        String dbUser = "remote".equals(modo)
                ? valueOrPlaceholder(aplicacion.getUsuarioBaseDatos(), "<RELLENAR>")
                : valueOrPlaceholder(aplicacion.getUsuarioBaseDatos(), "demo");

        String dbPassword = "remote".equals(modo)
                ? valueOrPlaceholder(aplicacion.getPasswordBaseDatos(), "<RELLENAR>")
                : valueOrPlaceholder(aplicacion.getPasswordBaseDatos(), "demo");

        // Aquí saco el nombre del repositorio de ECR desde la entidad.
        // Si no existe, dejo un placeholder para obligar a rellenarlo.
        String ecrRepo = valueOrPlaceholder(aplicacion.getNombreImagenEcr(), "<RELLENAR>");

        // Aquí empiezo a construir el contenido del TXT.
        StringBuilder sb = new StringBuilder();

        sb.append("# ==============================================\n");
        sb.append("# Variables Secretas (generadas por el sistema)\n");
        sb.append("# Copia/pega en GitHub/GitLab -> Secrets/Variables\n");
        sb.append("# ==============================================\n\n");

        // Aquí escribo variables que salen directamente de lo configurado en el formulario.
        sb.append("# --- Generadas / derivadas del formulario ---\n");
        sb.append("APP_PORT=").append(appPort).append("\n");
        sb.append("DB_ENGINE=").append(dbEngine).append("\n");
        sb.append("DB_MODE=").append(modo).append("\n");
        sb.append("DB_PORT=").append(dbPort).append("\n");
        sb.append("DB_NAME=").append(dbName).append("\n");

        // Aquí trato DB_URI:
        // - Si es Mongo remoto, la URI es obligatoria (por ejemplo Mongo Atlas).
        // - En el resto de casos la dejo vacía para no confundir.
        if ("mongo".equals(dbEngine) && "remote".equals(modo)) {
            sb.append("DB_URI=").append(valueOrPlaceholder(dbUri, "<RELLENAR_ATLAS_URI>")).append("\n");
        } else {
            sb.append("DB_URI=\n");
        }

        // Aquí trato DB_HOST:
        // - En remote: si es SQL, obligo a rellenar el endpoint; si es Mongo, lo dejo vacío porque manda la URI.
        // - En local: uso el nombre del servicio docker (mongo/mysql/postgres) para que compose lo resuelva por red interna.
        if ("remote".equals(modo)) {
            if ("mongo".equals(dbEngine)) {
                sb.append("DB_HOST=\n");
            } else {
                sb.append("DB_HOST=<RELLENAR>\n");
            }
        } else {
            sb.append("DB_HOST=").append(dbEngine).append("\n");
        }

        // Aquí trato DB_USER y DB_PASSWORD:
        // - En Mongo remoto normalmente las credenciales van en la URI, así que lo dejo vacío.
        // - En los demás casos, escribo los valores (defaults en local o placeholders en remote).
        if ("mongo".equals(dbEngine) && "remote".equals(modo)) {
            sb.append("DB_USER=\n");
            sb.append("DB_PASSWORD=\n");
        } else {
            sb.append("DB_USER=").append(dbUser).append("\n");
            sb.append("DB_PASSWORD=").append(dbPassword).append("\n");
        }

        // Aquí dejo el repositorio ECR para que el pipeline sepa dónde publicar y desde dónde desplegar.
        sb.append("ECR_REPOSITORY=").append(ecrRepo).append("\n");

        // Aquí dejo las variables obligatorias de AWS/EC2.
        // Estas no las puedo inventar, así que siempre las marco como <RELLENAR>.
        sb.append("\n# --- AWS / EC2 (obligatorias para CI/CD + deploy) ---\n");
        sb.append("AWS_REGION=<RELLENAR>\n");
        sb.append("AWS_ACCOUNT_ID=<RELLENAR>\n");
        sb.append("AWS_ACCESS_KEY_ID=<RELLENAR>\n");
        sb.append("AWS_SECRET_ACCESS_KEY=<RELLENAR>\n");
        sb.append("EC2_HOST=<RELLENAR>\n");
        sb.append("EC2_USER=<RELLENAR>\n");
        sb.append("EC2_LLAVE_SSH=<RELLENAR>\n");
        sb.append("EC2_KNOWN_HOSTS=<OPCIONAL>\n");

        // Aquí dejo las variables de Sonar como opcionales, porque el proyecto puede ejecutarse sin análisis.
        sb.append("\n# --- Sonar (opcional) ---\n");
        sb.append("SONAR_TOKEN=<OPCIONAL>\n");
        sb.append("SONAR_PROJECT_KEY=<OPCIONAL>\n");
        sb.append("SONAR_ORGANIZATION=<OPCIONAL>\n");
        sb.append("SONAR_HOST_URL=<OPCIONAL>\n");

        return sb.toString();
    }

    // En este método convierto el enum DbModo (LOCAL/REMOTE) a un string ("local"/"remote") que usarán los pipelines.
    private String normalizeModo(DbModo modo) {
        if (modo == null) return "local";
        return (modo == DbModo.REMOTE) ? "remote" : "local";
    }

    // En este método devuelvo el puerto por defecto según el motor de base de datos.
    // Lo uso para rellenar DB_PORT si el usuario no especifica nada.
    private String defaultDbPort(String dbEngine) {
        return switch (dbEngine) {
            case "mongo" -> "27017";
            case "postgres" -> "5432";
            case "mysql" -> "3306";
            default -> "3306";
        };
    }

    // En este método decido si devuelvo el valor real o un placeholder.
    // Me sirve para forzar a que el usuario rellene campos que no deberían quedar vacíos en modo remoto.
    private String valueOrPlaceholder(String value, String placeholder) {
        if (value == null) return placeholder;
        String v = value.trim();
        return v.isEmpty() ? placeholder : v;
    }
}