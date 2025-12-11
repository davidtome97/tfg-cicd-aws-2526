package com.sistemagestionapp.controller;

import com.sistemagestionapp.model.dto.ResultadoPaso;
import com.sistemagestionapp.service.GitService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/wizard")
public class GitController {

    private final GitService gitService;

    public GitController(GitService gitService) {
        this.gitService = gitService;
    }

    // PASO 3 – Comprobar repositorio Git
    @GetMapping("/paso3")
    public ResultadoPaso comprobarPaso3(
            @RequestParam String proveedor,
            @RequestParam String repo) {

        return gitService.comprobarRepositorio(proveedor, repo);
    }
}