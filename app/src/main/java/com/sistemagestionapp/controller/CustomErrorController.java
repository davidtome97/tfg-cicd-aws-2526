package com.sistemagestionapp.controller;

import org.springframework.boot.web.error.ErrorAttributeOptions;
import org.springframework.boot.web.servlet.error.ErrorController;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.boot.web.servlet.error.ErrorAttributes;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.context.request.WebRequest;

import java.util.Map;

/**
 * Controlador encargado de gestionar los errores globales de la aplicación.
 *
 * He implementado la interfaz ErrorController para poder personalizar
 * el tratamiento de errores y evitar mostrar las páginas de error
 * por defecto de Spring Boot.
 *
 * Cuando ocurre cualquier error no controlado (404, 500, etc.),
 * Spring redirige automáticamente a la ruta "/error", que es gestionada
 * por este controlador.
 */
@Controller
public class CustomErrorController implements ErrorController {

    /**
     * Inyecto el componente ErrorAttributes, que me permite acceder
     * a la información detallada del error que se ha producido,
     * como el mensaje, el código de estado o la ruta solicitada.
     */
    @Autowired
    private ErrorAttributes errorAttributes;

    /**
     * Este método se ejecuta automáticamente cuando se produce
     * cualquier error en la aplicación.
     *
     * A partir del objeto WebRequest obtengo los detalles del error
     * generados por Spring Boot y los añado al modelo para que puedan
     * mostrarse en una vista personalizada.
     *
     * De esta forma puedo ofrecer al usuario una página de error
     * más clara y controlada, en lugar de la página genérica del framework.
     *
     * @param request objeto que representa la petición web y contiene la información del error
     * @param model objeto que utilizo para enviar los datos a la vista
     * @return el nombre de la plantilla Thymeleaf que muestra el error
     */
    @RequestMapping("/error")
    public String handleError(WebRequest request, Model model) {

        // Obtengo los detalles del error a partir del contexto de la petición
        // Incluyo explícitamente el mensaje del error para poder mostrarlo en la vista
        Map<String, Object> errorDetails = errorAttributes.getErrorAttributes(
                request,
                ErrorAttributeOptions.of(ErrorAttributeOptions.Include.MESSAGE)
        );

        // Añado los detalles del error al modelo para que estén disponibles en la plantilla
        model.addAttribute("errorDetails", errorDetails);

        // Devuelvo la vista personalizada de error (error.html)
        return "error";
    }
}