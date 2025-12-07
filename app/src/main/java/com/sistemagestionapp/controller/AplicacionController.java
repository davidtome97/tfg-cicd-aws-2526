package com.sistemagestionapp.controller;

import com.sistemagestionapp.model.Aplicacion;
import com.sistemagestionapp.model.Usuario;
import com.sistemagestionapp.service.AplicacionService;
import com.sistemagestionapp.service.UsuarioService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.User;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Controlador para la ventana "Listado de aplicaciones".
 * Muestra las aplicaciones del usuario logueado y el estado
 * de los pasos de despliegue.
 */
@Controller
public class AplicacionController {

    @Autowired
    private AplicacionService aplicacionService;

    @Autowired
    private UsuarioService usuarioService;

    /**
     * Listado de aplicaciones del usuario autenticado.
     */
    @GetMapping("/aplicaciones")
    public String listarAplicaciones(@AuthenticationPrincipal User userDetails,
                                     Model model) {

        // Usuario autenticado (correo = username en Spring Security)
        String correo = userDetails.getUsername();
        Usuario propietario = usuarioService.obtenerPorCorreo(correo);

        // Lista de aplicaciones de ese usuario
        List<Aplicacion> aplicaciones = aplicacionService.listarPorUsuario(propietario);

        // Mapa <idAplicacion, pasosOk> para mostrar en la tabla
        Map<Long, Integer> pasosOkPorAplicacion = new HashMap<>();
        int totalPasos = aplicacionService.getTotalPasos();

        for (Aplicacion app : aplicaciones) {
            int pasosOk = aplicacionService.contarPasosOk(app);
            pasosOkPorAplicacion.put(app.getId(), pasosOk);
        }

        model.addAttribute("aplicaciones", aplicaciones);
        model.addAttribute("pasosOkPorAplicacion", pasosOkPorAplicacion);
        model.addAttribute("totalPasos", totalPasos);

        return "aplicaciones"; // plantilla aplicaciones.html
    }
}