package com.sistemagestionapp.util;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Utilidad para normalizar identificadores de repositorios Git.
 *
 * <p>Esta clase permite convertir distintas formas de indicar un repositorio
 * (URL HTTP, URL SSH o slug directo) a un formato uniforme:</p>
 *
 * <ul>
 *   <li><code>owner/repo</code></li>
 *   <li><code>group/subgroup/repo</code></li>
 * </ul>
 *
 * <p>Se utiliza para poder trabajar de forma consistente con GitHub y GitLab
 * en validaciones, llamadas a API y generación de configuraciones CI/CD.</p>
 */
public class RepoSlugUtil {

    /**
     * Patrón para repositorios indicados mediante URL HTTP/HTTPS.
     *
     * <p>Formatos soportados:</p>
     * <ul>
     *   <li>https://github.com/owner/repo.git</li>
     *   <li>https://github.com/owner/repo</li>
     *   <li>https://gitlab.com/group/subgroup/repo.git</li>
     * </ul>
     *
     * <p>El grupo 2 del patrón captura la ruta del repositorio
     * (<code>owner/repo</code> o <code>group/subgroup/repo</code>).</p>
     */
    private static final Pattern HTTP = Pattern.compile(
            "^(?:https?://)?(?:www\\.)?(github\\.com|gitlab\\.com)/(.+?)(?:\\.git)?/?$",
            Pattern.CASE_INSENSITIVE
    );

    /**
     * Patrón para repositorios indicados mediante URL SSH.
     *
     * <p>Formatos soportados:</p>
     * <ul>
     *   <li>git@github.com:owner/repo.git</li>
     *   <li>git@gitlab.com:group/subgroup/repo.git</li>
     * </ul>
     *
     * <p>El grupo 2 del patrón captura la ruta del repositorio.</p>
     */
    private static final Pattern SSH = Pattern.compile(
            "^git@(github\\.com|gitlab\\.com):(.+?)(?:\\.git)?$",
            Pattern.CASE_INSENSITIVE
    );

    /**
     * Convierte una URL o identificador de repositorio en un slug normalizado.
     *
     * <p>Si el repositorio ya viene en formato <code>owner/repo</code> o
     * <code>group/subgroup/repo</code>, se devuelve directamente.</p>
     *
     * <p>Si el formato no se reconoce, se devuelve {@code null}.</p>
     *
     * @param repoUrlOrSlug repositorio en formato URL (HTTP/SSH) o slug directo
     * @return slug normalizado o {@code null} si el formato no es válido
     */
    public static String toSlug(String repoUrlOrSlug) {
        if (repoUrlOrSlug == null) {
            return null;
        }

        String s = repoUrlOrSlug.trim();
        if (s.isEmpty()) {
            return null;
        }

        // Caso: ya viene como owner/repo o group/subgroup/repo
        if (!s.contains("://") && !s.startsWith("git@") && s.contains("/")) {
            if (s.endsWith(".git")) {
                s = s.substring(0, s.length() - 4);
            }
            return s;
        }

        // Caso: URL HTTP/HTTPS
        Matcher m1 = HTTP.matcher(s);
        if (m1.matches()) {
            String path = m1.group(2);
            if (path.endsWith(".git")) {
                path = path.substring(0, path.length() - 4);
            }
            path = path.replaceAll("^/+", "").replaceAll("/+$", "");
            return path;
        }

        // Caso: URL SSH
        Matcher m2 = SSH.matcher(s);
        if (m2.matches()) {
            String path = m2.group(2);
            if (path.endsWith(".git")) {
                path = path.substring(0, path.length() - 4);
            }
            path = path.replaceAll("^/+", "").replaceAll("/+$", "");
            return path;
        }

        return null;
    }
}
