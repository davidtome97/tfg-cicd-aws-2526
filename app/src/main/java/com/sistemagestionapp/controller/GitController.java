package com.sistemagestionapp.controller;

import com.sistemagestionapp.model.dto.ResultadoPaso;
import com.sistemagestionapp.service.GitService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * En este controlador REST expongo endpoints del asistente relacionados con la validación del repositorio Git.
 *
 * Delego la comprobación real en {@link GitService} y devuelvo un {@link ResultadoPaso} en formato JSON
 * para que la interfaz pueda informar al usuario del resultado.
 *
 * @author David Tomé Arnaiz
 */
@RestController
@RequestMapping("/api/wizard")
public class GitController {

    private final GitService gitService;

    /**
     * En este constructor inyecto el servicio responsable de realizar comprobaciones contra el proveedor Git.
     *
     * @param gitService servicio que encapsula la lógica de validación de repositorios
     * @author David Tomé Arnaiz
     */
    public GitController(GitService gitService) {
        this.gitService = gitService;
    }

    /**
     * En este endpoint valido el repositorio indicado en el paso correspondiente del asistente.
     *
     * Recibo el identificador de aplicación para asociar el resultado a la aplicación correcta, y recibo el
     * proveedor y el repositorio para ejecutar la comprobación (por ejemplo, GitHub/GitLab y owner/repo).
     *
     * @param appId identificador de la aplicación
     * @param proveedor proveedor Git a validar
     * @param repo repositorio a validar
     * @return resultado de la comprobación del paso en formato JSON
     * @author David Tomé Arnaiz
     */
    @GetMapping("/paso3")
    public ResultadoPaso comprobarPaso3(
            @RequestParam Long appId,
            @RequestParam String proveedor,
            @RequestParam String repo) {

        return gitService.comprobarRepositorio(appId, proveedor, repo);
    }
}