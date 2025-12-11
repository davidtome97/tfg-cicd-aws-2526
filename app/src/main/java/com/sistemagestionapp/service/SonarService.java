package com.sistemagestionapp.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sistemagestionapp.model.dto.ResultadoPaso;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

@Service
public class SonarService {

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    // ============================================================
    //  PASO 1 – Validar token + projectKey + existencia del proyecto
    // ============================================================
    public ResultadoPaso comprobarSonar(String token, String projectKey) {

        if (token == null || token.isBlank() || projectKey == null || projectKey.isBlank()) {
            return new ResultadoPaso("KO", "Token o ProjectKey vacío.");
        }

        try {
            // Extraer organización del projectKey: formato esperado "<org>_<proyecto>"
            String[] partes = projectKey.split("_", 2);
            if (partes.length < 2) {
                return new ResultadoPaso("KO",
                        "El ProjectKey debe seguir el formato <org>_<proyecto>. Ejemplo: davidtome97_tfg-cicd-aws-2526");
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

                return new ResultadoPaso("OK",
                        "Conexión con SonarCloud correcta. ProjectKey válido.");
            } else {
                return new ResultadoPaso("KO",
                        "SonarCloud respondió pero no encontró el proyecto en la organización " + organization + ".");
            }

        } catch (HttpClientErrorException.Unauthorized e) {
            return new ResultadoPaso("KO", "Token inválido o sin permisos.");
        } catch (HttpClientErrorException.BadRequest e) {
            return new ResultadoPaso("KO",
                    "Petición incorrecta a SonarCloud (400): " + e.getResponseBodyAsString());
        } catch (HttpClientErrorException e) {
            return new ResultadoPaso("KO",
                    "Error de SonarCloud (" + e.getStatusCode().value() + "): "
                            + e.getResponseBodyAsString());
        } catch (Exception e) {
            return new ResultadoPaso("KO",
                    "Error conectando con SonarCloud: " + e.getMessage());
        }
    }

    // ============================================================
    //  PASO 2 – Verificar si hay análisis (integración Sonar ↔ Git)
    // ============================================================
    public ResultadoPaso comprobarIntegracionSonarGit(String token, String projectKey) {

        if (projectKey == null || projectKey.isBlank()) {
            return new ResultadoPaso("KO", "ProjectKey vacío.");
        }

        try {
            // 1. Llamada pública (sin token) a los análisis del proyecto
            String url = "https://sonarcloud.io/api/project_analyses/search"
                    + "?project=" + projectKey
                    + "&ps=1";

            ResponseEntity<String> response =
                    restTemplate.getForEntity(url, String.class);

            String body = response.getBody();

            if (!response.getStatusCode().is2xxSuccessful() || body == null) {
                return new ResultadoPaso("KO",
                        "SonarCloud respondió con código " + response.getStatusCode().value()
                                + " al consultar los análisis.");
            }

            // 2. Parsear JSON para ver si hay análisis
            JsonNode root = objectMapper.readTree(body);
            JsonNode analyses = root.path("analyses");

            if (!analyses.isArray() || analyses.isEmpty()) {
                return new ResultadoPaso("KO",
                        "El proyecto existe pero no tiene ningún análisis en SonarCloud. "
                                + "Revisa que el pipeline CI/CD esté ejecutando el análisis.");
            }

            // 3. Tomamos el último análisis
            JsonNode ultimo = analyses.get(0);
            String date = ultimo.path("date").asText("fecha-desconocida");
            String revision = ultimo.path("revision").asText("revision-desconocida");

            String mensaje = "Integración Sonar–Git correcta. "
                    + "Último análisis el " + date
                    + " sobre la revisión " + revision + ".";

            return new ResultadoPaso("OK", mensaje);

        } catch (Exception e) {
            return new ResultadoPaso("KO",
                    "Error conectando con SonarCloud (analyses): " + e.getMessage());
        }
    }
}
