package com.sistemagestionapp.controller;

import com.sistemagestionapp.model.Aplicacion;
import com.sistemagestionapp.model.ControlDespliegue;
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
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * En este controlador gestiono el ciclo de vida de las aplicaciones del usuario autenticado.
 *
 * Me encargo de:
 * - mostrar el listado de aplicaciones,
 * - crear y editar aplicaciones,
 * - eliminar aplicaciones,
 * - exponer descargas (variables y ZIP),
 * - y calcular el progreso del asistente de despliegue para cada aplicación.
 *
 * Todas las operaciones se ejecutan asociadas a un identificador de aplicación (appId) y valido el propietario
 * para evitar accesos a recursos de otros usuarios.
 *
 * @author David Tomé Arnaiz
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
     * En este conjunto defino los pasos que considero "reales" para calcular el progreso del asistente.
     *
     * Excluyo el paso {@link PasoDespliegue#RESUMEN_FINAL} porque lo trato como un estado visual o calculado,
     * y no como un paso que el usuario complete.
     */
    private static final EnumSet<PasoDespliegue> PASOS_REALES = EnumSet.of(
            PasoDespliegue.PRIMER_COMMIT,
            PasoDespliegue.SONAR_ANALISIS,
            PasoDespliegue.SONAR_INTEGRACION_GIT,
            PasoDespliegue.REPOSITORIO_GIT,
            PasoDespliegue.IMAGEN_ECR,
            PasoDespliegue.BASE_DATOS,
            PasoDespliegue.DESPLIEGUE_EC2
    );

    /**
     * En este endpoint muestro el listado de aplicaciones del usuario autenticado y calculo su progreso de despliegue.
     *
     * Construyo mapas indexados por appId con el progreso, el estado textual y el estado del resumen final,
     * de forma que la vista pueda renderizar esta información sin mezclar datos entre aplicaciones.
     *
     * @param model modelo de Spring MVC que utilizo para pasar datos a la vista
     * @param principal identidad del usuario autenticado
     * @return nombre de la plantilla que renderiza el listado de aplicaciones
     * @author David Tomé Arnaiz
     */
    @GetMapping
    public String listarAplicaciones(Model model, Principal principal) {

        String correo = principal.getName();
        Usuario propietario = usuarioService.obtenerPorCorreo(correo);

        List<Aplicacion> aplicaciones = aplicacionService.listarPorPropietario(propietario);

        Map<Long, ProgresoDespliegue> progresoPorApp = new HashMap<>();
        Map<Long, String> estadoPorApp = new HashMap<>();
        Map<Long, EstadoControl> resumenFinalPorApp = new HashMap<>();

        int totalPasos = PASOS_REALES.size();

        for (Aplicacion app : aplicaciones) {
            Long appId = app.getId();

            List<ControlDespliegue> controles = controlDespliegueRepository.findByAplicacionIdOrderByPasoAsc(appId);

            long ok = controles.stream()
                    .filter(c -> c.getPaso() != null && PASOS_REALES.contains(c.getPaso()))
                    .filter(c -> c.getEstado() == EstadoControl.OK)
                    .count();

            progresoPorApp.put(appId, new ProgresoDespliegue(ok, totalPasos));

            String estado;
            if (ok == 0) {
                estado = "PENDIENTE";
            } else if (ok >= totalPasos) {
                estado = "OK";
            } else {
                estado = "EN PROGRESO";
            }
            estadoPorApp.put(appId, estado);

            Optional<ControlDespliegue> resumen = controlDespliegueRepository.findByAplicacionIdAndPaso(
                    appId,
                    PasoDespliegue.RESUMEN_FINAL
            );
            resumenFinalPorApp.put(appId, resumen.map(ControlDespliegue::getEstado).orElse(EstadoControl.PENDIENTE));
        }

        model.addAttribute("aplicaciones", aplicaciones);
        model.addAttribute("progresoPorApp", progresoPorApp);
        model.addAttribute("estadoPorApp", estadoPorApp);
        model.addAttribute("resumenFinalPorApp", resumenFinalPorApp);
        model.addAttribute("totalPasos", totalPasos);

        return "aplicaciones";
    }

    /**
     * En este endpoint preparo el formulario para crear una nueva aplicación.
     *
     * Inicializo una instancia vacía y cargo los valores enumerados necesarios para los desplegables del formulario.
     *
     * @param model modelo de Spring MVC que utilizo para pasar datos a la vista
     * @return nombre de la plantilla del formulario de aplicación
     * @author David Tomé Arnaiz
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
     * En este endpoint preparo el formulario para editar una aplicación existente.
     *
     * Recupero la aplicación por id y valido que pertenezca al usuario autenticado antes de exponerla en la vista.
     *
     * @param id identificador de la aplicación a editar
     * @param model modelo de Spring MVC que utilizo para pasar datos a la vista
     * @param principal identidad del usuario autenticado
     * @return nombre de la plantilla del formulario de aplicación
     * @author David Tomé Arnaiz
     */
    @GetMapping("/editar/{id}")
    public String mostrarFormularioEditar(@PathVariable Long id, Model model, Principal principal) {

        Aplicacion aplicacion = aplicacionService.obtenerPorId(id);
        validarPropietario(aplicacion, principal);

        model.addAttribute("aplicacion", aplicacion);
        model.addAttribute("lenguajes", Lenguaje.values());
        model.addAttribute("tiposProyecto", com.sistemagestionapp.model.TipoProyecto.values());
        model.addAttribute("tiposBaseDatos", TipoBaseDatos.values());
        model.addAttribute("proveedoresCiCd", ProveedorCiCd.values());

        return "aplicacion-form";
    }

    /**
     * En este endpoint genero y descargo un fichero de texto con las variables de configuración asociadas a una aplicación.
     *
     * Valido que la aplicación pertenezca al usuario autenticado y devuelvo el contenido como adjunto (text/plain).
     *
     * @param id identificador de la aplicación
     * @param response respuesta HTTP donde escribo el fichero resultante
     * @param principal identidad del usuario autenticado
     * @throws IOException si se produce un error al escribir la respuesta
     * @author David Tomé Arnaiz
     */
    @GetMapping("/{id}/variables")
    public void descargarVariables(@PathVariable Long id, HttpServletResponse response, Principal principal) throws IOException {

        Aplicacion aplicacion = aplicacionService.obtenerPorId(id);
        validarPropietario(aplicacion, principal);

        String txt = variablesSecretTxtService.generarTxt(aplicacion, null);
        String filename = variablesSecretTxtService.nombreFichero(aplicacion);

        response.setContentType("text/plain; charset=UTF-8");
        response.setHeader("Content-Disposition", "attachment; filename=\"" + filename + "\"");

        response.getWriter().write(txt);
        response.flushBuffer();
    }

    /**
     * En este endpoint guardo una aplicación nueva o actualizo una existente.
     *
     * Asocio la aplicación al usuario autenticado y delego en la capa de servicio la persistencia.
     *
     * @param aplicacion entidad recibida desde el formulario
     * @param principal identidad del usuario autenticado
     * @return redirección al listado de aplicaciones
     * @author David Tomé Arnaiz
     */
    @PostMapping("/guardar")
    public String guardarAplicacion(@ModelAttribute("aplicacion") Aplicacion aplicacion, Principal principal) {

        String correo = principal.getName();
        Usuario propietario = usuarioService.obtenerPorCorreo(correo);

        aplicacion.setPropietario(propietario);
        aplicacionService.guardar(aplicacion);

        return "redirect:/aplicaciones";
    }

    /**
     * En este endpoint genero y descargo un ZIP con la configuración asociada a una aplicación.
     *
     * Valido que la aplicación pertenezca al usuario autenticado y delego la generación del ZIP en el servicio correspondiente.
     *
     * @param id identificador de la aplicación
     * @param response respuesta HTTP donde escribo el ZIP resultante
     * @param principal identidad del usuario autenticado
     * @throws IOException si se produce un error durante la escritura de la respuesta
     * @author David Tomé Arnaiz
     */
    @GetMapping("/{id}/zip")
    public void descargarZip(@PathVariable Long id, HttpServletResponse response, Principal principal) throws IOException {

        Aplicacion aplicacion = aplicacionService.obtenerPorId(id);
        validarPropietario(aplicacion, principal);

        zipGeneratorService.generarZipAplicacion(id, response);
    }

    /**
     * En este endpoint elimino una aplicación.
     *
     * Valido que la aplicación pertenezca al usuario autenticado antes de ejecutar el borrado.
     *
     * @param id identificador de la aplicación a eliminar
     * @param principal identidad del usuario autenticado
     * @return redirección al listado de aplicaciones
     * @author David Tomé Arnaiz
     */
    @GetMapping("/eliminar/{id}")
    public String eliminarAplicacion(@PathVariable Long id, Principal principal) {

        Aplicacion aplicacion = aplicacionService.obtenerPorId(id);
        validarPropietario(aplicacion, principal);

        aplicacionService.eliminar(id);
        return "redirect:/aplicaciones";
    }

    /**
     * En este método valido que la aplicación pertenezca al usuario autenticado.
     *
     * Si el usuario no coincide con el propietario de la aplicación, bloqueo la operación lanzando una excepción.
     *
     * @param app aplicación sobre la que verifico el propietario
     * @param principal identidad del usuario autenticado
     * @author David Tomé Arnaiz
     */
    private void validarPropietario(Aplicacion app, Principal principal) {
        if (app == null || app.getPropietario() == null || principal == null) {
            throw new RuntimeException("Acceso no permitido");
        }
        String correo = principal.getName();
        String correoProp = app.getPropietario().getCorreo();
        if (correoProp == null || !correoProp.equalsIgnoreCase(correo)) {
            throw new RuntimeException("Acceso no permitido");
        }
    }
}