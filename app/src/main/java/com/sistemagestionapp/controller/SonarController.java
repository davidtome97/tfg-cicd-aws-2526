package com.sistemagestionapp.controller;

import com.sistemagestionapp.model.dto.ResultadoPaso;
import com.sistemagestionapp.service.SonarService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * En este controlador REST expongo endpoints del asistente relacionados con SonarCloud.
 *
 * Delego la validación del token, la comprobación del projectKey y la verificación de la integración con Git
 * en {@link SonarService}. Devuelvo un {@link ResultadoPaso} en formato JSON para que la interfaz muestre el
 * resultado de cada comprobación.
 *
 * @author David Tomé Arnaiz
 */
@RestController
@RequestMapping("/api/wizard")
public class SonarController {

    private final SonarService sonarService;

    /**
     * En este constructor inyecto el servicio que encapsula la lógica de comprobación de SonarCloud.
     *
     * @param sonarService servicio responsable de validar la configuración de SonarCloud
     * @author David Tomé Arnaiz
     */
    public SonarController(SonarService sonarService) {
        this.sonarService = sonarService;
    }

    /**
     * En este endpoint valido la configuración de SonarCloud del paso 1 del asistente.
     *
     * Compruebo que el token es válido y que el projectKey existe y es accesible con dicho token para la aplicación indicada.
     *
     * @param appId identificador de la aplicación
     * @param token token de acceso a SonarCloud
     * @param projectKey clave del proyecto en SonarCloud
     * @return resultado de la comprobación del paso en formato JSON
     * @author David Tomé Arnaiz
     */
    @GetMapping("/paso1")
    public ResultadoPaso comprobarPaso1(
            @RequestParam Long appId,
            @RequestParam String token,
            @RequestParam String projectKey) {

        return sonarService.comprobarSonar(appId, token, projectKey);
    }

    /**
     * En este endpoint valido la integración SonarCloud ↔ Git del paso 2 del asistente.
     *
     * Verifico que el proyecto de SonarCloud está correctamente integrado con el repositorio Git asociado a la aplicación.
     *
     * @param appId identificador de la aplicación
     * @param projectKey clave del proyecto en SonarCloud
     * @return resultado de la comprobación del paso en formato JSON
     * @author David Tomé Arnaiz
     */
    @GetMapping("/paso2")
    public ResultadoPaso comprobarPaso2(
            @RequestParam Long appId,
            @RequestParam String projectKey) {

        return sonarService.comprobarIntegracionSonarGit(appId, null, projectKey);
    }
}