package com.sistemagestionapp.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sistemagestionapp.model.EstadoControl;
import com.sistemagestionapp.model.PasoDespliegue;
import com.sistemagestionapp.model.dto.ResultadoPaso;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

@Service
public class SonarService {

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final DeployWizardService deployWizardService;

    public SonarService(DeployWizardService deployWizardService) {
        this.restTemplate = new RestTemplate();
        this.objectMapper = new ObjectMapper();
        this.deployWizardService = deployWizardService;
    }

    // ============================================================
    //  PASO 1 – Validar token + projectKey + existencia del proyecto
    // ============================================================
    public ResultadoPaso comprobarSonar(Long aplicacionId, String token, String projectKey) {

        if (token == null || token.isBlank() || projectKey == null || projectKey.isBlank()) {
            ResultadoPaso r = new ResultadoPaso("KO", "Token o ProjectKey vacío.");
            persistir(aplicacionId, PasoDespliegue.SONAR_ANALISIS, r);
            return r;
        }

        try {
            // Extraer organización del projectKey: formato esperado "<org>_<proyecto>"
            String[] partes = projectKey.split("_", 2);
            if (partes.length < 2) {
                ResultadoPaso r = new ResultadoPaso(
                        "KO",
                        "El ProjectKey debe seguir el formato <org>_<proyecto>. Ejemplo: davidtome97_tfg-cicd-aws-2526"
                );
                persistir(aplicacionId, PasoDespliegue.SONAR_ANALISIS, r);
                return r;
            }
            String organization = partes[0];

            // Auth Basic → token como usuario y contraseña vacía
            String auth = token + ":";
            String encodedAuth = Base64.getEncoder().encodeToString(auth.getBytes(StandardCharsets.UTF_8));

            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "Basic " + encodedAuth);
            HttpEntity<String> entity = new HttpEntity<>(headers);

            // Llamada para comprobar que el proyecto existe
            String url = "https://sonarcloud.io/api/projects/search"
                    + "?organization=" + organization
                    + "&projects=" + projectKey;

            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, entity, String.class);
            String body = response.getBody();

            if (response.getStatusCode().is2xxSuccessful()
                    && body != null
                    && body.contains("\"key\":\"" + projectKey + "\"")) {

                ResultadoPaso r = new ResultadoPaso("OK",
                        "Conexión con SonarCloud correcta. ProjectKey válido.");
                persistir(aplicacionId, PasoDespliegue.SONAR_ANALISIS, r);
                return r;

            } else {
                ResultadoPaso r = new ResultadoPaso("KO",
                        "SonarCloud respondió pero no encontró el proyecto en la organización " + organization + ".");
                persistir(aplicacionId, PasoDespliegue.SONAR_ANALISIS, r);
                return r;
            }

        } catch (HttpClientErrorException.Unauthorized e) {
            ResultadoPaso r = new ResultadoPaso("KO", "Token inválido o sin permisos.");
            persistir(aplicacionId, PasoDespliegue.SONAR_ANALISIS, r);
            return r;

        } catch (HttpClientErrorException.BadRequest e) {
            ResultadoPaso r = new ResultadoPaso("KO",
                    "Petición incorrecta a SonarCloud (400): " + e.getResponseBodyAsString());
            persistir(aplicacionId, PasoDespliegue.SONAR_ANALISIS, r);
            return r;

        } catch (HttpClientErrorException e) {
            ResultadoPaso r = new ResultadoPaso("KO",
                    "Error de SonarCloud (" + e.getStatusCode().value() + "): " + e.getResponseBodyAsString());
            persistir(aplicacionId, PasoDespliegue.SONAR_ANALISIS, r);
            return r;

        } catch (Exception e) {
            ResultadoPaso r = new ResultadoPaso("KO",
                    "Error conectando con SonarCloud: " + e.getMessage());
            persistir(aplicacionId, PasoDespliegue.SONAR_ANALISIS, r);
            return r;
        }
    }

    // ============================================================
    //  PASO 2 – Verificar si hay análisis (integración Sonar ↔ Git)
    // ============================================================
    public ResultadoPaso comprobarIntegracionSonarGit(Long aplicacionId, String token, String projectKey) {

        if (projectKey == null || projectKey.isBlank()) {
            ResultadoPaso r = new ResultadoPaso("KO", "ProjectKey vacío.");
            persistir(aplicacionId, PasoDespliegue.SONAR_INTEGRACION_GIT, r);
            return r;
        }

        try {
            // 1. Llamada pública (sin token) a los análisis del proyecto
            String url = "https://sonarcloud.io/api/project_analyses/search"
                    + "?project=" + projectKey
                    + "&ps=1";

            ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);
            String body = response.getBody();

            if (!response.getStatusCode().is2xxSuccessful() || body == null) {
                ResultadoPaso r = new ResultadoPaso("KO",
                        "SonarCloud respondió con código " + response.getStatusCode().value()
                                + " al consultar los análisis.");
                persistir(aplicacionId, PasoDespliegue.SONAR_INTEGRACION_GIT, r);
                return r;
            }

            // 2. Parsear JSON para ver si hay análisis
            JsonNode root = objectMapper.readTree(body);
            JsonNode analyses = root.path("analyses");

            if (!analyses.isArray() || analyses.isEmpty()) {
                ResultadoPaso r = new ResultadoPaso("KO",
                        "El proyecto existe pero no tiene ningún análisis en SonarCloud. "
                                + "Revisa que el pipeline CI/CD esté ejecutando el análisis.");
                persistir(aplicacionId, PasoDespliegue.SONAR_INTEGRACION_GIT, r);
                return r;
            }

            // 3. Tomamos el último análisis
            JsonNode ultimo = analyses.get(0);
            String date = ultimo.path("date").asText("fecha-desconocida");
            String revision = ultimo.path("revision").asText("revision-desconocida");

            String mensaje = "Integración Sonar–Git correcta. "
                    + "Último análisis el " + date
                    + " sobre la revisión " + revision + ".";

            ResultadoPaso r = new ResultadoPaso("OK", mensaje);
            persistir(aplicacionId, PasoDespliegue.SONAR_INTEGRACION_GIT, r);
            return r;

        } catch (Exception e) {
            ResultadoPaso r = new ResultadoPaso("KO",
                    "Error conectando con SonarCloud (analyses): " + e.getMessage());
            persistir(aplicacionId, PasoDespliegue.SONAR_INTEGRACION_GIT, r);
            return r;
        }
    }

    private void persistir(Long aplicacionId, PasoDespliegue paso, ResultadoPaso r) {
        if (aplicacionId == null) return; // por si todavía llamas sin appId
        EstadoControl estado = "OK".equalsIgnoreCase(r.getEstado()) ? EstadoControl.OK : EstadoControl.KO;
        deployWizardService.marcarPaso(aplicacionId, paso, estado, r.getMensaje());
    }
}
