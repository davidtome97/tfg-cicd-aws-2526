package com.sistemagestionapp.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sistemagestionapp.model.EstadoControl;
import com.sistemagestionapp.model.PasoDespliegue;
import com.sistemagestionapp.model.dto.ResultadoPaso;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

/**
 * En este servicio valido la configuración de SonarCloud para el asistente de despliegue.
 *
 * Por un lado, compruebo el paso 1 verificando que el token y el projectKey son correctos y que el
 * proyecto es accesible. Por otro lado, compruebo el paso 2 validando que el proyecto dispone de al
 * menos un análisis, lo que aporta evidencia de que el pipeline ha ejecutado el análisis y existe
 * trazabilidad con el repositorio.
 *
 * En ambos casos devuelvo un {@link ResultadoPaso} y persisto el estado del paso en la base de datos
 * mediante {@link DeployWizardService}.
 *
 * @author David Tomé Arnaiz
 */
@Service
public class SonarService {

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final DeployWizardService deployWizardService;

    /**
     * En este constructor inicializo las dependencias necesarias para consultar SonarCloud.
     *
     * @param deployWizardService servicio que utilizo para registrar el estado de cada paso
     */
    public SonarService(DeployWizardService deployWizardService) {
        this.restTemplate = new RestTemplate();
        this.objectMapper = new ObjectMapper();
        this.deployWizardService = deployWizardService;
    }

    /**
     * Valido el paso 1 del asistente comprobando token y projectKey contra SonarCloud.
     *
     * Interpreto la organización a partir del projectKey cuando sigue el formato {@code <org>_<proyecto>}.
     * Si la respuesta de SonarCloud es correcta y el proyecto aparece en los resultados, considero la
     * validación OK; en caso contrario, devuelvo KO con un mensaje explicativo.
     *
     * @param aplicacionId identificador de la aplicación
     * @param token token de SonarCloud
     * @param projectKey clave del proyecto en SonarCloud
     * @return resultado de la validación del paso 1
     */
    public ResultadoPaso comprobarSonar(Long aplicacionId, String token, String projectKey) {

        if (token == null || token.isBlank() || projectKey == null || projectKey.isBlank()) {
            ResultadoPaso r = new ResultadoPaso("KO", "Token o ProjectKey vacío.");
            persistir(aplicacionId, PasoDespliegue.SONAR_ANALISIS, r);
            return r;
        }

        try {
            String[] partes = projectKey.split("_", 2);
            if (partes.length < 2) {
                ResultadoPaso r = new ResultadoPaso(
                        "KO",
                        "El ProjectKey debe seguir el formato <org>_<proyecto>. Ejemplo: miOrg_mi-proyecto"
                );
                persistir(aplicacionId, PasoDespliegue.SONAR_ANALISIS, r);
                return r;
            }
            String organization = partes[0];

            String auth = token + ":";
            String encodedAuth = Base64.getEncoder().encodeToString(auth.getBytes(StandardCharsets.UTF_8));

            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "Basic " + encodedAuth);
            HttpEntity<String> entity = new HttpEntity<>(headers);

            String url = "https://sonarcloud.io/api/projects/search"
                    + "?organization=" + organization
                    + "&projects=" + projectKey;

            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, entity, String.class);
            String body = response.getBody();

            if (response.getStatusCode().is2xxSuccessful()
                    && body != null
                    && body.contains("\"key\":\"" + projectKey + "\"")) {

                ResultadoPaso r = new ResultadoPaso("OK", "Conexión con SonarCloud correcta. ProjectKey válido.");
                persistir(aplicacionId, PasoDespliegue.SONAR_ANALISIS, r);
                return r;
            }

