package com.sistemagestionapp.controller;

import com.sistemagestionapp.model.Aplicacion;
import com.sistemagestionapp.model.Lenguaje;
import com.sistemagestionapp.model.ProveedorCiCd;
import com.sistemagestionapp.model.TipoBaseDatos;
import com.sistemagestionapp.model.Usuario;
import com.sistemagestionapp.service.AplicacionService;
import com.sistemagestionapp.service.UsuarioService;
import com.sistemagestionapp.service.ProyectoZipService;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.security.Principal;
import java.util.List;

/**
 * Controlador que utilizo para gestionar las aplicaciones del usuario:
 * - Listado de aplicaciones.
 * - Formulario de creación y edición.
 * - Eliminación.
 * - Descarga de proyecto demo (ZIP).
 */
@Controller
@RequestMapping("/aplicaciones")
public class AplicacionController {

    @Autowired
    private AplicacionService aplicacionService;

    @Autowired
    private UsuarioService usuarioService;

    @Autowired
    private ProyectoZipService proyectoZipService;

    /**
     * Muestro el listado de aplicaciones del usuario autenticado.
     */
    @GetMapping
    public String listarAplicaciones(Model model, Principal principal) {
        String correo = principal.getName();
        Usuario propietario = usuarioService.obtenerPorCorreo(correo);

        List<Aplicacion> aplicaciones = aplicacionService.listarPorPropietario(propietario);
        model.addAttribute("aplicaciones", aplicaciones);

        return "aplicaciones";
    }

    /**
     * Muestro el formulario para crear una nueva aplicación.
     */
    @GetMapping("/nueva")
    public String mostrarFormularioNueva(Model model) {
        Aplicacion aplicacion = new Aplicacion();

        model.addAttribute("aplicacion", aplicacion);
        model.addAttribute("lenguajes", Lenguaje.values());
        model.addAttribute("tiposBaseDatos", TipoBaseDatos.values());
        model.addAttribute("proveedoresCiCd", ProveedorCiCd.values());

        return "aplicacion-form";
    }

    /**
     * Muestro el formulario para editar una aplicación existente.
     */
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
     * Proceso el formulario de creación/edición de una aplicación.
     * Asigno siempre como propietario al usuario autenticado.
     */
    @PostMapping("/guardar")
    public String guardarAplicacion(@ModelAttribute("aplicacion") Aplicacion aplicacion,
                                    Principal principal) {

        String correo = principal.getName();
        Usuario propietario = usuarioService.obtenerPorCorreo(correo);
        aplicacion.setPropietario(propietario);

        aplicacionService.guardar(aplicacion);

        // De momento ambos botones (Guardar / Guardar y generar ZIP) hacen lo mismo
        return "redirect:/aplicaciones";
    }

    /**
     * Descargo el ZIP con el proyecto demo para esta aplicación.
     */
    @GetMapping("/{id}/zip")
    public void descargarZip(@PathVariable Long id, HttpServletResponse response) throws IOException {
        // 1) Busco la aplicación en BD
        Aplicacion aplicacion = aplicacionService.obtenerPorId(id);

        // 2) Genero el ZIP temporal con el proyecto Java + app-config.properties
        java.nio.file.Path zipPath = proyectoZipService.generarProyectoJava(aplicacion);

        // 3) Preparo la respuesta HTTP para que el navegador lo descargue
        response.setContentType("application/zip");
        response.setHeader(
                "Content-Disposition",
                "attachment; filename=\"" + zipPath.getFileName().toString() + "\""
        );

        java.nio.file.Files.copy(zipPath, response.getOutputStream());
        response.flushBuffer();
    }

    /**
     * Elimino una aplicación.
     */
    @GetMapping("/eliminar/{id}")
    public String eliminarAplicacion(@PathVariable Long id) {
        aplicacionService.eliminar(id);
        return "redirect:/aplicaciones";
    }
}