package com.sistemagestionapp.controller;

import com.sistemagestionapp.model.*;
import com.sistemagestionapp.repository.AplicacionRepository;
import com.sistemagestionapp.repository.ControlDespliegueRepository;
import com.sistemagestionapp.service.DeployWizardService;
import com.sistemagestionapp.service.Paso6PdfService;
import com.sistemagestionapp.util.RepoSlugUtil;
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

    private final DeployWizardService deployWizardService;
    private final Paso6PdfService paso6PdfService;
    private final AplicacionRepository aplicacionRepository;
    private final ControlDespliegueRepository controlDespliegueRepository;

    public DeployWizardController(DeployWizardService deployWizardService,
                                  Paso6PdfService paso6PdfService,
                                  AplicacionRepository aplicacionRepository,
                                  ControlDespliegueRepository controlDespliegueRepository) {

        this.deployWizardService = deployWizardService;
        this.paso6PdfService = paso6PdfService;
        this.aplicacionRepository = aplicacionRepository;
        this.controlDespliegueRepository = controlDespliegueRepository;
    }

    /* =========================================================
       ✅ UTILIDADES COMUNES
       ========================================================= */

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

    /**
     * Bloquea si el paso previo al "pasoActual" no está en OK.
     * Requiere PasoDespliegue#getPasoPrevio() correctamente definido.
     */
    private String bloquearSiPrevioNoOk(Long appId, PasoDespliegue pasoActual) {
        PasoDespliegue previo = pasoActual.getPasoPrevio();
        if (previo == null) return null;

        boolean previoOk = deployWizardService
                .obtenerControl(appId, previo)
                .map(c -> c.getEstado() == EstadoControl.OK)
                .orElse(false);

        if (previoOk) return null;

        return "redirect:/wizard/" + toPath(previo) + "?appId=" + appId;
    }

    /**
     * Carga datos comunes y precarga campos desde BD para que no se "pierdan" al navegar.
     */
    private void cargarModeloPaso(Model model, Long appId, PasoDespliegue paso) {
        model.addAttribute("appId", appId);

        Optional<ControlDespliegue> controlOpt = deployWizardService.obtenerControl(appId, paso);
        controlOpt.ifPresent(c -> model.addAttribute("controlPaso", c));

        boolean pasoActualCompleto =
                controlOpt.map(c -> c.getEstado() == EstadoControl.OK).orElse(false);

        model.addAttribute("pasoActualCompleto", pasoActualCompleto);

        // Cargamos la aplicación solo cuando realmente hace falta
        Aplicacion app = null;
        boolean necesitaApp =
                paso == PasoDespliegue.SONAR_ANALISIS ||
                        paso == PasoDespliegue.REPOSITORIO_GIT ||
                        paso == PasoDespliegue.IMAGEN_ECR ||
                        paso == PasoDespliegue.BASE_DATOS ||
                        paso == PasoDespliegue.DESPLIEGUE_EC2;

        if (necesitaApp) {
            app = deployWizardService.obtenerAplicacion(appId);
        }

        // ✅ PRECARGA PASO 1 (SONAR)
        if (paso == PasoDespliegue.SONAR_ANALISIS) {
            String sonarHost =
                    (app.getSonarHostUrl() != null && !app.getSonarHostUrl().isBlank())
                            ? app.getSonarHostUrl().trim()
                            : "https://sonarcloud.io";

            model.addAttribute("sonarHostUrl", sonarHost);
            model.addAttribute("sonarOrganization", app.getSonarOrganization());
            model.addAttribute("projectKey", app.getSonarProjectKey());
            model.addAttribute("sonarToken", app.getSonarToken());
        }

        // ✅ PRECARGA PASO 2 (projectKey)
        if (paso == PasoDespliegue.SONAR_INTEGRACION_GIT) {
            String projectKey = deployWizardService.obtenerProjectKey(appId);
            model.addAttribute("projectKey", projectKey);
        }

        // ✅ PRECARGA PASO 3 (repo)
        if (paso == PasoDespliegue.REPOSITORIO_GIT) {
            model.addAttribute("repositorioGit", app.getRepositorioGit());

            String prov = (app.getProveedorCiCd() != null)
                    ? app.getProveedorCiCd().name().toLowerCase()
                    : "github";
            model.addAttribute("proveedor", prov);
        }

        // ✅ PRECARGA PASO 4 (AWS/ECR) + TAG default latest
        if (paso == PasoDespliegue.IMAGEN_ECR) {

            // ✅ ECR repo: intenta primero ecrRepository; si está vacío, usa nombreImagenEcr (fallback)
            String repo = (app.getEcrRepository() != null) ? app.getEcrRepository().trim() : "";
            if (repo.isBlank()) {
                // si tu entidad tiene este getter, lo usamos como plan B
                // (según tu DB/logs existe nombre_imagen_ecr)
                repo = (app.getNombreImagenEcr() != null) ? app.getNombreImagenEcr().trim() : "";
            }
            model.addAttribute("ecrRepository", repo);

            model.addAttribute("awsRegion", app.getAwsRegion());
            model.addAttribute("awsAccessKeyId", app.getAwsAccessKeyId());
            model.addAttribute("awsSecretAccessKey", app.getAwsSecretAccessKey());
            model.addAttribute("awsAccountId", app.getAwsAccountId());

            // IMAGE_TAG: si está vacío -> latest
            String tag = (app.getImageTag() != null && !app.getImageTag().isBlank())
                    ? app.getImageTag().trim()
                    : "latest";
            model.addAttribute("imageTag", tag);
        }

        // ✅ PRECARGA PASO 5 (BBDD)
        if (paso == PasoDespliegue.BASE_DATOS) {

            // mode: local / remote
            String mode = (app.getDbModo() != null)
                    ? app.getDbModo().name().toLowerCase()
                    : "local";
            model.addAttribute("dbMode", mode);

            // engine: postgres / mysql / mongo
            String engine = "postgres";
            if (app.getTipoBaseDatos() != null) {
                engine = switch (app.getTipoBaseDatos()) {
                    case POSTGRESQL -> "postgres";
                    case MYSQL -> "mysql";
                    case MONGODB -> "mongo";
                };
            }
            model.addAttribute("dbEngine", engine);

            model.addAttribute("dbName", app.getNombreBaseDatos());
            model.addAttribute("dbUser", app.getUsuarioBaseDatos());

            // contraseña: por seguridad NO precargar
            model.addAttribute("dbPassword", null);

            // endpoint / puerto: precargar desde entidad (si existen)
            model.addAttribute("dbEndpoint", app.getDbEndpoint());
            model.addAttribute("dbPort", app.getDbPort());
        }

        // ✅ PRECARGA PASO 6 (EC2)
        if (paso == PasoDespliegue.DESPLIEGUE_EC2) {

            // Para que funcionen: app.ec2Host, app.ec2User, app.ec2KnownHosts, etc.
            model.addAttribute("app", app);

            // Puerto que tú quieres (puerto_aplicacion)
            model.addAttribute("appPort", app.getPuertoAplicacion());
        }
    }

    private Aplicacion getAppOrThrow(Long appId) {
        return aplicacionRepository.findById(appId)
                .orElseThrow(() -> new IllegalArgumentException("Aplicación no encontrada: " + appId));
    }

    /* =========================================================
       ✅ PASO 0 - PRIMER COMMIT
       ========================================================= */

    @GetMapping("/paso0")
    public String paso0(@RequestParam Long appId, Model model) {
        cargarModeloPaso(model, appId, PasoDespliegue.PRIMER_COMMIT);
        return "wizard/paso0";
    }

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

    @PostMapping("/paso0/confirmar")
    public String confirmarPaso0(@RequestParam Long appId,
                                 @RequestParam(required = false) String commitHash) {

        String msg = "Repositorio inicializado y primer commit realizado.";
        if (commitHash != null && !commitHash.isBlank()) {
            msg += " Hash: " + commitHash.trim();
        }

        deployWizardService.marcarPaso(appId, PasoDespliegue.PRIMER_COMMIT, EstadoControl.OK, msg);
        return "redirect:/wizard/paso1?appId=" + appId;
    }

    /* =========================================================
       ✅ PASO 1 - SONAR (variables + confirmación)
       ========================================================= */

    @GetMapping("/paso1")
    public String paso1(@RequestParam Long appId, Model model) {
        String redir = bloquearSiPrevioNoOk(appId, PasoDespliegue.SONAR_ANALISIS);
        if (redir != null) return redir;

        Aplicacion app = getAppOrThrow(appId);

        // Control del paso (SIEMPRE por appId + paso)
        var controlPaso = controlDespliegueRepository
                .findByAplicacionIdAndPaso(appId, PasoDespliegue.SONAR_ANALISIS)
                .orElse(null);

        model.addAttribute("appId", appId);
        model.addAttribute("app", app); // <-- CLAVE: usar app en la vista
        model.addAttribute("controlPaso", controlPaso);

        // Si usas lógica de bloqueo en la vista:
        boolean pasoActualCompleto = controlPaso != null && controlPaso.getEstado() == EstadoControl.OK;
        model.addAttribute("pasoActualCompleto", pasoActualCompleto);

        return "wizard/paso1";
    }

    @PostMapping("/paso1/confirmar")
    public ResponseEntity<Void> confirmarPaso1(@RequestParam Long appId,
                                               @RequestParam String sonarHostUrl,
                                               @RequestParam String sonarOrganization,
                                               @RequestParam String projectKey,
                                               @RequestParam String sonarToken) {

        Aplicacion app = getAppOrThrow(appId);

        String host = sonarHostUrl == null ? "" : sonarHostUrl.trim();
        String org = sonarOrganization == null ? "" : sonarOrganization.trim();
        String pk = projectKey == null ? "" : projectKey.trim();
        String token = sonarToken == null ? "" : sonarToken.trim();

        app.setSonarHostUrl(host.isBlank() ? "https://sonarcloud.io" : host);
        app.setSonarOrganization(org);
        app.setSonarProjectKey(pk);
        app.setSonarToken(token);
        aplicacionRepository.save(app);

        deployWizardService.marcarPaso(
                appId,
                PasoDespliegue.SONAR_ANALISIS,
                EstadoControl.OK,
                "SonarCloud configurado. Project Key: " + pk
        );

        return ResponseEntity.noContent().build();
    }

    /* =========================================================
       ✅ PASO 2 - INTEGRACIÓN SONAR ↔ GIT
       ========================================================= */

    @GetMapping("/paso2")
    public String paso2(@RequestParam Long appId, Model model) {
        String redir = bloquearSiPrevioNoOk(appId, PasoDespliegue.SONAR_INTEGRACION_GIT);
        if (redir != null) return redir;

        cargarModeloPaso(model, appId, PasoDespliegue.SONAR_INTEGRACION_GIT);
        return "wizard/paso2";
    }

    @PostMapping("/paso2/confirmar")
    public String confirmarPaso2(@RequestParam Long appId,
                                 @RequestParam(required = false) String projectKey) {

        String pk = projectKey == null ? null : projectKey.trim();
        if (pk != null && !pk.isBlank()) {
            Aplicacion app = getAppOrThrow(appId);
            app.setSonarProjectKey(pk);
            aplicacionRepository.save(app);
        }

        deployWizardService.marcarPaso(
                appId,
                PasoDespliegue.SONAR_INTEGRACION_GIT,
                EstadoControl.OK,
                "Integración Sonar ↔ Git verificada."
        );

        return "redirect:/wizard/paso3?appId=" + appId;
    }

    /* =========================================================
       ✅ PASO 3 - REPOSITORIO GIT
       ========================================================= */

    @GetMapping("/paso3")
    public String paso3(@RequestParam Long appId, Model model) {
        String redir = bloquearSiPrevioNoOk(appId, PasoDespliegue.REPOSITORIO_GIT);
        if (redir != null) return redir;

        cargarModeloPaso(model, appId, PasoDespliegue.REPOSITORIO_GIT);

        // ✅ Autocompletar owner/repo desde la URL guardada al crear la app
        Aplicacion app = deployWizardService.getAppOrThrow(appId);

        String repoUrlOrSlug = app.getRepositorioGit(); // puede ser URL o slug
        String repoSugerido = RepoSlugUtil.toSlug(repoUrlOrSlug);

        // si no se reconoce, al menos dejamos lo que haya (si el usuario guardó ya "owner/repo")
        if (repoSugerido == null) repoSugerido = (repoUrlOrSlug == null ? "" : repoUrlOrSlug.trim());

        model.addAttribute("repoSugerido", repoSugerido);

        return "wizard/paso3";
    }

    @PostMapping("/paso3/confirmar")
    public String confirmarPaso3(@RequestParam Long appId,
                                 @RequestParam String proveedor,
                                 @RequestParam String repo) {

        String prov = (proveedor == null ? "" : proveedor.trim().toLowerCase());
        String repoInput = (repo == null ? "" : repo.trim());

        // ✅ Convertir URL -> owner/repo si hace falta
        String repoNormalizado = RepoSlugUtil.toSlug(repoInput);
        if (repoNormalizado == null) repoNormalizado = repoInput;

        if (prov.isBlank() || repoNormalizado.isBlank()) {
            deployWizardService.marcarPaso(
                    appId,
                    PasoDespliegue.REPOSITORIO_GIT,
                    EstadoControl.KO,
                    "Debes indicar proveedor y repositorio."
            );
            return "redirect:/wizard/paso3?appId=" + appId;
        }

        // Guardamos ya normalizado (owner/repo)
        deployWizardService.guardarRepoGit(appId, prov, repoNormalizado);

        deployWizardService.marcarPaso(
                appId,
                PasoDespliegue.REPOSITORIO_GIT,
                EstadoControl.OK,
                "Repositorio verificado: " + prov.toUpperCase() + " → " + repoNormalizado
        );

        return "redirect:/wizard/paso4?appId=" + appId;
    }

    /* =========================================================
       ✅ PASO 4 - IMAGEN ECR
       ========================================================= */

    @GetMapping("/paso4")
    public String paso4(@RequestParam Long appId, Model model) {
        String redir = bloquearSiPrevioNoOk(appId, PasoDespliegue.IMAGEN_ECR);
        if (redir != null) return redir;

        cargarModeloPaso(model, appId, PasoDespliegue.IMAGEN_ECR);
        return "wizard/paso4";
    }

    @PostMapping("/paso4/confirmar")
    public ResponseEntity<Void> confirmarPaso4(@RequestParam Long appId,
                                               @RequestParam String ecrRepository,
                                               @RequestParam String awsRegion,
                                               @RequestParam String awsAccessKeyId,
                                               @RequestParam String awsSecretAccessKey,
                                               @RequestParam String awsAccountId) {

        Aplicacion app = getAppOrThrow(appId);

        String repo = ecrRepository == null ? "" : ecrRepository.trim();
        String region = awsRegion == null ? "" : awsRegion.trim();
        String access = awsAccessKeyId == null ? "" : awsAccessKeyId.trim();
        String secret = awsSecretAccessKey == null ? "" : awsSecretAccessKey.trim();
        String account = awsAccountId == null ? "" : awsAccountId.trim();

        app.setEcrRepository(repo);
        app.setAwsRegion(region);
        app.setAwsAccessKeyId(access);
        app.setAwsSecretAccessKey(secret);
        app.setAwsAccountId(account);

        aplicacionRepository.save(app);

        deployWizardService.marcarPaso(
                appId,
                PasoDespliegue.IMAGEN_ECR,
                EstadoControl.OK,
                "Variables AWS/ECR guardadas. Repositorio: " + repo + " (" + region + ")"
        );

        return ResponseEntity.noContent().build();
    }

    /* =========================================================
       ✅ PASO 5 - BASE DATOS (PANTALLA)
       ========================================================= */

    @GetMapping("/paso5")
    public String paso5(@RequestParam Long appId, Model model) {
        String redir = bloquearSiPrevioNoOk(appId, PasoDespliegue.BASE_DATOS);
        if (redir != null) return redir;

        cargarModeloPaso(model, appId, PasoDespliegue.BASE_DATOS);
        return "wizard/paso5";
    }

    @PostMapping("/paso5/confirmar")
    public ResponseEntity<Void> confirmarPaso5(@RequestParam Long appId,
                                               @RequestParam DbModo modo,
                                               @RequestParam TipoBaseDatos tipo,
                                               @RequestParam String dbName,
                                               @RequestParam(required = false) String dbUser,
                                               @RequestParam(required = false) String dbPassword,
                                               @RequestParam(required = false) Integer dbPort,
                                               @RequestParam(required = false) String dbEndpoint) {

        // Limpieza básica
        String name = dbName == null ? "" : dbName.trim();
        String user = dbUser == null ? "" : dbUser.trim();
        String pass = dbPassword == null ? "" : dbPassword.trim();
        String endpoint = dbEndpoint == null ? "" : dbEndpoint.trim();

        // ✅ Reglas mínimas: lo importante para Mongo REMOTE es endpoint (DB_URI)
        if (name.isBlank() || modo == null || tipo == null) {
            deployWizardService.marcarPaso(appId, PasoDespliegue.BASE_DATOS, EstadoControl.KO,
                    "Completa DB_MODE, DB_ENGINE y DB_NAME.");
            return ResponseEntity.badRequest().build();
        }

        // Si es REMOTE + MONGODB -> dbEndpoint debe ser la URI completa
        if (modo == DbModo.REMOTE && tipo == TipoBaseDatos.MONGODB) {
            if (endpoint.isBlank()) {
                deployWizardService.marcarPaso(appId, PasoDespliegue.BASE_DATOS, EstadoControl.KO,
                        "Para Mongo REMOTE debes indicar DB_URI (se guarda en DB_ENDPOINT).");
                return ResponseEntity.badRequest().build();
            }
        }

        // ✅ Aquí es donde se hace la llamada que tú preguntas:
        deployWizardService.guardarPaso5Bd(
                appId,
                modo,
                tipo,
                name,
                user.isBlank() ? null : user,
                pass.isBlank() ? null : pass,
                dbPort,
                endpoint.isBlank() ? null : endpoint
        );

        deployWizardService.marcarPaso(appId, PasoDespliegue.BASE_DATOS, EstadoControl.OK,
                "Configuración de base de datos guardada.");

        return ResponseEntity.noContent().build();
    }


    /**
     * Descarga el PDF de BD y marca automáticamente el paso 5 (BASE_DATOS) como OK.
     * (Mapping conservado: /paso6/pdf, como venías usando en tu proyecto).
     */
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

    /* =========================================================
       ✅ PASO 6 - DESPLIEGUE EC2
       ========================================================= */

    @GetMapping("/paso6")
    public String paso6(@RequestParam Long appId, Model model) {

        String redir = bloquearSiPrevioNoOk(appId, PasoDespliegue.DESPLIEGUE_EC2);
        if (redir != null) return redir;

        cargarModeloPaso(model, appId, PasoDespliegue.DESPLIEGUE_EC2);

        Aplicacion app = deployWizardService.getAppOrThrow(appId);
        model.addAttribute("app", app);
        model.addAttribute("appPort", app.getPuertoAplicacion());

        boolean hayLlaveGuardada = app.getEc2LlaveSsh() != null && !app.getEc2LlaveSsh().isBlank();
        model.addAttribute("hayLlaveGuardada", hayLlaveGuardada);

        return "wizard/paso6";
    }

    @PostMapping("/paso6/confirmar")
    public ResponseEntity<Void> confirmarPaso6(@RequestParam Long appId,
                                               @RequestParam String ec2Host,
                                               @RequestParam String ec2User,
                                               @RequestParam String ec2KnownHosts,
                                               @RequestParam Integer appPort,
                                               @RequestParam(required = false) String ec2LlaveSsh) {

        String host = ec2Host == null ? "" : ec2Host.trim();
        String user = ec2User == null ? "" : ec2User.trim();
        String known = ec2KnownHosts == null ? "" : ec2KnownHosts.trim();
        Integer port = appPort;
        String key = ec2LlaveSsh == null ? "" : ec2LlaveSsh.trim();

        if (host.isBlank() || user.isBlank() || known.isBlank() || port == null) {
            deployWizardService.marcarPaso(
                    appId,
                    PasoDespliegue.DESPLIEGUE_EC2,
                    EstadoControl.KO,
                    "Debes completar EC2_HOST, EC2_USER, EC2_KNOWN_HOSTS y APP_PORT."
            );
            return ResponseEntity.badRequest().build();
        }

        deployWizardService.guardarVarsEc2(
                appId,
                host,
                user,
                known,
                port,
                key
        );

        deployWizardService.marcarPaso(
                appId,
                PasoDespliegue.DESPLIEGUE_EC2,
                EstadoControl.OK,
                "Variables EC2 guardadas. Listo para desplegar."
        );

        return ResponseEntity.noContent().build();
    }

    /* =========================================================
       ✅ RESUMEN + DESCARGA VARIABLES
       ========================================================= */

    @GetMapping("/resumen")
    public String resumen(@RequestParam Long appId, Model model) {

        String redir = bloquearSiPrevioNoOk(appId, PasoDespliegue.DESPLIEGUE_EC2);
        if (redir != null) return redir;

        // ✅ IMPORTANTE: Cargar la app para que el HTML pueda pintar variables
        Aplicacion app = deployWizardService.getAppOrThrow(appId);

        List<ControlDespliegue> controles = deployWizardService.obtenerControlesOrdenados(appId);

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
        model.addAttribute("app", app); // ✅ clave
        return "wizard/resumen";
    }

    @GetMapping(value = "/resumen/variables-txt", produces = "text/plain; charset=UTF-8")
    public ResponseEntity<String> descargarVariablesTxt(@RequestParam Long appId) {

        Aplicacion app = deployWizardService.getAppOrThrow(appId);

        StringBuilder sb = new StringBuilder();

        sb.append("========================================\n");
        sb.append("RESUMEN VARIABLES CI/CD (por pasos)\n");
        sb.append("========================================\n");
        sb.append("Fecha: ").append(java.time.LocalDateTime.now()).append("\n");
        sb.append("appId: ").append(appId).append("\n\n");

    /* =========================
       PASO 1 - SONAR
       ========================= */
        sb.append("========== PASO 1 - SONAR ==========\n");
        appendKv(sb, "SONAR_HOST_URL", app.getSonarHostUrl(), false);
        appendKv(sb, "SONAR_ORGANIZATION", app.getSonarOrganization(), false);
        appendKv(sb, "SONAR_PROJECT_KEY", app.getSonarProjectKey(), false);
        appendKv(sb, "SONAR_TOKEN", app.getSonarToken(), true);
        sb.append("\n");

    /* =========================
       PASO 4 - AWS / ECR
       ========================= */
        sb.append("========== PASO 4 - AWS / ECR ==========\n");
        appendKv(sb, "ECR_REPOSITORY", app.getEcrRepository(), false);
        appendKv(sb, "AWS_REGION", app.getAwsRegion(), false);
        appendKv(sb, "AWS_ACCOUNT_ID", app.getAwsAccountId(), false);
        appendKv(sb, "AWS_ACCESS_KEY_ID", app.getAwsAccessKeyId(), true);
        appendKv(sb, "AWS_SECRET_ACCESS_KEY", app.getAwsSecretAccessKey(), true);
        sb.append("\n");

    /* =========================
       PASO 5 - BASE DE DATOS
       ========================= */
        sb.append("========== PASO 5 - BASE DE DATOS ==========\n");

        // DB_MODE
        String dbMode = (app.getDbModo() != null) ? app.getDbModo().name().toLowerCase() : null;
        appendKv(sb, "DB_MODE", dbMode, false);

        // DB_ENGINE
        String engine = null;
        if (app.getTipoBaseDatos() != null) {
            engine = switch (app.getTipoBaseDatos()) {
                case POSTGRESQL -> "postgres";
                case MYSQL -> "mysql";
                case MONGODB -> "mongo";
            };
        }
        appendKv(sb, "DB_ENGINE", engine, false);

        // DB_NAME siempre
        appendKv(sb, "DB_NAME", app.getNombreBaseDatos(), false);

        boolean modoRemote = (app.getDbModo() == DbModo.REMOTE);

        // Puerto por defecto (si no lo guardaste)
        Integer port = app.getDbPort();
        if (port == null) port = defaultPort(engine);

        // ====== CASOS SEGÚN MOTOR/MODO ======
        // Queremos que SIEMPRE se vean todas y las no aplicables digan NO_CREAR

        if ("mongo".equals(engine)) {
            // Mongo local / remoto
            appendKv(sb, "DB_PORT", port != null ? String.valueOf(port) : null, false);

            if (modoRemote) {
                // ✅ Mongo REMOTE usa DB_URI (sale de dbEndpoint)
                appendKv(sb, "DB_HOST", "NO_CREAR", false);
                appendKv(sb, "DB_SSLMODE", "NO_CREAR", false);

                String uri = app.getDbEndpoint(); // aquí guardas la URI completa
                appendKv(sb, "DB_URI", (uri != null && !uri.isBlank()) ? uri : "NO_CREAR", false);

            } else {
                // Mongo LOCAL
                appendKv(sb, "DB_HOST", "mongo", false);
                appendKv(sb, "DB_SSLMODE", "NO_CREAR", false);
                appendKv(sb, "DB_URI", "NO_CREAR", false);
            }

            // En tu guía estás usando DB_USER/DB_PASSWORD también para mongo
            appendKv(sb, "DB_USER", app.getUsuarioBaseDatos(), true);
            appendKv(sb, "DB_PASSWORD", app.getPasswordBaseDatos(), true);

        } else if ("postgres".equals(engine)) {
            // Postgres local / remoto
            appendKv(sb, "DB_PORT", port != null ? String.valueOf(port) : null, false);

            if (modoRemote) {
                // REMOTE: DB_HOST = endpoint guardado en dbEndpoint
                appendKv(sb, "DB_HOST", valueOrNoCrear(app.getDbEndpoint()), false);
                appendKv(sb, "DB_SSLMODE", "require", false);
                appendKv(sb, "DB_URI", "NO_CREAR", false);
            } else {
                // LOCAL
                appendKv(sb, "DB_HOST", "postgres", false);
                appendKv(sb, "DB_SSLMODE", "disable", false);
                appendKv(sb, "DB_URI", "NO_CREAR", false);
            }

            appendKv(sb, "DB_USER", app.getUsuarioBaseDatos(), true);
            appendKv(sb, "DB_PASSWORD", app.getPasswordBaseDatos(), true);

        } else if ("mysql".equals(engine)) {
            // MySQL local / remoto
            appendKv(sb, "DB_PORT", port != null ? String.valueOf(port) : null, false);

            if (modoRemote) {
                appendKv(sb, "DB_HOST", valueOrNoCrear(app.getDbEndpoint()), false);
            } else {
                appendKv(sb, "DB_HOST", "mysql", false);
            }

            appendKv(sb, "DB_SSLMODE", "NO_CREAR", false);
            appendKv(sb, "DB_URI", "NO_CREAR", false);

            appendKv(sb, "DB_USER", app.getUsuarioBaseDatos(), true);
            appendKv(sb, "DB_PASSWORD", app.getPasswordBaseDatos(), true);

        } else {
            // Si no hay motor seleccionado
            appendKv(sb, "DB_PORT", "NO_CREAR", false);
            appendKv(sb, "DB_HOST", "NO_CREAR", false);
            appendKv(sb, "DB_SSLMODE", "NO_CREAR", false);
            appendKv(sb, "DB_URI", "NO_CREAR", false);
            appendKv(sb, "DB_USER", "NO_CREAR", true);
            appendKv(sb, "DB_PASSWORD", "NO_CREAR", true);
        }

        sb.append("\n");

    /* =========================
       PASO 6 - EC2
       ========================= */
        sb.append("========== PASO 6 - EC2 ==========\n");
        appendKv(sb, "EC2_HOST", app.getEc2Host(), false);
        appendKv(sb, "EC2_USER", app.getEc2User(), false);
        appendKv(sb, "APP_PORT", app.getAppPort() != null ? String.valueOf(app.getAppPort()) : null, false);
        appendKv(sb, "EC2_KNOWN_HOSTS", app.getEc2KnownHosts(), false);
        appendKv(sb, "EC2_LLAVE_SSH", app.getEc2LlaveSsh(), true);

        sb.append("\n========================================\n");
        sb.append("NOTA:\n");
        sb.append("- Los valores marcados como SECRET deben crearse como secretos en tu CI/CD.\n");
        sb.append("- Si pone NO_CREAR, esa variable NO aplica en ese modo/motor.\n");
        sb.append("========================================\n");

        HttpHeaders headers = new HttpHeaders();
        headers.setContentDisposition(
                ContentDisposition.attachment()
                        .filename("variables-por-pasos.txt")
                        .build()
        );

        return ResponseEntity.ok()
                .headers(headers)
                .body(sb.toString());
    }

    /** Imprime KEY=VALUE y marca # SECRET si aplica */
    private static void appendKv(StringBuilder sb, String key, String value, boolean secret) {
        if (value == null) return;
        String v = value.trim();
        if (v.isBlank()) return;

        sb.append(key).append("=").append(v);
        if (secret) sb.append("   # SECRET");
        sb.append("\n");
    }

    private static String valueOrNoCrear(String v) {
        if (v == null) return "NO_CREAR";
        String t = v.trim();
        return t.isBlank() ? "NO_CREAR" : t;
    }

    private static Integer defaultPort(String engine) {
        if (engine == null) return null;
        return switch (engine) {
            case "postgres" -> 5432;
            case "mysql" -> 3306;
            case "mongo" -> 27017;
            default -> null;
        };
    }


    /**
     * Nombre típico del servicio en docker-compose para modo LOCAL.
     * ⚠️ Ajusta si tus servicios se llaman diferente.
     */
   /* private static String defaultLocalServiceName(String engine) {
        if (engine == null) return null;
        return switch (engine) {
            case "postgres" -> "postgres";
            case "mysql" -> "mysql";
            case "mongo" -> "mongo";
            default -> null;
        };
    }*/

    /* =========================================================
       DTO PARA EL RESUMEN
       ========================================================= */

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
            return new PasoView(numero, titulo, c.getEstado().name(), c.getMensaje());
        }

        public int getNumero() { return numero; }
        public String getTitulo() { return titulo; }
        public String getEstado() { return estado; }
        public String getMensaje() { return mensaje; }
    }
}