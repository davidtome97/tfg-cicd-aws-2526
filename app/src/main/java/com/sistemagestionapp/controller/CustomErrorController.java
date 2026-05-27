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
 * En este controlador gestiono los errores globales de la aplicación.
 *
 * Implemento la interfaz {@link ErrorController} para interceptar la ruta "/error" y
 * mostrar una página de error personalizada, evitando el uso de las páginas por defecto
 * de Spring Boot.
 *
 * @author David Tomé Arnaiz
 */
@Controller
public class CustomErrorController implements ErrorController {

    /**
     * Componente que utilizo para acceder a la información asociada al error producido.
     */
    @Autowired
    private ErrorAttributes errorAttributes;

    /**
     * En este método capturo cualquier error no controlado que se produzca en la aplicación.
     *
     * Obtengo los atributos del error a partir del contexto de la petición y los añado al modelo
     * para que puedan mostrarse en una vista personalizada.
     *
     * @param request petición web que contiene la información del error
     * @param model modelo utilizado para enviar los datos a la vista
     * @return nombre de la plantilla que renderiza la página de error
     * @author David Tomé Arnaiz
     */
    @RequestMapping("/error")
    public String handleError(WebRequest request, Model model) {

        Map<String, Object> errorDetails = errorAttributes.getErrorAttributes(
                request,
                ErrorAttributeOptions.of(ErrorAttributeOptions.Include.MESSAGE)
        );

        model.addAttribute("errorDetails", errorDetails);

        return "error";
    }
}