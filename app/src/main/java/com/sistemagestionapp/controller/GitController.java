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

    // Servicio que utilizo para encapsular toda la lógica de comprobación contra Git (GitHub/GitLab)
    private final GitService gitService;

    // Inyecto el servicio por constructor para mantener el controlador simple y fácil de probar
    public GitController(GitService gitService) {
        this.gitService = gitService;
    }

    // Este endpoint lo uso en el paso 3 del asistente.
    // Recibo appId para registrar el resultado en la aplicación correspondiente.
    // Recibo proveedor y repo para saber dónde y qué repositorio tengo que validar.
    @GetMapping("/paso3")
    public ResultadoPaso comprobarPaso3(
            @RequestParam Long appId,
            @RequestParam String proveedor,
            @RequestParam String repo) {

        // Delego la comprobación al servicio:
        // yo aquí solo coordino la entrada/salida HTTP y devuelvo el ResultadoPaso a la UI.
        return gitService.comprobarRepositorio(appId, proveedor, repo);
    }
}