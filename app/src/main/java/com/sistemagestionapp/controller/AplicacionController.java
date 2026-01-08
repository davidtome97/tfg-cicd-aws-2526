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

/**
 * Controlador encargado de la gestión completa de las aplicaciones del usuario.
 * Desde aquí gestiono:
 * - El listado de aplicaciones
 * - La creación y edición
 * - El progreso del asistente de despliegue
 * - La descarga de variables y ficheros ZIP
 */
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

    /**
     * Muestro el listado de aplicaciones del usuario autenticado.
     * Obtengo el usuario desde el Principal (Spring Security) y solo
     * cargo las aplicaciones que le pertenecen.
     *
     * Además, calculo el progreso del asistente de despliegue para
     * cada aplicación, mostrando cuántos pasos están en OK y el estado
     * general (PENDIENTE / EN PROGRESO / OK).
     */
    @GetMapping
    public String listarAplicaciones(Model model, Principal principal) {

        // Obtengo el correo del usuario logueado
        String correo = principal.getName();

        // Busco el usuario completo en base de datos
        Usuario propietario = usuarioService.obtenerPorCorreo(correo);

        // Cargo solo las aplicaciones del usuario autenticado
        List<Aplicacion> aplicaciones = aplicacionService.listarPorPropietario(propietario);

        Map<Long, ProgresoDespliegue> progresoPorApp = new HashMap<>();
        Map<Long, String> estadoPorApp = new HashMap<>();

        int totalPasos = PasoDespliegue.values().length;

        // Para cada aplicación calculo su progreso en el asistente
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

    /**
     * Muestro el formulario para crear una nueva aplicación.
     * Inicializo una aplicación vacía y cargo los valores de los enums
     * necesarios para los selects del formulario.
     */
    @GetMapping("/nueva")
    public String mostrarFormularioNueva(Model model) {

        Aplicacion aplicacion = new Aplicacion();

        model.addAttribute("aplicacion", aplicacion);
        model.addAttribute("lenguajes", Lenguaje.values());
        model.addAttribute("tiposProyecto", com.sistemagestionapp.model.TipoProyecto.values());
        model.addAttribute("tiposBaseDatos", TipoBaseDatos.values());
        model.addAttribute("proveedoresCiCd", ProveedorCiCd.values());

        return "aplicacion-form";
    }

    /**
     * Muestro el formulario de edición de una aplicación existente.
     * Cargo la aplicación por su ID y reutilizo el mismo formulario
     * que para la creación.
     */
    @GetMapping("/editar/{id}")
    public String mostrarFormularioEditar(@PathVariable Long id, Model model) {

        Aplicacion aplicacion = aplicacionService.obtenerPorId(id);

        model.addAttribute("aplicacion", aplicacion);
        model.addAttribute("lenguajes", Lenguaje.values());
        model.addAttribute("tiposProyecto", com.sistemagestionapp.model.TipoProyecto.values());
        model.addAttribute("tiposBaseDatos", TipoBaseDatos.values());
        model.addAttribute("proveedoresCiCd", ProveedorCiCd.values());

        return "aplicacion-form";
    }

    /**
     * Genero y descargo un fichero TXT con las variables de entorno
     * necesarias para la aplicación.
     *
     * Este fichero sirve para que el usuario pueda configurar
     * GitHub Actions, GitLab CI o Jenkins de forma sencilla.
     */
    @GetMapping("/{id}/variables")
    public void descargarVariables(@PathVariable Long id, HttpServletResponse response) throws IOException {

        Aplicacion aplicacion = aplicacionService.obtenerPorId(id);

        String txt = variablesSecretTxtService.generarTxt(aplicacion, null);
        String filename = variablesSecretTxtService.nombreFichero(aplicacion);

        response.setContentType("text/plain; charset=UTF-8");
        response.setHeader("Content-Disposition", "attachment; filename=\"" + filename + "\"");

        response.getWriter().write(txt);
        response.flushBuffer();
    }

    /**
     * Guardo una aplicación nueva o editada.
     * Asocio siempre la aplicación al usuario autenticado para evitar
     * que se puedan crear aplicaciones sin propietario.
     */
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
     * Genero un ZIP con toda la configuración necesaria para la aplicación.
     * El contenido del ZIP depende del tipo de proyecto (Java o Python)
     * y del proveedor CI/CD seleccionado.
     *
     * Toda la lógica se delega al ZipGeneratorService.
     */
    @GetMapping("/{id}/zip")
    public void descargarZip(@PathVariable Long id, HttpServletResponse response) throws IOException {
        zipGeneratorService.generarZipAplicacion(id, response);
    }

    /**
     * Elimino una aplicación por su ID.
     * Tras eliminarla, redirijo de nuevo al listado de aplicaciones.
     */
    @GetMapping("/eliminar/{id}")
    public String eliminarAplicacion(@PathVariable Long id) {
        aplicacionService.eliminar(id);
        return "redirect:/aplicaciones";
    }
}