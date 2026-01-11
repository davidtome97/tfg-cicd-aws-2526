package com.sistemagestionapp.controller;

import com.sistemagestionapp.model.DbModo;
import com.sistemagestionapp.model.EstadoControl;
import com.sistemagestionapp.model.PasoDespliegue;
import com.sistemagestionapp.model.TipoBaseDatos;
import com.sistemagestionapp.service.DeployWizardService;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/wizard")
public class WizardApiController {

    private final DeployWizardService deployWizardService;

    public WizardApiController(DeployWizardService deployWizardService) {
        this.deployWizardService = deployWizardService;
    }

    /* =========================================================
       ✅ PASO 1 - VALIDACIÓN SONAR (solo token + projectKey)
       ========================================================= */
    @GetMapping(value = "/paso1", produces = MediaType.APPLICATION_JSON_VALUE)
    public Map<String, String> validarPaso1(@RequestParam Long appId,
                                            @RequestParam String token,
                                            @RequestParam String projectKey) {

        Map<String, String> out = new HashMap<>();

        String t = token == null ? "" : token.trim();
        String pk = projectKey == null ? "" : projectKey.trim();

        if (t.isBlank() || pk.isBlank()) {
            out.put("estado", "KO");
            out.put("mensaje", "Debes indicar SONAR_TOKEN y SONAR_PROJECT_KEY.");
            return out;
        }

        out.put("estado", "OK");
        out.put("mensaje", "Formato correcto. Token y ProjectKey presentes.");
        return out;
    }

    /* =========================================================
       ✅ PASO 2 - VALIDACIÓN INTEGRACIÓN SONAR ↔ GIT (mínima)
       ========================================================= */
    @GetMapping(value = "/paso2", produces = MediaType.APPLICATION_JSON_VALUE)
    public Map<String, String> validarPaso2(@RequestParam Long appId,
                                            @RequestParam String projectKey) {

        Map<String, String> out = new HashMap<>();
        String pk = projectKey == null ? "" : projectKey.trim();

        if (pk.isBlank()) {
            out.put("estado", "KO");
            out.put("mensaje", "SONAR_PROJECT_KEY vacío.");
            return out;
        }

        out.put("estado", "OK");
        out.put("mensaje", "ProjectKey presente. Integración lista para confirmar.");
        return out;
    }

    /* =========================================================
       ✅ PASO 3 - VALIDACIÓN REPO (mínima)
       ========================================================= */
    @GetMapping(value = "/paso3", produces = MediaType.APPLICATION_JSON_VALUE)
    public Map<String, String> validarPaso3(@RequestParam Long appId,
                                            @RequestParam String proveedor,
                                            @RequestParam String repo) {

        Map<String, String> out = new HashMap<>();

        String prov = proveedor == null ? "" : proveedor.trim().toLowerCase();
        String r = repo == null ? "" : repo.trim();

        if (prov.isBlank() || r.isBlank()) {
            out.put("estado", "KO");
            out.put("mensaje", "Debes indicar proveedor y repositorio.");
            return out;
        }

        if (!prov.equals("github") && !prov.equals("gitlab")) {
            out.put("estado", "KO");
            out.put("mensaje", "Proveedor inválido. Usa github o gitlab.");
            return out;
        }

        if (!r.contains("/") || r.startsWith("/") || r.endsWith("/")) {
            out.put("estado", "KO");
            out.put("mensaje", "Formato inválido. Usa owner/repo (GitHub) o grupo/proyecto (GitLab).");
            return out;
        }

        out.put("estado", "OK");
        out.put("mensaje", "Formato correcto. Puedes confirmar para guardar en el asistente.");
        return out;
    }

