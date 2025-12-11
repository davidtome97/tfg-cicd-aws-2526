package com.sistemagestionapp.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sistemagestionapp.model.dto.ResultadoPaso;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@Service
public class GitService {

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * proveedor: "github" o "gitlab"
     * repoPath: por ejemplo "davidtome97/tfg-cicd-aws-2526"
     */
    public ResultadoPaso comprobarRepositorio(String proveedor, String repoPath) {

        if (proveedor == null || proveedor.isBlank()
                || repoPath == null || repoPath.isBlank()) {

            return new ResultadoPaso("KO",
                    "Proveedor o repositorio vacío. Ejemplo: proveedor=github, repo=davidtome97/tfg-cicd-aws-2526");
        }

        try {
            String proveedorLower = proveedor.toLowerCase();

            return switch (proveedorLower) {
                case "github" -> comprobarGitHub(repoPath);
                case "gitlab" -> comprobarGitLab(repoPath);
                default -> new ResultadoPaso("KO",
                        "Proveedor no soportado. Usa 'github' o 'gitlab'.");
            };

        } catch (Exception e) {
            return new ResultadoPaso("KO",
                    "Error comprobando el repositorio Git: " + e.getMessage());
        }
    }

    // ===============================
    //  GitHub
    // ===============================
    private ResultadoPaso comprobarGitHub(String repoPath) {
        try {
            String baseUrl = "https://api.github.com";

            // 1) Comprobar que el repo existe
            String repoUrl = baseUrl + "/repos/" + repoPath;
            ResponseEntity<String> repoResponse =
                    restTemplate.getForEntity(repoUrl, String.class);

            if (!repoResponse.getStatusCode().is2xxSuccessful()) {
                return new ResultadoPaso("KO",
                        "GitHub respondió con código "
                                + repoResponse.getStatusCode().value()
                                + " al consultar el repositorio.");
            }

            // 2) Consultar el último commit
            String commitsUrl = baseUrl + "/repos/" + repoPath + "/commits?per_page=1";
            ResponseEntity<String> commitsResponse =
                    restTemplate.getForEntity(commitsUrl, String.class);

            if (!commitsResponse.getStatusCode().is2xxSuccessful()
                    || commitsResponse.getBody() == null) {

                return new ResultadoPaso("KO",
                        "No se ha podido obtener la lista de commits del repositorio en GitHub.");
            }

            JsonNode commitsArray = objectMapper.readTree(commitsResponse.getBody());
            if (!commitsArray.isArray() || commitsArray.isEmpty()) {
                return new ResultadoPaso("KO",
                        "El repositorio existe en GitHub pero no tiene ningún commit.");
            }

            JsonNode ultimoCommit = commitsArray.get(0);
            String sha = ultimoCommit.path("sha").asText("desconocido");
            String fecha = ultimoCommit.path("commit").path("author").path("date")
                    .asText("fecha-desconocida");

            String mensaje = "Repositorio GitHub válido. Último commit "
                    + sha + " en fecha " + fecha + ".";

            return new ResultadoPaso("OK", mensaje);

        } catch (HttpClientErrorException.NotFound e) {
            return new ResultadoPaso("KO",
                    "El repositorio no existe en GitHub: " + repoPath);
        } catch (HttpClientErrorException e) {
            return new ResultadoPaso("KO",
                    "Error de GitHub (" + e.getStatusCode().value() + "): "
                            + e.getResponseBodyAsString());
        } catch (Exception e) {
            return new ResultadoPaso("KO",
                    "Error conectando con la API de GitHub: " + e.getMessage());
        }
    }

    // ===============================
    //  GitLab
    // ===============================
    private ResultadoPaso comprobarGitLab(String repoPath) {
        try {
            String baseUrl = "https://gitlab.com/api/v4";

            // Nombre corto del proyecto (parte después del último "/")
            String projectName = repoPath.substring(repoPath.lastIndexOf("/") + 1);

            // 1) Buscar proyectos por nombre
            String searchUrl = baseUrl + "/projects?search="
                    + URLEncoder.encode(projectName, StandardCharsets.UTF_8)
                    + "&simple=true&per_page=50";

            ResponseEntity<String> searchResponse =
                    restTemplate.getForEntity(searchUrl, String.class);

            if (!searchResponse.getStatusCode().is2xxSuccessful()
                    || searchResponse.getBody() == null) {

                return new ResultadoPaso("KO",
                        "No se ha podido buscar el proyecto en GitLab.");
            }

            JsonNode projectsArray = objectMapper.readTree(searchResponse.getBody());
            if (!projectsArray.isArray() || projectsArray.isEmpty()) {
                return new ResultadoPaso("KO",
                        "No se ha encontrado ningún proyecto en GitLab con nombre " + projectName + ".");
            }

            // 2) Buscar en los resultados el que coincida exactamente con path_with_namespace
            JsonNode proyectoEncontrado = null;
            for (JsonNode proj : projectsArray) {
                String pathWithNamespace = proj.path("path_with_namespace").asText("");
                if (repoPath.equalsIgnoreCase(pathWithNamespace)) {
                    proyectoEncontrado = proj;
                    break;
                }
            }

            if (proyectoEncontrado == null) {
                return new ResultadoPaso("KO",
                        "El repositorio no existe en GitLab: " + repoPath);
            }

            int projectId = proyectoEncontrado.path("id").asInt(-1);
            if (projectId == -1) {
                return new ResultadoPaso("KO",
                        "Proyecto de GitLab encontrado pero sin ID válido.");
            }

            // 3) Obtener el último commit usando el ID numérico
            String commitsUrl = baseUrl + "/projects/" + projectId + "/repository/commits?per_page=1";
            ResponseEntity<String> commitsResponse =
                    restTemplate.getForEntity(commitsUrl, String.class);

            if (!commitsResponse.getStatusCode().is2xxSuccessful()
                    || commitsResponse.getBody() == null) {

                return new ResultadoPaso("KO",
                        "No se ha podido obtener la lista de commits del proyecto en GitLab.");
            }

            JsonNode commitsArray = objectMapper.readTree(commitsResponse.getBody());
            if (!commitsArray.isArray() || commitsArray.isEmpty()) {
                return new ResultadoPaso("KO",
                        "El proyecto existe en GitLab pero no tiene ningún commit.");
            }

            JsonNode ultimo = commitsArray.get(0);
            String id = ultimo.path("id").asText("desconocido");
            String fecha = ultimo.path("created_at").asText("fecha-desconocida");

            String mensaje = "Repositorio GitLab válido. Último commit "
                    + id + " en fecha " + fecha + ".";

            return new ResultadoPaso("OK", mensaje);

        } catch (HttpClientErrorException e) {
            return new ResultadoPaso("KO",
                    "Error de GitLab (" + e.getStatusCode().value() + "): "
                            + e.getResponseBodyAsString());
        } catch (Exception e) {
            return new ResultadoPaso("KO",
                    "Error conectando con la API de GitLab: " + e.getMessage());
        }
    }
}
