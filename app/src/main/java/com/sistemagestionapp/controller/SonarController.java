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

    private final SonarService sonarService;

    public SonarController(SonarService sonarService) {
        this.sonarService = sonarService;
    }

    // PASO 1 – Comprueba token + projectKey (existe el proyecto)
    @GetMapping("/paso1")
    public ResultadoPaso comprobarPaso1(
            @RequestParam String token,
            @RequestParam String projectKey) {

        return sonarService.comprobarSonar(token, projectKey);
    }

    // PASO 2 – Solo necesita el projectKey (comprueba análisis Sonar–Git)
    @GetMapping("/paso2")
    public ResultadoPaso comprobarPaso2(
            @RequestParam String projectKey) {

        // El método ya ignora el token, solo mira los análisis del proyecto
        return sonarService.comprobarIntegracionSonarGit(null, projectKey);
    }
}