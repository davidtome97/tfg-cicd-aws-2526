package com.sistemagestionapp.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Esta clase se encarga de controlar el acceso a la vista del mapa dentro de la aplicación.
 * He utilizado la anotación {@link Controller} para que Spring Boot la reconozca como
 * un controlador web.
 * Mediante una petición GET, muestro la plantilla correspondiente al mapa.
 *
 * @author David Tomé Arnáiz
 */
@Controller
public class MapaController {

    /**
     * Este método gestiona las peticiones GET a la ruta "/mapa".
     * Devuelvo el nombre de la plantilla "mapa", que se encarga de renderizar
     * la vista con el mapa en el navegador.
     *
     * @return el nombre de la plantilla Thymeleaf que muestra el mapa.
     */
    @GetMapping("/mapa")
    public String verMapa() {
        return "mapa";
    }
}