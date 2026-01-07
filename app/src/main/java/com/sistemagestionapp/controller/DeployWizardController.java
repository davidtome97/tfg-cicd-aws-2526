package com.sistemagestionapp.controller;

import com.sistemagestionapp.model.ControlDespliegue;
import com.sistemagestionapp.model.EstadoControl;
import com.sistemagestionapp.model.PasoDespliegue;
import com.sistemagestionapp.service.DeployWizardService;
import com.sistemagestionapp.service.Paso6PdfService;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.*;

@Controller
@RequestMapping("/wizard")
public class DeployWizardController {

    // Servicio que utilizo para centralizar toda la lógica del asistente de despliegue.
    // Aquí delego el guardado del estado de cada paso y sus mensajes.
    private final DeployWizardService deployWizardService;

    // Servicio responsable de generar el PDF con la guía de configuración de base de datos.
    private final Paso6PdfService paso6PdfService;

    public DeployWizardController(DeployWizardService deployWizardService,
                                  Paso6PdfService paso6PdfService) {
        this.deployWizardService = deployWizardService;
        this.paso6PdfService = paso6PdfService;
    }

    // Compruebo si el paso anterior al actual está marcado como OK.
    // Esto me permite forzar que el usuario siga el asistente en orden
    // y evitar que se salte pasos intermedios.
    private String bloquearSiPrevioNoOk(Long appId, PasoDespliegue pasoActual) {
        PasoDespliegue previo = pasoActual.getPasoPrevio();
        if (previo == null) return null;

        boolean previoOk = deployWizardService
                .obtenerControl(appId, previo)
                .map(c -> c.getEstado() == EstadoControl.OK)
                .orElse(false);

        if (previoOk) return null;

        // Si el paso previo no está completado, redirijo automáticamente a ese paso
        return "redirect:/wizard/" + toPath(previo) + "?appId=" + appId;
    }

    // Cargo en el modelo los datos comunes que necesitan todas las vistas del asistente:
    // el appId, el control del paso actual y si el paso está completado o no.
    private void cargarModeloPaso(Model model, Long appId, PasoDespliegue paso) {
        model.addAttribute("appId", appId);

        Optional<ControlDespliegue> controlOpt =
                deployWizardService.obtenerControl(appId, paso);

        controlOpt.ifPresent(c -> model.addAttribute("controlPaso", c));

        boolean pasoActualCompleto =
                controlOpt.map(c -> c.getEstado() == EstadoControl.OK).orElse(false);

        model.addAttribute("pasoActualCompleto", pasoActualCompleto);
    }

    // Traduce el enum PasoDespliegue a la ruta real del asistente.
    // Esto me evita hardcodear rutas por toda la clase.
    private String toPath(PasoDespliegue paso) {
        return switch (paso) {
            case PRIMER_COMMIT -> "paso0";
            case SONAR_ANALISIS -> "paso1";
            case SONAR_INTEGRACION_GIT -> "paso2";
            case REPOSITORIO_GIT -> "paso3";
            case IMAGEN_ECR -> "paso4";
            case BASE_DATOS -> "paso5";
            case DESPLIEGUE_EC2 -> "paso6";
            default -> "paso0";
        };
    }

    // Muestro el paso 0 del asistente, donde el usuario confirma el primer commit del repositorio.
    @GetMapping("/paso0")
    public String paso0(@RequestParam Long appId, Model model) {
        cargarModeloPaso(model, appId, PasoDespliegue.PRIMER_COMMIT);
        return "wizard/paso0";
    }

    // Paso 1: configuración y validación de SonarCloud.
    // Antes de mostrar la vista compruebo que el paso anterior esté completado.
    @GetMapping("/paso1")
    public String paso1(@RequestParam Long appId, Model model) {
        String redir = bloquearSiPrevioNoOk(appId, PasoDespliegue.SONAR_ANALISIS);
        if (redir != null) return redir;

        cargarModeloPaso(model, appId, PasoDespliegue.SONAR_ANALISIS);
        return "wizard/paso1";
    }

    // Paso 2: verificación de la integración entre SonarCloud y el repositorio Git.
    @GetMapping("/paso2")
    public String paso2(@RequestParam Long appId, Model model) {
        String redir = bloquearSiPrevioNoOk(appId, PasoDespliegue.SONAR_INTEGRACION_GIT);
        if (redir != null) return redir;

        cargarModeloPaso(model, appId, PasoDespliegue.SONAR_INTEGRACION_GIT);
        return "wizard/paso2";
    }

    // Paso 3: comprobación de que el repositorio Git existe y tiene commits.
    @GetMapping("/paso3")
    public String paso3(@RequestParam Long appId, Model model) {
        String redir = bloquearSiPrevioNoOk(appId, PasoDespliegue.REPOSITORIO_GIT);
        if (redir != null) return redir;

        cargarModeloPaso(model, appId, PasoDespliegue.REPOSITORIO_GIT);
        return "wizard/paso3";
    }

    // Paso 4: validación de la imagen Docker en Amazon ECR.
    @GetMapping("/paso4")
    public String paso4(@RequestParam Long appId, Model model) {
        String redir = bloquearSiPrevioNoOk(appId, PasoDespliegue.IMAGEN_ECR);
        if (redir != null) return redir;

        cargarModeloPaso(model, appId, PasoDespliegue.IMAGEN_ECR);
        return "wizard/paso4";
    }

    // Paso 5: configuración de la base de datos (local o remota).
    @GetMapping("/paso5")
    public String paso5(@RequestParam Long appId, Model model) {
        String redir = bloquearSiPrevioNoOk(appId, PasoDespliegue.BASE_DATOS);
        if (redir != null) return redir;

        cargarModeloPaso(model, appId, PasoDespliegue.BASE_DATOS);
        return "wizard/paso5";
    }

