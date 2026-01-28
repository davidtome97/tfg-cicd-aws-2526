package com.sistemagestionapp.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * En este controlador gestiono la navegación a la sección de ayuda de la aplicación.
 *
 * Me limito a exponer la ruta que muestra la vista de ayuda, sin lógica adicional,
 * ya que su contenido es únicamente informativo.
 *
 * @author David Tomé Arnaiz
 */
@Controller
public class AyudaController {

    /**
     * En este endpoint devuelvo la vista asociada a la página de ayuda.
     *
     * @return nombre de la plantilla que renderiza la página de ayuda
     * @author David Tomé Arnaiz
     */
    @GetMapping("/ayuda")
    public String ayuda() {
        return "ayuda";
    }
}