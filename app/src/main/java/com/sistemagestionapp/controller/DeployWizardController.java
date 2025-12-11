package com.sistemagestionapp.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/wizard")
public class DeployWizardController {

    @GetMapping("/paso1")
    public String mostrarPaso1() {
        // Busca templates/wizard/paso1.html
        return "wizard/paso1";
    }

    @GetMapping("/paso2")
    public String mostrarPaso2() {
        return "wizard/paso2";
    }

    @GetMapping("/paso3")
    public String mostrarPaso3() {
        return "wizard/paso3";
    }

    @GetMapping("/paso4")
    public String mostrarPaso4() {
        return "wizard/paso4";
    }

    @GetMapping("/paso5")
    public String mostrarPaso5() {
        return "wizard/paso5";
    }

    @GetMapping("/resumen")
    public String mostrarResumen() {
        return "wizard/resumen";
    }
}
