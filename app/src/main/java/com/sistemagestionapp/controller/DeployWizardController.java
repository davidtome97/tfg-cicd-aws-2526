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

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/wizard")
public class DeployWizardController {

    private final DeployWizardService deployWizardService;
    private final Paso6PdfService paso6PdfService;

    public DeployWizardController(DeployWizardService deployWizardService,
                                  Paso6PdfService paso6PdfService) {
        this.deployWizardService = deployWizardService;
        this.paso6PdfService = paso6PdfService;
    }

    @GetMapping("/paso1")
    public String mostrarPaso1(@RequestParam Long appId, Model model) {
        model.addAttribute("appId", appId);
        deployWizardService.obtenerControl(appId, PasoDespliegue.SONAR_ANALISIS)
                .ifPresent(c -> model.addAttribute("controlPaso", c));
        return "wizard/paso1";
    }

    @GetMapping("/paso2")
    public String mostrarPaso2(@RequestParam Long appId, Model model) {
        model.addAttribute("appId", appId);
        deployWizardService.obtenerControl(appId, PasoDespliegue.SONAR_INTEGRACION_GIT)
                .ifPresent(c -> model.addAttribute("controlPaso", c));
        return "wizard/paso2";
    }

    @GetMapping("/paso3")
    public String mostrarPaso3(@RequestParam Long appId, Model model) {
        model.addAttribute("appId", appId);
        deployWizardService.obtenerControl(appId, PasoDespliegue.REPOSITORIO_GIT)
                .ifPresent(c -> model.addAttribute("controlPaso", c));
        return "wizard/paso3";
    }

    @GetMapping("/paso4")
    public String mostrarPaso4(@RequestParam Long appId, Model model) {
        model.addAttribute("appId", appId);
        deployWizardService.obtenerControl(appId, PasoDespliegue.IMAGEN_ECR)
                .ifPresent(c -> model.addAttribute("controlPaso", c));
        return "wizard/paso4";
    }

    @GetMapping("/paso5")
    public String mostrarPaso5(@RequestParam Long appId, Model model) {
        model.addAttribute("appId", appId);
        deployWizardService.obtenerControl(appId, PasoDespliegue.DESPLIEGUE_EC2)
                .ifPresent(c -> model.addAttribute("controlPaso", c));
        return "wizard/paso5";
    }

    @GetMapping("/paso6")
    public String mostrarPaso6(@RequestParam Long appId, Model model) {
        model.addAttribute("appId", appId);
        deployWizardService.obtenerControl(appId, PasoDespliegue.BASE_DATOS)
                .ifPresent(c -> model.addAttribute("controlPaso", c));
        return "wizard/paso6";
    }

   /* @GetMapping("/entry")
    public String entry(@RequestParam Long appId) {
        boolean paso0Ok = deployWizardService
                .obtenerControl(appId, PasoDespliegue.PRIMER_COMMIT)
                .map(c -> c.getEstado() == EstadoControl.OK)
                .orElse(false);

        if (paso0Ok) {
            return "redirect:/wizard/paso1?appId=" + appId;
        }
        return "redirect:/wizard/paso0?appId=" + appId;
    }*/

    @GetMapping("/paso0")
    public String mostrarPaso0(@RequestParam Long appId, Model model) {
        model.addAttribute("appId", appId);
        deployWizardService.obtenerControl(appId, PasoDespliegue.PRIMER_COMMIT)
                .ifPresent(c -> model.addAttribute("controlPaso", c));
        return "wizard/paso0";
    }

    @PostMapping("/paso0/confirmar")
    public String confirmarPaso0(@RequestParam Long appId,
                                 @RequestParam(value = "commitHash", required = false) String commitHash) {

        String msg = "Primer commit realizado y repositorio inicializado.";
        if (commitHash != null && !commitHash.isBlank()) {
            msg = "Primer commit realizado. Hash: " + commitHash.trim();
        }

        deployWizardService.marcarPaso(
                appId,
                PasoDespliegue.PRIMER_COMMIT,
                EstadoControl.OK,
                msg
        );

        return "redirect:/wizard/paso1?appId=" + appId;
    }


