package com.sistemagestionapp.controller;

import com.sistemagestionapp.model.Aplicacion;
import com.sistemagestionapp.model.EstadoControl;
import com.sistemagestionapp.model.Lenguaje;
import com.sistemagestionapp.model.PasoDespliegue;
import com.sistemagestionapp.model.ProveedorCiCd;
import com.sistemagestionapp.model.TipoBaseDatos;
import com.sistemagestionapp.model.Usuario;
import com.sistemagestionapp.model.dto.ProgresoDespliegue;
import com.sistemagestionapp.repository.ControlDespliegueRepository;
import com.sistemagestionapp.service.AplicacionService;
import com.sistemagestionapp.service.UsuarioService;
import com.sistemagestionapp.service.VariablesSecretTxtService;
import com.sistemagestionapp.service.ZipGeneratorService;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.security.Principal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/aplicaciones")
public class AplicacionController {

    @Autowired
    private AplicacionService aplicacionService;

    @Autowired
    private UsuarioService usuarioService;

    @Autowired
    private ZipGeneratorService zipGeneratorService;

    @Autowired
    private VariablesSecretTxtService variablesSecretTxtService;

    @Autowired
    private ControlDespliegueRepository controlDespliegueRepository;

    @GetMapping
    public String listarAplicaciones(Model model, Principal principal) {
        String correo = principal.getName();
        Usuario propietario = usuarioService.obtenerPorCorreo(correo);

        List<Aplicacion> aplicaciones = aplicacionService.listarPorPropietario(propietario);

        Map<Long, ProgresoDespliegue> progresoPorApp = new HashMap<>();
        Map<Long, String> estadoPorApp = new HashMap<>();

        int totalPasos = PasoDespliegue.values().length;

        for (Aplicacion app : aplicaciones) {
            long ok = controlDespliegueRepository
                    .countByAplicacionIdAndEstado(app.getId(), EstadoControl.OK);

            progresoPorApp.put(app.getId(), new ProgresoDespliegue(ok, totalPasos));

            String estado;
            if (ok == 0) {
                estado = "PENDIENTE";
            } else if (ok >= totalPasos) {
                estado = "OK";
            } else {
                estado = "EN PROGRESO";
            }

            estadoPorApp.put(app.getId(), estado);
        }

        model.addAttribute("aplicaciones", aplicaciones);
        model.addAttribute("progresoPorApp", progresoPorApp);
        model.addAttribute("estadoPorApp", estadoPorApp);

        return "aplicaciones";
    }

    @GetMapping("/nueva")
    public String mostrarFormularioNueva(Model model) {
        Aplicacion aplicacion = new Aplicacion();

        model.addAttribute("aplicacion", aplicacion);
        model.addAttribute("lenguajes", Lenguaje.values());
        model.addAttribute("tiposBaseDatos", TipoBaseDatos.values());
        model.addAttribute("proveedoresCiCd", ProveedorCiCd.values());

        return "aplicacion-form";
    }

    @GetMapping("/editar/{id}")
    public String mostrarFormularioEditar(@PathVariable Long id, Model model) {
        Aplicacion aplicacion = aplicacionService.obtenerPorId(id);

        model.addAttribute("aplicacion", aplicacion);
        model.addAttribute("lenguajes", Lenguaje.values());
        model.addAttribute("tiposBaseDatos", TipoBaseDatos.values());
        model.addAttribute("proveedoresCiCd", ProveedorCiCd.values());

        return "aplicacion-form";
    }

    @GetMapping("/{id}/variables")
    public void descargarVariables(@PathVariable Long id,
                                   HttpServletResponse response) throws IOException {

        Aplicacion aplicacion = aplicacionService.obtenerPorId(id);

        String txt = variablesSecretTxtService.generarTxt(aplicacion, null);
        String filename = variablesSecretTxtService.nombreFichero(aplicacion);

        response.setContentType("text/plain; charset=UTF-8");
        response.setHeader("Content-Disposition", "attachment; filename=\"" + filename + "\"");

        response.getWriter().write(txt);
        response.flushBuffer();
    }

    @PostMapping("/guardar")
    public String guardarAplicacion(@ModelAttribute("aplicacion") Aplicacion aplicacion,
                                    Principal principal) {

        String correo = principal.getName();
        Usuario propietario = usuarioService.obtenerPorCorreo(correo);
        aplicacion.setPropietario(propietario);

        aplicacionService.guardar(aplicacion);

        return "redirect:/aplicaciones";
    }

    @GetMapping("/{id}/zip")
    public void descargarZip(@PathVariable Long id, HttpServletResponse response) throws IOException {
        zipGeneratorService.generarZipAplicacion(id, response);
    }

    @GetMapping("/eliminar/{id}")
    public String eliminarAplicacion(@PathVariable Long id) {
        aplicacionService.eliminar(id);
        return "redirect:/aplicaciones";
    }
}