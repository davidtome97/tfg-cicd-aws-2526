package com.sistemagestionapp.controller;

import com.sistemagestionapp.model.dto.ResultadoPaso;
import com.sistemagestionapp.service.SonarService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/wizard")
public class SonarController {

    // Servicio que utilizo para encapsular toda la lógica relacionada con SonarCloud
    // (validación de token, proyecto y comprobación de integraciones)
    private final SonarService sonarService;

    // Inyecto el servicio por constructor para seguir buenas prácticas
    // y mantener el controlador lo más simple posible
    public SonarController(SonarService sonarService) {
        this.sonarService = sonarService;
    }

    // Este endpoint corresponde al paso 1 del asistente.
    // Aquí compruebo que el token de SonarCloud es válido y que el projectKey existe
    // y es accesible con ese token.
    @GetMapping("/paso1")
    public ResultadoPaso comprobarPaso1(
            @RequestParam Long appId,
            @RequestParam String token,
            @RequestParam String projectKey) {

        // Delego toda la lógica de validación al servicio de Sonar
        // y devuelvo directamente el resultado para que lo consuma la interfaz
        return sonarService.comprobarSonar(appId, token, projectKey);
    }

    // Este endpoint corresponde al paso 2 del asistente.
    // En este paso compruebo que el proyecto de SonarCloud está correctamente
    // integrado con el repositorio Git (GitHub o GitLab).
    @GetMapping("/paso2")
    public ResultadoPaso comprobarPaso2(
            @RequestParam Long appId,
            @RequestParam String projectKey) {

        // En este caso no necesito token porque la integración ya debería estar configurada.
        // Paso null en el proveedor Git y dejo que el servicio se encargue de la lógica interna.
        return sonarService.comprobarIntegracionSonarGit(appId, null, projectKey);
    }
}