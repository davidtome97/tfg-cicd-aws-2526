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
 * En este servicio valido que un repositorio remoto exista en GitHub o GitLab y que tenga al menos un commit.
 *
 * Utilizo esta comprobación en el paso 3 del asistente para asegurar que el repositorio está listo para
 * integrarse en un flujo de CI/CD. Como resultado, devuelvo un {@link ResultadoPaso} y persisto el estado
 * del paso en la base de datos mediante {@link DeployWizardService}.
 *
 * @author David Tomé Arnaiz
 */
@Service
public class GitService {

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final DeployWizardService deployWizardService;

    /**
     * En este constructor inicializo las dependencias necesarias para consultar las APIs públicas de Git.
     *
     * @param deployWizardService servicio que utilizo para registrar el resultado del paso
     */
    public GitService(DeployWizardService deployWizardService) {
        this.restTemplate = new RestTemplate();
        this.objectMapper = new ObjectMapper();
        this.deployWizardService = deployWizardService;
    }

    /**
     * Compruebo la validez de un repositorio en función del proveedor indicado.
     *
     * Verifico un formato mínimo de repositorio y ejecuto la validación contra la API pública de GitHub
     * o GitLab. Si el repositorio existe pero no tiene commits, considero la comprobación fallida.
     *
     * @param aplicacionId identificador de la aplicación
     * @param proveedor proveedor Git (github o gitlab)
     * @param repoPath ruta del repositorio (por ejemplo, owner/repo o grupo/proyecto)
     * @return resultado de la comprobación
     */
    public ResultadoPaso comprobarRepositorio(Long aplicacionId, String proveedor, String repoPath) {

        if (proveedor == null || proveedor.isBlank() || repoPath == null || repoPath.isBlank()) {
            ResultadoPaso r = new ResultadoPaso(
                    "KO",
                    "Proveedor o repositorio vacío. Ejemplo: proveedor=github, repo=owner/repositorio"
            );
            persistir(aplicacionId, PasoDespliegue.REPOSITORIO_GIT, r);
            return r;
        }

        String repo = repoPath.trim();

        if (!repo.contains("/") || repo.startsWith("/") || repo.endsWith("/")) {
            ResultadoPaso r = new ResultadoPaso(
                    "KO",
                    "Formato de repositorio inválido. Debe ser 'owner/repo' (GitHub) o 'grupo/proyecto' (GitLab)."
            );
            persistir(aplicacionId, PasoDespliegue.REPOSITORIO_GIT, r);
            return r;
        }

        ResultadoPaso resultado;

        try {
            String proveedorLower = proveedor.trim().toLowerCase();

            resultado = switch (proveedorLower) {
                case "github" -> comprobarGitHub(repo);
                case "gitlab" -> comprobarGitLab(repo);
                default -> new ResultadoPaso("KO", "Proveedor no soportado. Usa 'github' o 'gitlab'.");
            };

        } catch (Exception e) {
            resultado = new ResultadoPaso("KO", "Error comprobando el repositorio Git: " + e.getMessage());
        }

        persistir(aplicacionId, PasoDespliegue.REPOSITORIO_GIT, resultado);
        return resultado;
    }

