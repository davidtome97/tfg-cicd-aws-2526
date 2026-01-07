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

    // En este servicio hago llamadas HTTP a SonarCloud para validar el proyecto y comprobar que existen análisis.
    private final RestTemplate restTemplate;

    // Utilizo ObjectMapper para leer el JSON que devuelve la API de SonarCloud.
    private final ObjectMapper objectMapper;

    // Utilizo este servicio para guardar el estado del paso (OK/KO) y su mensaje en la base de datos.
    private final DeployWizardService deployWizardService;

    public SonarService(DeployWizardService deployWizardService) {
        this.restTemplate = new RestTemplate();
        this.objectMapper = new ObjectMapper();
        this.deployWizardService = deployWizardService;
    }

    // En este método valido el paso 1: compruebo que el token y el projectKey son correctos
    // y que el proyecto existe en SonarCloud dentro de su organización.
    public ResultadoPaso comprobarSonar(Long aplicacionId, String token, String projectKey) {

        // Aquí evito llamadas a la API si faltan datos básicos.
        if (token == null || token.isBlank() || projectKey == null || projectKey.isBlank()) {
            ResultadoPaso r = new ResultadoPaso("KO", "Token o ProjectKey vacío.");
            persistir(aplicacionId, PasoDespliegue.SONAR_ANALISIS, r);
            return r;
        }

        try {
            // Aquí extraigo la organización a partir del projectKey con formato "<org>_<proyecto>".
            // Si el usuario no respeta ese formato, devuelvo KO con un mensaje claro.
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

            // Aquí preparo la autenticación Basic que pide SonarCloud:
            // el token va como “usuario” y la contraseña se deja vacía.
            String auth = token + ":";
            String encodedAuth = Base64.getEncoder().encodeToString(auth.getBytes(StandardCharsets.UTF_8));

            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "Basic " + encodedAuth);
            HttpEntity<String> entity = new HttpEntity<>(headers);

            // Aquí consulto la API para verificar que el proyecto existe dentro de esa organización.
            String url = "https://sonarcloud.io/api/projects/search"
                    + "?organization=" + organization
                    + "&projects=" + projectKey;

            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, entity, String.class);
            String body = response.getBody();

            // Aquí valido la respuesta: si es 2xx y en el body aparece la key, doy OK.
            // (Es una comprobación simple y suficiente para este asistente).
            if (response.getStatusCode().is2xxSuccessful()
                    && body != null
                    && body.contains("\"key\":\"" + projectKey + "\"")) {

                ResultadoPaso r = new ResultadoPaso(
                        "OK",
                        "Conexión con SonarCloud correcta. ProjectKey válido."
                );
                persistir(aplicacionId, PasoDespliegue.SONAR_ANALISIS, r);
                return r;

            } else {
                // Si Sonar responde pero no aparece el proyecto, considero que el projectKey no corresponde a esa org
                // o que el token no tiene permisos para ver el proyecto.
                ResultadoPaso r = new ResultadoPaso(
                        "KO",
                        "SonarCloud respondió pero no encontró el proyecto en la organización " + organization + "."
                );
                persistir(aplicacionId, PasoDespliegue.SONAR_ANALISIS, r);
                return r;
            }

        } catch (HttpClientErrorException.Unauthorized e) {
            // Aquí controlo el caso típico: token inválido o token sin permisos para esa organización/proyecto.
            ResultadoPaso r = new ResultadoPaso("KO", "Token inválido o sin permisos.");
            persistir(aplicacionId, PasoDespliegue.SONAR_ANALISIS, r);
            return r;

        } catch (HttpClientErrorException.BadRequest e) {
            // Aquí devuelvo el detalle del 400 para que el usuario entienda qué ha mandado mal.
            ResultadoPaso r = new ResultadoPaso(
                    "KO",
                    "Petición incorrecta a SonarCloud (400): " + e.getResponseBodyAsString()
            );
            persistir(aplicacionId, PasoDespliegue.SONAR_ANALISIS, r);
            return r;

        } catch (HttpClientErrorException e) {
            // Aquí capturo cualquier otro error HTTP (403, 404, 500...) y lo explico en el mensaje.
            ResultadoPaso r = new ResultadoPaso(
                    "KO",
                    "Error de SonarCloud (" + e.getStatusCode().value() + "): " + e.getResponseBodyAsString()
            );
            persistir(aplicacionId, PasoDespliegue.SONAR_ANALISIS, r);
            return r;

        } catch (Exception e) {
            // Aquí capturo errores generales de red/parseo u otros para no romper el asistente.
            ResultadoPaso r = new ResultadoPaso(
                    "KO",
                    "Error conectando con SonarCloud: " + e.getMessage()
            );
            persistir(aplicacionId, PasoDespliegue.SONAR_ANALISIS, r);
            return r;
        }
    }

    // En este método valido el paso 2: compruebo que el proyecto tiene al menos un análisis en SonarCloud.
    // Si hay análisis, normalmente significa que el CI está ejecutando SonarScanner y que hay integración con el repo.
    public ResultadoPaso comprobarIntegracionSonarGit(Long aplicacionId, String token, String projectKey) {

        // Aquí compruebo que el projectKey viene informado antes de llamar a la API.
        if (projectKey == null || projectKey.isBlank()) {
            ResultadoPaso r = new ResultadoPaso("KO", "ProjectKey vacío.");
            persistir(aplicacionId, PasoDespliegue.SONAR_INTEGRACION_GIT, r);
            return r;
        }

        try {
            // Aquí hago una llamada pública (sin token) para pedir el último análisis del proyecto.
            // Con "ps=1" solo me traigo uno para que sea rápido.
            String url = "https://sonarcloud.io/api/project_analyses/search"
                    + "?project=" + projectKey
                    + "&ps=1";

            ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);
            String body = response.getBody();

            // Si la API no devuelve 2xx o no hay body, doy KO.
            if (!response.getStatusCode().is2xxSuccessful() || body == null) {
                ResultadoPaso r = new ResultadoPaso(
                        "KO",
                        "SonarCloud respondió con código " + response.getStatusCode().value()
                                + " al consultar los análisis."
                );
                persistir(aplicacionId, PasoDespliegue.SONAR_INTEGRACION_GIT, r);
                return r;
            }

            // Aquí parseo el JSON y compruebo el array "analyses".
            JsonNode root = objectMapper.readTree(body);
            JsonNode analyses = root.path("analyses");

            // Si no hay análisis, normalmente significa que el pipeline aún no pasó SonarScanner.
            if (!analyses.isArray() || analyses.isEmpty()) {
                ResultadoPaso r = new ResultadoPaso(
                        "KO",
                        "El proyecto existe pero no tiene ningún análisis en SonarCloud. "
                                + "Revisa que el pipeline CI/CD esté ejecutando el análisis."
                );
                persistir(aplicacionId, PasoDespliegue.SONAR_INTEGRACION_GIT, r);
                return r;
            }

            // Aquí saco los datos del último análisis para mostrarlos como evidencia.
            JsonNode ultimo = analyses.get(0);
            String date = ultimo.path("date").asText("fecha-desconocida");
            String revision = ultimo.path("revision").asText("revision-desconocida");

            // Esto ayuda a justificar ante el profesor que realmente hay trazabilidad con el repo.
            String mensaje = "Integración Sonar–Git correcta. "
                    + "Último análisis el " + date
                    + " sobre la revisión " + revision + ".";

            ResultadoPaso r = new ResultadoPaso("OK", mensaje);
            persistir(aplicacionId, PasoDespliegue.SONAR_INTEGRACION_GIT, r);
            return r;

        } catch (Exception e) {
            // Aquí capturo cualquier fallo (red, parseo, etc.) y devuelvo un KO controlado.
            ResultadoPaso r = new ResultadoPaso(
                    "KO",
                    "Error conectando con SonarCloud (analyses): " + e.getMessage()
            );
            persistir(aplicacionId, PasoDespliegue.SONAR_INTEGRACION_GIT, r);
            return r;
        }
    }

    // En este método guardo el resultado (OK/KO + mensaje) en la tabla de control de despliegue.
    // Así el asistente puede bloquear pasos, pintar el estado en pantalla y mostrar el resumen final.
    private void persistir(Long aplicacionId, PasoDespliegue paso, ResultadoPaso r) {
        if (aplicacionId == null) return; // Si alguien llama sin appId, no rompo, pero tampoco guardo nada.
        EstadoControl estado = "OK".equalsIgnoreCase(r.getEstado())
                ? EstadoControl.OK
                : EstadoControl.KO;

        deployWizardService.marcarPaso(aplicacionId, paso, estado, r.getMensaje());
    }
}