    /* =========================================================
       ✅ PASO 4 - VALIDACIÓN AWS/ECR (mínima)
       ========================================================= */
    @GetMapping(value = "/paso4", produces = MediaType.APPLICATION_JSON_VALUE)
    public Map<String, String> validarPaso4(@RequestParam Long appId,
                                            @RequestParam String ecrRepository,
                                            @RequestParam String awsRegion,
                                            @RequestParam String awsAccessKeyId,
                                            @RequestParam String awsSecretAccessKey,
                                            @RequestParam String awsAccountId) {

        Map<String, String> out = new HashMap<>();

        String repo = ecrRepository == null ? "" : ecrRepository.trim();
        String region = awsRegion == null ? "" : awsRegion.trim();
        String access = awsAccessKeyId == null ? "" : awsAccessKeyId.trim();
        String secret = awsSecretAccessKey == null ? "" : awsSecretAccessKey.trim();
        String account = awsAccountId == null ? "" : awsAccountId.trim();

        if (repo.isBlank() || region.isBlank() || access.isBlank() || secret.isBlank() || account.isBlank()) {
            out.put("estado", "KO");
            out.put("mensaje", "Debes indicar ECR_REPOSITORY, AWS_REGION, AWS_ACCESS_KEY_ID, AWS_SECRET_ACCESS_KEY y AWS_ACCOUNT_ID.");
            return out;
        }

        if (!account.matches("^\\d{12}$")) {
            out.put("estado", "KO");
            out.put("mensaje", "AWS_ACCOUNT_ID inválido. Debe tener 12 dígitos.");
            return out;
        }

        if (!region.matches("^[a-z]{2}-[a-z]+-\\d$")) {
            out.put("estado", "KO");
            out.put("mensaje", "AWS_REGION inválida. Ejemplo: eu-west-1");
            return out;
        }

        out.put("estado", "OK");
        out.put("mensaje", "Formato correcto. Puedes confirmar para guardar las variables AWS/ECR.");
        return out;
    }

    /* =========================================================
       ✅ PASO 5 - GUARDAR CONFIG BD + MARCAR OK
       ========================================================= */
    @PostMapping(value = "/paso5/confirmar")
    public ResponseEntity<Void> confirmarPaso5(@RequestParam Long appId,
                                               @RequestParam String mode,     // local | remote
                                               @RequestParam String engine,   // postgres | mysql | mongo
                                               @RequestParam(required = false) Integer port,
                                               @RequestParam(required = false) String dbName,
                                               @RequestParam(required = false) String dbUser,
                                               @RequestParam(required = false) String dbPassword,
                                               @RequestParam(required = false) String endpoint // opcional
    ) {
        // Normaliza modo
        DbModo dbModo = "remote".equalsIgnoreCase(mode) ? DbModo.REMOTE : DbModo.LOCAL;

        // Normaliza motor (AJUSTA nombres si tu enum no coincide)
        TipoBaseDatos tipo = switch (engine.toLowerCase()) {
            case "postgres" -> TipoBaseDatos.POSTGRESQL; // <- si tu enum es POSTGRES, cambia aquí
            case "mysql" -> TipoBaseDatos.MYSQL;
            case "mongo" -> TipoBaseDatos.MONGODB;       // <- si tu enum es MONGO, cambia aquí
            default -> throw new IllegalArgumentException("Motor no soportado: " + engine);
        };

        // ✅ Guarda todo usando el service (el controller NO toca repositorios)
        deployWizardService.guardarPaso5Bd(
                appId,
                dbModo,
                tipo,
                dbName,
                dbUser,
                dbPassword,
                port,
                endpoint
        );

        // ✅ Marca el paso
        deployWizardService.marcarPaso(
                appId,
                PasoDespliegue.BASE_DATOS,
                EstadoControl.OK,
                "Base de datos configurada (" + engine.toUpperCase() + ", " + mode.toLowerCase() + ")."
        );

        return ResponseEntity.noContent().build();
    }




    @GetMapping(value = "/paso6", produces = MediaType.APPLICATION_JSON_VALUE)
    public Map<String, String> validarPaso6(@RequestParam Long appId,
                                            @RequestParam String host,
                                            @RequestParam Integer port) {

        Map<String, String> out = new HashMap<>();

        String h = host == null ? "" : host.trim();
        if (h.isBlank() || port == null) {
            out.put("estado", "KO");
            out.put("mensaje", "Debes indicar host y puerto.");
            return out;
        }

        // Comprobación HTTP simple (timeout corto)
        try {
            HttpClient client = HttpClient.newBuilder()
                    .connectTimeout(Duration.ofSeconds(4))
                    .build();

            URI uri = URI.create("http://" + h + ":" + port + "/");

            HttpRequest req = HttpRequest.newBuilder(uri)
                    .timeout(Duration.ofSeconds(6))
                    .GET()
                    .build();

            HttpResponse<Void> resp = client.send(req, HttpResponse.BodyHandlers.discarding());

            int code = resp.statusCode();
            if (code >= 200 && code < 500) {
                out.put("estado", "OK");
                out.put("mensaje", "Respuesta HTTP recibida (" + code + "). Puedes guardar las variables EC2.");
            } else {
                out.put("estado", "KO");
                out.put("mensaje", "La EC2 respondió con código " + code + ".");
            }
            return out;

        } catch (Exception e) {
            out.put("estado", "KO");
            out.put("mensaje", "No se pudo conectar a la app en EC2 (http://" + h + ":" + port + ").");
            return out;
        }
    }
}