            ResultadoPaso r = new ResultadoPaso(
                    "KO",
                    "SonarCloud respondió pero no encontró el proyecto en la organización " + organization + "."
            );
            persistir(aplicacionId, PasoDespliegue.SONAR_ANALISIS, r);
            return r;

        } catch (HttpClientErrorException.Unauthorized e) {
            ResultadoPaso r = new ResultadoPaso("KO", "Token inválido o sin permisos.");
            persistir(aplicacionId, PasoDespliegue.SONAR_ANALISIS, r);
            return r;

        } catch (HttpClientErrorException.BadRequest e) {
            ResultadoPaso r = new ResultadoPaso(
                    "KO",
                    "Petición incorrecta a SonarCloud (400): " + e.getResponseBodyAsString()
            );
            persistir(aplicacionId, PasoDespliegue.SONAR_ANALISIS, r);
            return r;

        } catch (HttpClientErrorException e) {
            ResultadoPaso r = new ResultadoPaso(
                    "KO",
                    "Error de SonarCloud (" + e.getStatusCode().value() + "): " + e.getResponseBodyAsString()
            );
            persistir(aplicacionId, PasoDespliegue.SONAR_ANALISIS, r);
            return r;

        } catch (Exception e) {
            ResultadoPaso r = new ResultadoPaso("KO", "Error conectando con SonarCloud: " + e.getMessage());
            persistir(aplicacionId, PasoDespliegue.SONAR_ANALISIS, r);
            return r;
        }
    }

    /**
     * Valido el paso 2 del asistente comprobando que el proyecto tiene al menos un análisis en SonarCloud.
     *
     * Esta comprobación sirve como evidencia de que el análisis se ha ejecutado correctamente y, por tanto,
     * la integración está operativa desde el punto de vista del asistente.
     *
     * @param aplicacionId identificador de la aplicación
     * @param token parámetro no utilizado en esta comprobación (se mantiene por compatibilidad)
     * @param projectKey clave del proyecto en SonarCloud
     * @return resultado de la validación del paso 2
     */
    public ResultadoPaso comprobarIntegracionSonarGit(Long aplicacionId, String token, String projectKey) {

        if (projectKey == null || projectKey.isBlank()) {
            ResultadoPaso r = new ResultadoPaso("KO", "ProjectKey vacío.");
            persistir(aplicacionId, PasoDespliegue.SONAR_INTEGRACION_GIT, r);
            return r;
        }

        try {
            String url = "https://sonarcloud.io/api/project_analyses/search"
                    + "?project=" + projectKey
                    + "&ps=1";

            ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);
            String body = response.getBody();

            if (!response.getStatusCode().is2xxSuccessful() || body == null) {
                ResultadoPaso r = new ResultadoPaso(
                        "KO",
                        "SonarCloud respondió con código " + response.getStatusCode().value() + " al consultar los análisis."
                );
                persistir(aplicacionId, PasoDespliegue.SONAR_INTEGRACION_GIT, r);
                return r;
            }

            JsonNode root = objectMapper.readTree(body);
            JsonNode analyses = root.path("analyses");

            if (!analyses.isArray() || analyses.isEmpty()) {
                ResultadoPaso r = new ResultadoPaso(
                        "KO",
                        "El proyecto existe pero no tiene ningún análisis en SonarCloud. Revisa el pipeline de CI/CD."
                );
                persistir(aplicacionId, PasoDespliegue.SONAR_INTEGRACION_GIT, r);
                return r;
            }

            JsonNode ultimo = analyses.get(0);
            String date = ultimo.path("date").asText("fecha-desconocida");
            String revision = ultimo.path("revision").asText("revision-desconocida");

            String mensaje = "Integración Sonar–Git correcta. Último análisis el " + date + " sobre la revisión " + revision + ".";
            ResultadoPaso r = new ResultadoPaso("OK", mensaje);
            persistir(aplicacionId, PasoDespliegue.SONAR_INTEGRACION_GIT, r);
            return r;

        } catch (Exception e) {
            ResultadoPaso r = new ResultadoPaso(
                    "KO",
                    "Error conectando con SonarCloud (analyses): " + e.getMessage()
            );
            persistir(aplicacionId, PasoDespliegue.SONAR_INTEGRACION_GIT, r);
            return r;
        }
    }

    /**
     * Persisto el resultado del paso convirtiendo el estado textual (OK/KO) al enum {@link EstadoControl}.
     *
     * @param aplicacionId identificador de la aplicación
     * @param paso paso del asistente a registrar
     * @param r resultado obtenido en la validación
     */
    private void persistir(Long aplicacionId, PasoDespliegue paso, ResultadoPaso r) {
        if (aplicacionId == null) {
            return;
        }

        EstadoControl estado = "OK".equalsIgnoreCase(r.getEstado())
                ? EstadoControl.OK
                : EstadoControl.KO;

        deployWizardService.marcarPaso(aplicacionId, paso, estado, r.getMensaje());
    }
}