package com.sistemagestionapp.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sistemagestionapp.model.EstadoControl;
import com.sistemagestionapp.model.PasoDespliegue;
import com.sistemagestionapp.model.dto.ResultadoPaso;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/**
 * En este servicio compruebo si un repositorio existe en GitHub o GitLab y si tiene al menos un commit.
 * Lo uso en el asistente (paso 3) para asegurar que el repositorio remoto está listo para CI/CD.
 */
@Service
public class GitService {

    // Uso RestTemplate para hacer llamadas HTTP a las APIs públicas de GitHub y GitLab.
    private final RestTemplate restTemplate;

    // Uso ObjectMapper para parsear el JSON que devuelven las APIs.
    private final ObjectMapper objectMapper;

    // Uso este servicio para guardar el estado del paso (OK/KO) y el mensaje en base de datos.
    private final DeployWizardService deployWizardService;

    public GitService(DeployWizardService deployWizardService) {
        this.restTemplate = new RestTemplate();
        this.objectMapper = new ObjectMapper();
        this.deployWizardService = deployWizardService;
    }

    /**
     * En este método decido qué API usar (GitHub o GitLab) y devuelvo un ResultadoPaso.
     * proveedor: "github" o "gitlab"
     * repoPath: por ejemplo "davidtome97/tfg-cicd-aws-2526"
     */
    public ResultadoPaso comprobarRepositorio(Long aplicacionId, String proveedor, String repoPath) {

        // Valido que me hayan enviado proveedor y ruta del repo.
        // Si no, devuelvo KO y no hago llamadas innecesarias a internet.
        if (proveedor == null || proveedor.isBlank() || repoPath == null || repoPath.isBlank()) {
            ResultadoPaso r = new ResultadoPaso(
                    "KO",
                    "Proveedor o repositorio vacío. Ejemplo: proveedor=github, repo=davidtome97/tfg-cicd-aws-2526"
            );
            persistir(aplicacionId, PasoDespliegue.REPOSITORIO_GIT, r);
            return r;
        }

        // Compruebo el formato mínimo "algo/algo" para evitar llamadas inútiles.
        if (!repoPath.contains("/") || repoPath.startsWith("/") || repoPath.endsWith("/")) {
            ResultadoPaso r = new ResultadoPaso(
                    "KO",
                    "Formato de repositorio inválido. Debe ser 'owner/repo' (GitHub) o 'grupo/proyecto' (GitLab)."
            );
            persistir(aplicacionId, PasoDespliegue.REPOSITORIO_GIT, r);
            return r;
        }

        ResultadoPaso resultado;

        try {
            // Paso el proveedor a minúsculas y quito espacios para comparar bien.
            String proveedorLower = proveedor.trim().toLowerCase();

            // Aquí elijo la comprobación según el proveedor.
            resultado = switch (proveedorLower) {
                case "github" -> comprobarGitHub(repoPath.trim());
                case "gitlab" -> comprobarGitLab(repoPath.trim());
                default -> new ResultadoPaso("KO", "Proveedor no soportado. Usa 'github' o 'gitlab'.");
            };

        } catch (Exception e) {
            // Capturo cualquier error inesperado para que el asistente no reviente.
            resultado = new ResultadoPaso("KO",
                    "Error comprobando el repositorio Git: " + e.getMessage());
        }

        // Guardo el resultado siempre para que quede trazabilidad del paso.
        persistir(aplicacionId, PasoDespliegue.REPOSITORIO_GIT, resultado);
        return resultado;
    }

    // Compruebo un repositorio de GitHub usando su API pública:
    // 1) verifico que existe /repos/{owner}/{repo}
    // 2) pido /commits?per_page=1 para asegurar que hay al menos un commit
    private ResultadoPaso comprobarGitHub(String repoPath) {
        try {
            String baseUrl = "https://api.github.com";

            // 1) Compruebo que el repositorio existe.
            String repoUrl = baseUrl + "/repos/" + repoPath;
            ResponseEntity<String> repoResponse = restTemplate.getForEntity(repoUrl, String.class);

            if (!repoResponse.getStatusCode().is2xxSuccessful()) {
                return new ResultadoPaso("KO",
                        "GitHub respondió con código " + repoResponse.getStatusCode().value()
                                + " al consultar el repositorio.");
            }

            // 2) Compruebo que hay al menos un commit pidiendo el último.
            String commitsUrl = baseUrl + "/repos/" + repoPath + "/commits?per_page=1";
            ResponseEntity<String> commitsResponse = restTemplate.getForEntity(commitsUrl, String.class);

            if (!commitsResponse.getStatusCode().is2xxSuccessful() || commitsResponse.getBody() == null) {
                return new ResultadoPaso("KO",
                        "No se ha podido obtener la lista de commits del repositorio en GitHub.");
            }

            JsonNode commitsArray = objectMapper.readTree(commitsResponse.getBody());
            if (!commitsArray.isArray() || commitsArray.isEmpty()) {
                return new ResultadoPaso("KO",
                        "El repositorio existe en GitHub pero no tiene ningún commit.");
            }

            // Extraigo un par de datos para dar un mensaje más útil (sha + fecha).
            JsonNode ultimoCommit = commitsArray.get(0);
            String sha = ultimoCommit.path("sha").asText("desconocido");
            String fecha = ultimoCommit.path("commit").path("author").path("date")
                    .asText("fecha-desconocida");

            return new ResultadoPaso("OK",
                    "Repositorio GitHub válido. Último commit " + sha + " en fecha " + fecha + ".");

        } catch (HttpClientErrorException.NotFound e) {
            // Si GitHub devuelve 404, el repo no existe o no es accesible públicamente.
            return new ResultadoPaso("KO", "El repositorio no existe en GitHub: " + repoPath);

        } catch (HttpClientErrorException e) {
            // Para otros errores (403 rate limit, etc.) devuelvo el código y el body.
            return new ResultadoPaso("KO",
                    "Error de GitHub (" + e.getStatusCode().value() + "): " + e.getResponseBodyAsString());

        } catch (Exception e) {
            // Cualquier otro error: red, JSON, etc.
            return new ResultadoPaso("KO", "Error conectando con la API de GitHub: " + e.getMessage());
        }
    }

