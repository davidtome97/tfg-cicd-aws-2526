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

    @GetMapping("/paso1")
    public ResultadoPaso comprobarPaso1(
            @RequestParam Long appId,
            @RequestParam String token,
            @RequestParam String projectKey) {

        return sonarService.comprobarSonar(appId, token, projectKey);
    }

    @GetMapping("/paso2")
    public ResultadoPaso comprobarPaso2(
            @RequestParam Long appId,
            @RequestParam String projectKey) {

        return sonarService.comprobarIntegracionSonarGit(appId, null, projectKey);
    }
}