    /**
     * ✅ Descarga PDF Paso 6 + marca el paso como OK.
     * URL ejemplo:
     * /wizard/paso6/pdf?appId=1&mode=local&engine=postgres&port=5432
     */
    @GetMapping(value = "/paso6/pdf", produces = MediaType.APPLICATION_PDF_VALUE)
    public ResponseEntity<byte[]> descargarPdfPaso6(
            @RequestParam("appId") Long appId,
            @RequestParam("mode") String mode,
            @RequestParam("engine") String engine,
            @RequestParam(value = "port", required = false) Integer port
    ) {
        // 1) Normalizar inputs (evita "PostgreSQL", " postgres ", null, etc.)
        String safeMode = (mode == null || mode.isBlank()) ? "local" : mode.trim().toLowerCase();
        String safeEngine = (engine == null || engine.isBlank()) ? "postgres" : engine.trim().toLowerCase();

        // 2) Generar PDF con valores ya normalizados
        byte[] pdf = paso6PdfService.generarPdf(appId, safeMode, safeEngine, port);

        // 3) Si el PDF no se generó bien, NO marcar OK
        if (pdf == null || pdf.length == 0) {
            deployWizardService.marcarPaso(
                    appId,
                    PasoDespliegue.BASE_DATOS,
                    EstadoControl.PENDIENTE,
                    "No se pudo generar el PDF de configuración de base de datos."
            );
            return ResponseEntity.internalServerError().build();
        }

        // 4) Marcar Paso 6 en OK al descargar (guía completada)
        deployWizardService.marcarPaso(
                appId,
                PasoDespliegue.BASE_DATOS,
                EstadoControl.OK,
                "Guía de configuración de base de datos descargada (PDF)."
        );

        // 5) Nombre de fichero consistente
        String filename = "paso6-bbdd-" + safeEngine + "-" + safeMode + ".pdf";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.setContentDisposition(
                ContentDisposition.attachment().filename(filename).build()
        );

        return ResponseEntity.ok()
                .headers(headers)
                .body(pdf);
    }

    @GetMapping("/resumen")
    public String mostrarResumen(@RequestParam Long appId, Model model) {
        List<ControlDespliegue> controles = deployWizardService.obtenerControlesOrdenados(appId);

        Map<PasoDespliegue, ControlDespliegue> porPaso = new EnumMap<>(PasoDespliegue.class);
        for (ControlDespliegue c : controles) {
            if (c.getPaso() != null) porPaso.put(c.getPaso(), c);
        }

        List<PasoView> pasos = List.of(
                PasoView.of(0, "Primer commit (repositorio)", porPaso.get(PasoDespliegue.PRIMER_COMMIT)),
                PasoView.of(1, "SonarCloud", porPaso.get(PasoDespliegue.SONAR_ANALISIS)),
                PasoView.of(2, "Integración Sonar ↔ Git", porPaso.get(PasoDespliegue.SONAR_INTEGRACION_GIT)),
                PasoView.of(3, "Repositorio Git", porPaso.get(PasoDespliegue.REPOSITORIO_GIT)),
                PasoView.of(4, "Imagen Docker en ECR", porPaso.get(PasoDespliegue.IMAGEN_ECR)),
                PasoView.of(5, "Despliegue en EC2", porPaso.get(PasoDespliegue.DESPLIEGUE_EC2)),
                PasoView.of(6, "Base de datos", porPaso.get(PasoDespliegue.BASE_DATOS)),
                PasoView.of(7, "Resumen final", porPaso.get(PasoDespliegue.RESUMEN_FINAL))
        );

        model.addAttribute("appId", appId);
        model.addAttribute("pasos", pasos);
        return "wizard/resumen";
    }

    /** DTO simple para la vista */
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
            if (c == null) return new PasoView(numero, titulo, "N/A", "Este paso no se ha ejecutado todavía.");
            String est = (c.getEstado() != null) ? c.getEstado().name() : "N/A";
            String msg = (c.getMensaje() != null && !c.getMensaje().isBlank()) ? c.getMensaje() : "";
            return new PasoView(numero, titulo, est, msg);
        }

        public int getNumero() { return numero; }
        public String getTitulo() { return titulo; }
        public String getEstado() { return estado; }
        public String getMensaje() { return mensaje; }
    }
}