    // Compruebo un repositorio en GitLab con su API pública:
    // 1) busco el proyecto por nombre
    // 2) filtro el resultado por path_with_namespace para encontrar el repo exacto
    // 3) pido el último commit con el projectId numérico
    private ResultadoPaso comprobarGitLab(String repoPath) {
        try {
            String baseUrl = "https://gitlab.com/api/v4";

            // En GitLab uso el nombre del proyecto para buscar y luego filtro por namespace completo.
            String projectName = repoPath.substring(repoPath.lastIndexOf("/") + 1);

            // 1) Busco proyectos por nombre (puede devolver varios resultados).
            String searchUrl = baseUrl + "/projects?search="
                    + URLEncoder.encode(projectName, StandardCharsets.UTF_8)
                    + "&simple=true&per_page=50";

            ResponseEntity<String> searchResponse = restTemplate.getForEntity(searchUrl, String.class);

            if (!searchResponse.getStatusCode().is2xxSuccessful() || searchResponse.getBody() == null) {
                return new ResultadoPaso("KO", "No se ha podido buscar el proyecto en GitLab.");
            }

            JsonNode projectsArray = objectMapper.readTree(searchResponse.getBody());
            if (!projectsArray.isArray() || projectsArray.isEmpty()) {
                return new ResultadoPaso("KO",
                        "No se ha encontrado ningún proyecto en GitLab con nombre " + projectName + ".");
            }

            // 2) Dentro de los resultados, busco el que coincida exactamente con path_with_namespace.
            JsonNode proyectoEncontrado = null;
            for (JsonNode proj : projectsArray) {
                String pathWithNamespace = proj.path("path_with_namespace").asText("");
                if (repoPath.equalsIgnoreCase(pathWithNamespace)) {
                    proyectoEncontrado = proj;
                    break;
                }
            }

            if (proyectoEncontrado == null) {
                return new ResultadoPaso("KO", "El repositorio no existe en GitLab: " + repoPath);
            }

            int projectId = proyectoEncontrado.path("id").asInt(-1);
            if (projectId == -1) {
                return new ResultadoPaso("KO", "Proyecto de GitLab encontrado pero sin ID válido.");
            }

            // 3) Pido el último commit del repositorio usando el ID del proyecto.
            String commitsUrl = baseUrl + "/projects/" + projectId + "/repository/commits?per_page=1";
            ResponseEntity<String> commitsResponse = restTemplate.getForEntity(commitsUrl, String.class);

            if (!commitsResponse.getStatusCode().is2xxSuccessful() || commitsResponse.getBody() == null) {
                return new ResultadoPaso("KO",
                        "No se ha podido obtener la lista de commits del proyecto en GitLab.");
            }

            JsonNode commitsArray = objectMapper.readTree(commitsResponse.getBody());
            if (!commitsArray.isArray() || commitsArray.isEmpty()) {
                return new ResultadoPaso("KO",
                        "El proyecto existe en GitLab pero no tiene ningún commit.");
            }

            // Devuelvo el ID del commit y la fecha para que el usuario vea que está vivo.
            JsonNode ultimo = commitsArray.get(0);
            String id = ultimo.path("id").asText("desconocido");
            String fecha = ultimo.path("created_at").asText("fecha-desconocida");

            return new ResultadoPaso("OK",
                    "Repositorio GitLab válido. Último commit " + id + " en fecha " + fecha + ".");

        } catch (HttpClientErrorException e) {
            // Aquí entran 401/403/404 o rate limit según el caso.
            return new ResultadoPaso("KO",
                    "Error de GitLab (" + e.getStatusCode().value() + "): " + e.getResponseBodyAsString());

        } catch (Exception e) {
            return new ResultadoPaso("KO", "Error conectando con la API de GitLab: " + e.getMessage());
        }
    }

    // En este método guardo el resultado del paso 3 en base de datos (OK o KO) para poder bloquear el avance.
    private void persistir(Long aplicacionId, PasoDespliegue paso, ResultadoPaso r) {
        if (aplicacionId == null) return;

        EstadoControl estado = "OK".equalsIgnoreCase(r.getEstado())
                ? EstadoControl.OK
                : EstadoControl.KO;

        deployWizardService.marcarPaso(aplicacionId, paso, estado, r.getMensaje());
    }
}