    // Paso 6: comprobación final del despliegue en EC2.
    @GetMapping("/paso6")
    public String paso6(@RequestParam Long appId, Model model) {
        String redir = bloquearSiPrevioNoOk(appId, PasoDespliegue.DESPLIEGUE_EC2);
        if (redir != null) return redir;

        cargarModeloPaso(model, appId, PasoDespliegue.DESPLIEGUE_EC2);
        return "wizard/paso6";
    }

    // Valido únicamente el formato del hash del commit.
    // No compruebo si existe realmente en Git porque este paso es de ayuda al usuario.
    @PostMapping(value = "/paso0/validar",
            consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public Map<String, String> validarPaso0(@RequestParam String commitHash) {

        Map<String, String> out = new HashMap<>();
        String hash = commitHash == null ? "" : commitHash.trim();

        if (!hash.matches("^[0-9a-fA-F]{7,40}$")) {
            out.put("estado", "KO");
            out.put("mensaje", "Hash inválido (7–40 caracteres hexadecimales).");
            return out;
        }

        out.put("estado", "OK");
        out.put("mensaje", "Formato de hash correcto.");
        return out;
    }

    // Confirmo el paso 0 y lo marco como completado en la base de datos.
    // A partir de aquí el usuario puede avanzar al siguiente paso del asistente.
    @PostMapping("/paso0/confirmar")
    public String confirmarPaso0(@RequestParam Long appId,
                                 @RequestParam(required = false) String commitHash) {

        String msg = "Repositorio inicializado y primer commit realizado.";
        if (commitHash != null && !commitHash.isBlank()) {
            msg += " Hash: " + commitHash.trim();
        }

        deployWizardService.marcarPaso(
                appId,
                PasoDespliegue.PRIMER_COMMIT,
                EstadoControl.OK,
                msg
        );

        return "redirect:/wizard/paso1?appId=" + appId;
    }

    // Genero el PDF con la guía de configuración de base de datos y,
    // al descargarlo, marco automáticamente el paso como completado.
    @GetMapping(value = "/paso6/pdf", produces = MediaType.APPLICATION_PDF_VALUE)
    public ResponseEntity<byte[]> descargarPdfPasoBaseDatos(
            @RequestParam Long appId,
            @RequestParam String mode,
            @RequestParam String engine,
            @RequestParam(required = false) Integer port
    ) {
        String redir = bloquearSiPrevioNoOk(appId, PasoDespliegue.BASE_DATOS);
        if (redir != null) {
            return ResponseEntity.status(302)
                    .location(URI.create(redir.replace("redirect:", "")))
                    .build();
        }

        byte[] pdf = paso6PdfService.generarPdf(appId, mode, engine, port);

        deployWizardService.marcarPaso(
                appId,
                PasoDespliegue.BASE_DATOS,
                EstadoControl.OK,
                "Guía de base de datos descargada (PDF)."
        );

        HttpHeaders headers = new HttpHeaders();
        headers.setContentDisposition(
                ContentDisposition.attachment()
                        .filename("bbdd-" + engine + "-" + mode + ".pdf")
                        .build()
        );

        return ResponseEntity.ok()
                .headers(headers)
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdf);
    }

    // Muestro un resumen final con el estado y el mensaje de todos los pasos del asistente.
    @GetMapping("/resumen")
    public String resumen(@RequestParam Long appId, Model model) {

        String redir = bloquearSiPrevioNoOk(appId, PasoDespliegue.DESPLIEGUE_EC2);
        if (redir != null) return redir;

        List<ControlDespliegue> controles =
                deployWizardService.obtenerControlesOrdenados(appId);

        Map<PasoDespliegue, ControlDespliegue> porPaso = new EnumMap<>(PasoDespliegue.class);
        controles.forEach(c -> porPaso.put(c.getPaso(), c));

        List<PasoView> pasos = List.of(
                PasoView.of(0, "Primer commit", porPaso.get(PasoDespliegue.PRIMER_COMMIT)),
                PasoView.of(1, "SonarCloud", porPaso.get(PasoDespliegue.SONAR_ANALISIS)),
                PasoView.of(2, "Integración Sonar ↔ Git", porPaso.get(PasoDespliegue.SONAR_INTEGRACION_GIT)),
                PasoView.of(3, "Repositorio Git", porPaso.get(PasoDespliegue.REPOSITORIO_GIT)),
                PasoView.of(4, "Imagen Docker en ECR", porPaso.get(PasoDespliegue.IMAGEN_ECR)),
                PasoView.of(5, "Base de datos", porPaso.get(PasoDespliegue.BASE_DATOS)),
                PasoView.of(6, "Despliegue en EC2", porPaso.get(PasoDespliegue.DESPLIEGUE_EC2))
        );

        model.addAttribute("appId", appId);
        model.addAttribute("pasos", pasos);
        return "wizard/resumen";
    }

    // DTO interno que utilizo únicamente para pasar la información
    // de cada paso a la vista de resumen.
    public static class PasoView {
        private final int numero;
        private final String titulo;
        private final String estado;
        private final String mensaje;

        public PasoView(int numero, String titulo, String estado, String mensaje) {
            this.numero = numero;
            this.titulo = titulo;
            this.estado = estado;
            this.mensaje = mensaje;
        }

        public static PasoView of(int numero, String titulo, ControlDespliegue c) {
            if (c == null) {
                return new PasoView(numero, titulo, "N/A", "Paso no ejecutado.");
            }
            return new PasoView(
                    numero,
                    titulo,
                    c.getEstado().name(),
                    c.getMensaje()
            );
        }

        public int getNumero() { return numero; }
        public String getTitulo() { return titulo; }
        public String getEstado() { return estado; }
        public String getMensaje() { return mensaje; }
    }
}