    /**
     * Compruebo un repositorio de GitHub mediante su API pública.
     *
     * Verifico que el repositorio exista y solicito el último commit para asegurar que hay historial.
     *
     * @param repoPath ruta del repositorio en formato owner/repo
     * @return resultado de la comprobación
     */
    private ResultadoPaso comprobarGitHub(String repoPath) {
        try {
            String baseUrl = "https://api.github.com";

            String repoUrl = baseUrl + "/repos/" + repoPath;
            ResponseEntity<String> repoResponse = restTemplate.getForEntity(repoUrl, String.class);

            if (!repoResponse.getStatusCode().is2xxSuccessful()) {
                return new ResultadoPaso(
                        "KO",
                        "GitHub respondió con código " + repoResponse.getStatusCode().value()
                                + " al consultar el repositorio."
                );
            }

            String commitsUrl = baseUrl + "/repos/" + repoPath + "/commits?per_page=1";
            ResponseEntity<String> commitsResponse = restTemplate.getForEntity(commitsUrl, String.class);

            if (!commitsResponse.getStatusCode().is2xxSuccessful() || commitsResponse.getBody() == null) {
                return new ResultadoPaso("KO", "No se ha podido obtener la lista de commits del repositorio en GitHub.");
            }

            JsonNode commitsArray = objectMapper.readTree(commitsResponse.getBody());
            if (!commitsArray.isArray() || commitsArray.isEmpty()) {
                return new ResultadoPaso("KO", "El repositorio existe en GitHub pero no tiene ningún commit.");
            }

            JsonNode ultimoCommit = commitsArray.get(0);
            String sha = ultimoCommit.path("sha").asText("desconocido");
            String fecha = ultimoCommit.path("commit").path("author").path("date").asText("fecha-desconocida");

            return new ResultadoPaso("OK", "Repositorio GitHub válido. Último commit " + sha + " en fecha " + fecha + ".");

        } catch (HttpClientErrorException.NotFound e) {
            return new ResultadoPaso("KO", "El repositorio no existe en GitHub: " + repoPath);

        } catch (HttpClientErrorException e) {
            return new ResultadoPaso(
                    "KO",
                    "Error de GitHub (" + e.getStatusCode().value() + "): " + e.getResponseBodyAsString()
            );

        } catch (Exception e) {
            return new ResultadoPaso("KO", "Error conectando con la API de GitHub: " + e.getMessage());
        }
    }

    /**
     * Compruebo un repositorio de GitLab mediante su API pública.
     *
     * Busco el proyecto por nombre y filtro por {@code path_with_namespace} para localizar el repositorio exacto.
     * Después solicito el último commit para asegurar que existe historial.
     *
     * @param repoPath ruta del repositorio en formato grupo/proyecto
     * @return resultado de la comprobación
     */
    private ResultadoPaso comprobarGitLab(String repoPath) {
        try {
            String baseUrl = "https://gitlab.com/api/v4";

            String projectName = repoPath.substring(repoPath.lastIndexOf("/") + 1);

            String searchUrl = baseUrl + "/projects?search="
                    + URLEncoder.encode(projectName, StandardCharsets.UTF_8)
                    + "&simple=true&per_page=50";

            ResponseEntity<String> searchResponse = restTemplate.getForEntity(searchUrl, String.class);

            if (!searchResponse.getStatusCode().is2xxSuccessful() || searchResponse.getBody() == null) {
                return new ResultadoPaso("KO", "No se ha podido buscar el proyecto en GitLab.");
            }

            JsonNode projectsArray = objectMapper.readTree(searchResponse.getBody());
            if (!projectsArray.isArray() || projectsArray.isEmpty()) {
                return new ResultadoPaso(
                        "KO",
                        "No se ha encontrado ningún proyecto en GitLab con nombre " + projectName + "."
                );
            }

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

            String commitsUrl = baseUrl + "/projects/" + projectId + "/repository/commits?per_page=1";
            ResponseEntity<String> commitsResponse = restTemplate.getForEntity(commitsUrl, String.class);

            if (!commitsResponse.getStatusCode().is2xxSuccessful() || commitsResponse.getBody() == null) {
                return new ResultadoPaso("KO", "No se ha podido obtener la lista de commits del proyecto en GitLab.");
            }

            JsonNode commitsArray = objectMapper.readTree(commitsResponse.getBody());
            if (!commitsArray.isArray() || commitsArray.isEmpty()) {
                return new ResultadoPaso("KO", "El proyecto existe en GitLab pero no tiene ningún commit.");
            }

            JsonNode ultimo = commitsArray.get(0);
            String id = ultimo.path("id").asText("desconocido");
            String fecha = ultimo.path("created_at").asText("fecha-desconocida");

            return new ResultadoPaso("OK", "Repositorio GitLab válido. Último commit " + id + " en fecha " + fecha + ".");

        } catch (HttpClientErrorException e) {
            return new ResultadoPaso(
                    "KO",
                    "Error de GitLab (" + e.getStatusCode().value() + "): " + e.getResponseBodyAsString()
            );

        } catch (Exception e) {
            return new ResultadoPaso("KO", "Error conectando con la API de GitLab: " + e.getMessage());
        }
    }

    /**
     * Persisto el resultado del paso convirtiendo el estado textual (OK/KO) al enum {@link EstadoControl}.
     *
     * @param aplicacionId identificador de la aplicación
     * @param paso paso del asistente a registrar
     * @param r resultado obtenido en la comprobación
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
