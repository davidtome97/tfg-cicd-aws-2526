package com.sistemagestionapp.controller;

import com.sistemagestionapp.model.Aplicacion;
import com.sistemagestionapp.model.Lenguaje;
import com.sistemagestionapp.model.ProveedorCiCd;
import com.sistemagestionapp.model.TipoBaseDatos;
import com.sistemagestionapp.model.Usuario;
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
import java.util.List;

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

    @GetMapping
    public String listarAplicaciones(Model model, Principal principal) {
        String correo = principal.getName();
        Usuario propietario = usuarioService.obtenerPorCorreo(correo);

        List<Aplicacion> aplicaciones = aplicacionService.listarPorPropietario(propietario);
        model.addAttribute("aplicaciones", aplicaciones);

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

    /**
     * Descarga un .txt con variables/secrets sugeridas según la configuración.
     * Se alimenta con query params enviados desde el formulario:
     * - dbModo (local|remote)
     * - dbUri (opcional, sobre todo mongo remote)
     * - ecrRepository (opcional)
     */
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

    /**
     * Descarga el ZIP generado (JAVA o PYTHON) con pipeline + compose + config.
     */
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