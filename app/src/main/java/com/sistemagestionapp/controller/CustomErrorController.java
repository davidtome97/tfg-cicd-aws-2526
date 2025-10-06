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
 * Esta clase se encarga de gestionar los errores que ocurren en la aplicación.
 * He implementado la interfaz {@link ErrorController} para personalizar la forma
 * en que se manejan los errores del sistema.
 * Cuando ocurre un error, capturo la información relevante utilizando
 * {@link ErrorAttributes} y la paso al modelo para que se muestre en la plantilla
 * de error correspondiente.
 *
 * @author David Tomé Arnáiz
 */
@Controller
public class CustomErrorController implements ErrorController {

    /**
     * Inyecto el componente que me permite acceder a los detalles del error.
     */
    @Autowired
    private ErrorAttributes errorAttributes;

    /**
     * Este método se ejecuta automáticamente cuando ocurre un error en la aplicación.
     * Recopilo los detalles del error desde el {@link WebRequest} y los añado al modelo
     * para que se muestren en la vista "error.html".
     *
     * @param request objeto que representa la solicitud web y contiene la información del error.
     * @param model objeto que utilizo para enviar datos a la vista.
     * @return el nombre de la plantilla que muestra los detalles del error.
     */
    @RequestMapping("/error")
    public String handleError(WebRequest request, Model model) {
        // Obtengo los detalles del error usando el objeto 'request'
        Map<String, Object> errorDetails = errorAttributes.getErrorAttributes(
                request,
                ErrorAttributeOptions.of(ErrorAttributeOptions.Include.MESSAGE) // Incluyo el mensaje del error
        );

        // Paso los detalles del error a la vista para que se muestren
        model.addAttribute("errorDetails", errorDetails);

        // Devuelvo la plantilla "error.html"
        return "error";
    }
}