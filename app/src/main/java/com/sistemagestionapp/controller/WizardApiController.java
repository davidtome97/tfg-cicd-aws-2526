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

/**
 * En este controlador REST expongo endpoints de validación y confirmación para distintos pasos del asistente.
 *
 * Devuelvo respuestas JSON con estado y mensaje para su consumo desde la interfaz. En el paso de base de datos,
 * además persisto la configuración y registro el estado del paso mediante {@link DeployWizardService}.
 *
 * @author David Tomé Arnaiz
 */
@RestController
@RequestMapping("/api/wizard")
public class WizardApiController {

    private final DeployWizardService deployWizardService;

    /**
     * En este constructor inyecto el servicio principal del asistente.
     *
     * @param deployWizardService servicio con la lógica de persistencia y control de pasos
     * @author David Tomé Arnaiz
     */
    public WizardApiController(DeployWizardService deployWizardService) {
        this.deployWizardService = deployWizardService;
    }

    /**
     * En este endpoint realizo una validación mínima del paso 1 (Sonar).
     *
     * Verifico la presencia de token y projectKey, devolviendo un estado y un mensaje informativo.
     *
     * @param appId identificador de la aplicación
     * @param token token de Sonar
     * @param projectKey clave del proyecto en Sonar
     * @return mapa con el estado y el mensaje en formato JSON
     */
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

    /**
     * En este endpoint realizo una validación mínima del paso 2 (integración Sonar ↔ Git).
     *
     * Verifico la presencia del projectKey, devolviendo un estado y un mensaje informativo.
     *
     * @param appId identificador de la aplicación
     * @param projectKey clave del proyecto en Sonar
     * @return mapa con el estado y el mensaje en formato JSON
     */
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

    /**
     * En este endpoint realizo una validación mínima del paso 3 (repositorio Git).
     *
     * Verifico proveedor, formato de repositorio y valores admitidos para el proveedor.
     *
     * @param appId identificador de la aplicación
     * @param proveedor proveedor Git (github o gitlab)
     * @param repo repositorio en formato slug (owner/repo o grupo/proyecto)
     * @return mapa con el estado y el mensaje en formato JSON
     */
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

    /**
     * En este endpoint realizo una validación mínima del paso 4 (AWS/ECR).
     *
     * Verifico la presencia de variables requeridas y valido formato de accountId (12 dígitos) y región AWS.
     *
     * @param appId identificador de la aplicación
     * @param ecrRepository repositorio de ECR
     * @param awsRegion región de AWS
     * @param awsAccessKeyId access key de AWS
     * @param awsSecretAccessKey secret access key de AWS
     * @param awsAccountId account id de AWS (12 dígitos)
     * @return mapa con el estado y el mensaje en formato JSON
     */
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

    /**
     * En este endpoint guardo la configuración de base de datos y marco el paso 5 como completado.
     *
     * Normalizo el modo (local/remote) y el motor (postgres/mysql/mongo) para mapearlos a {@link DbModo} y
     * {@link TipoBaseDatos}, y delego el guardado en {@link DeployWizardService}.
     *
     * @param appId identificador de la aplicación
     * @param mode modo de base de datos (local|remote)
     * @param engine motor de base de datos (postgres|mysql|mongo)
     * @param port puerto (opcional)
     * @param dbName nombre de la base de datos (opcional según modo/motor)
     * @param dbUser usuario (opcional)
     * @param dbPassword contraseña (opcional)
     * @param endpoint endpoint o URI (opcional según modo/motor)
     * @return respuesta vacía en caso de éxito
     */
    @PostMapping(value = "/paso5/confirmar")
    public ResponseEntity<Void> confirmarPaso5(@RequestParam Long appId,
                                               @RequestParam String mode,
                                               @RequestParam String engine,
                                               @RequestParam(required = false) Integer port,
                                               @RequestParam(required = false) String dbName,
                                               @RequestParam(required = false) String dbUser,
                                               @RequestParam(required = false) String dbPassword,
                                               @RequestParam(required = false) String endpoint) {

        DbModo dbModo = "remote".equalsIgnoreCase(mode) ? DbModo.REMOTE : DbModo.LOCAL;

        TipoBaseDatos tipo = switch (engine.toLowerCase()) {
            case "postgres" -> TipoBaseDatos.POSTGRESQL;
            case "mysql" -> TipoBaseDatos.MYSQL;
            case "mongo" -> TipoBaseDatos.MONGODB;
            default -> throw new IllegalArgumentException("Motor no soportado: " + engine);
        };

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

        deployWizardService.marcarPaso(
                appId,
                PasoDespliegue.BASE_DATOS,
                EstadoControl.OK,
                "Base de datos configurada (" + engine.toUpperCase() + ", " + mode.toLowerCase() + ")."
        );

        return ResponseEntity.noContent().build();
    }

    /**
     * En este endpoint valido el paso 6 comprobando la accesibilidad HTTP de la aplicación desplegada en EC2.
     *
     * Realizo una petición HTTP simple a la ruta raíz utilizando un timeout corto. Si recibo una respuesta con código
     * entre 200 y 499 considero que existe conectividad y devuelvo OK. En caso contrario, devuelvo KO.
     *
     * @param appId identificador de la aplicación
     * @param host dirección IP o DNS público
     * @param port puerto público de la aplicación
     * @return mapa con el estado y el mensaje en formato JSON
     */
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
