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
     * PASOS REALES del asistente para calcular progreso.
     * Excluyo RESUMEN_FINAL porque es un “estado calculado/visual”.
     *
     * Ajusta este set EXACTAMENTE a los pasos que uses en tu wizard.
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
     * Muestro el listado de aplicaciones del usuario autenticado.
     * ✅ Importante: TODO va por appId, así no se mezclan datos entre apps.
     */
    @GetMapping
    public String listarAplicaciones(Model model, Principal principal) {

        // 1) Usuario logueado
        String correo = principal.getName();
        Usuario propietario = usuarioService.obtenerPorCorreo(correo);

        // 2) Solo aplicaciones de este usuario
        List<Aplicacion> aplicaciones = aplicacionService.listarPorPropietario(propietario);

        // 3) Mapas por appId
        Map<Long, ProgresoDespliegue> progresoPorApp = new HashMap<>();
        Map<Long, String> estadoPorApp = new HashMap<>();
        Map<Long, EstadoControl> resumenFinalPorApp = new HashMap<>();

        int totalPasos = PASOS_REALES.size();

        for (Aplicacion app : aplicaciones) {
            Long appId = app.getId();

            // Traigo todos los controles de ESA app
            List<ControlDespliegue> controles = controlDespliegueRepository.findByAplicacionIdOrderByPasoAsc(appId);

            // Cuento OK solo en pasos reales (para que NO te “contamine” RESUMEN_FINAL)
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

            // (Opcional) estado del RESUMEN_FINAL si existe
            Optional<ControlDespliegue> resumen = controlDespliegueRepository.findByAplicacionIdAndPaso(
                    appId,
                    PasoDespliegue.RESUMEN_FINAL
            );
            resumenFinalPorApp.put(appId, resumen.map(ControlDespliegue::getEstado).orElse(EstadoControl.PENDIENTE));
        }

        model.addAttribute("aplicaciones", aplicaciones);
        model.addAttribute("progresoPorApp", progresoPorApp);
        model.addAttribute("estadoPorApp", estadoPorApp);
        model.addAttribute("resumenFinalPorApp", resumenFinalPorApp); // por si lo usas en UI
        model.addAttribute("totalPasos", totalPasos); // ✅ para el HTML

        return "aplicaciones";
    }

    /**
     * Formulario para crear una nueva aplicación.
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
     * Formulario para editar una aplicación existente.
     */
    @GetMapping("/editar/{id}")
    public String mostrarFormularioEditar(@PathVariable Long id, Model model, Principal principal) {

        // (Recomendado) validar ownership para que nadie edite apps de otros
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
     * Descarga TXT con variables.
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
     * Guardar aplicación nueva o editada.
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
     * Descarga ZIP de configuración.
     */
    @GetMapping("/{id}/zip")
    public void descargarZip(@PathVariable Long id, HttpServletResponse response, Principal principal) throws IOException {

        Aplicacion aplicacion = aplicacionService.obtenerPorId(id);
        validarPropietario(aplicacion, principal);

        zipGeneratorService.generarZipAplicacion(id, response);
    }

    /**
     * Eliminar aplicación.
     */
    @GetMapping("/eliminar/{id}")
    public String eliminarAplicacion(@PathVariable Long id, Principal principal) {

        Aplicacion aplicacion = aplicacionService.obtenerPorId(id);
        validarPropietario(aplicacion, principal);

        aplicacionService.eliminar(id);
        return "redirect:/aplicaciones";
    }

    /**
     * Helper: valida que la app pertenece al usuario logueado.
     * (Evita que otro usuario vea/borre/descargue cosas de otra app)
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