package com.sistemagestionapp.util;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class RepoSlugUtil {

    // Soporta:
    // - https://github.com/owner/repo.git
    // - https://github.com/owner/repo
    // - git@github.com:owner/repo.git
    // - https://gitlab.com/owner/repo.git
    // - https://gitlab.com/group/subgroup/repo.git  (devuelve group/subgroup/repo)
    private static final Pattern HTTP = Pattern.compile(
            "^(?:https?://)?(?:www\\.)?(github\\.com|gitlab\\.com)/(.+?)(?:\\.git)?/?$",
            Pattern.CASE_INSENSITIVE
    );

    private static final Pattern SSH = Pattern.compile(
            "^git@(github\\.com|gitlab\\.com):(.+?)(?:\\.git)?$",
            Pattern.CASE_INSENSITIVE
    );

    public static String toSlug(String repoUrlOrSlug) {
        if (repoUrlOrSlug == null) return null;
        String s = repoUrlOrSlug.trim();
        if (s.isEmpty()) return null;

        // Si ya viene como owner/repo o group/subgroup/repo
        if (!s.contains("://") && !s.startsWith("git@") && s.contains("/")) {
            // quita .git si lo han puesto
            if (s.endsWith(".git")) s = s.substring(0, s.length() - 4);
            return s;
        }

        Matcher m1 = HTTP.matcher(s);
        if (m1.matches()) {
            String path = m1.group(2);
            if (path.endsWith(".git")) path = path.substring(0, path.length() - 4);
            // Quita posibles dobles slashes o fragments
            path = path.replaceAll("^/+", "").replaceAll("/+$", "");
            return path;
        }

        Matcher m2 = SSH.matcher(s);
        if (m2.matches()) {
            String path = m2.group(2);
            if (path.endsWith(".git")) path = path.substring(0, path.length() - 4);
            path = path.replaceAll("^/+", "").replaceAll("/+$", "");
            return path;
        }

        return null; // no reconocido
    }
}
