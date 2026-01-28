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

/**
 * En este controlador gestiono el asistente de despliegue (wizard) y su navegación por pasos.
 *
 * Me encargo de:
 * - renderizar las pantallas de cada paso,
 * - validar y confirmar acciones del usuario,
 * - persistir la configuración asociada a cada paso,
 * - y registrar el estado de ejecución mediante {@link ControlDespliegue}.
 *
 * Aplico control de flujo bloqueando el acceso a un paso si el paso previo no está en estado {@link EstadoControl#OK}.
 *
 * @author David Tomé Arnaiz
 */
@Controller
@RequestMapping("/wizard")
public class DeployWizardController {

    private final DeployWizardService deployWizardService;
    private final Paso6PdfService paso6PdfService;
    private final AplicacionRepository aplicacionRepository;
    private final ControlDespliegueRepository controlDespliegueRepository;

    /**
     * En este constructor inyecto los servicios y repositorios necesarios para gestionar el asistente.
     *
     * @param deployWizardService servicio con la lógica principal del asistente
     * @param paso6PdfService servicio encargado de generar la guía PDF de base de datos
     * @param aplicacionRepository repositorio de acceso a {@link Aplicacion}
     * @param controlDespliegueRepository repositorio de acceso a {@link ControlDespliegue}
     * @author David Tomé Arnaiz
     */
    public DeployWizardController(DeployWizardService deployWizardService,
                                  Paso6PdfService paso6PdfService,
                                  AplicacionRepository aplicacionRepository,
                                  ControlDespliegueRepository controlDespliegueRepository) {

        this.deployWizardService = deployWizardService;
        this.paso6PdfService = paso6PdfService;
        this.aplicacionRepository = aplicacionRepository;
        this.controlDespliegueRepository = controlDespliegueRepository;
    }

    /**
     * En este método traduzco un {@link PasoDespliegue} al identificador de ruta utilizado en el asistente.
     *
     * @param paso paso lógico del asistente
     * @return nombre del segmento de ruta asociado (por ejemplo, "paso3")
     */
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
     * En este método impido el acceso al paso actual si el paso previo no está completado correctamente.
     *
     * @param appId identificador de la aplicación
     * @param pasoActual paso al que se intenta acceder
     * @return redirección al paso previo si no está en OK; en caso contrario, null
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
     * En este método cargo en el modelo los atributos comunes de un paso y precargo los valores persistidos.
     *
     * @param model modelo de Spring MVC
     * @param appId identificador de la aplicación
     * @param paso paso actual del asistente
     */
    private void cargarModeloPaso(Model model, Long appId, PasoDespliegue paso) {
        model.addAttribute("appId", appId);

        Optional<ControlDespliegue> controlOpt = deployWizardService.obtenerControl(appId, paso);
        controlOpt.ifPresent(c -> model.addAttribute("controlPaso", c));

        boolean pasoActualCompleto =
                controlOpt.map(c -> c.getEstado() == EstadoControl.OK).orElse(false);
        model.addAttribute("pasoActualCompleto", pasoActualCompleto);

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

        if (paso == PasoDespliegue.SONAR_INTEGRACION_GIT) {
            String projectKey = deployWizardService.obtenerProjectKey(appId);
            model.addAttribute("projectKey", projectKey);
        }

        if (paso == PasoDespliegue.REPOSITORIO_GIT) {
            model.addAttribute("repositorioGit", app.getRepositorioGit());

            String prov = (app.getProveedorCiCd() != null)
                    ? app.getProveedorCiCd().name().toLowerCase()
                    : "github";
            model.addAttribute("proveedor", prov);
        }

        if (paso == PasoDespliegue.IMAGEN_ECR) {
            String repo = (app.getEcrRepository() != null) ? app.getEcrRepository().trim() : "";
            if (repo.isBlank()) {
                repo = (app.getNombreImagenEcr() != null) ? app.getNombreImagenEcr().trim() : "";
            }
            model.addAttribute("ecrRepository", repo);

            model.addAttribute("awsRegion", app.getAwsRegion());
            model.addAttribute("awsAccessKeyId", app.getAwsAccessKeyId());
            model.addAttribute("awsSecretAccessKey", app.getAwsSecretAccessKey());
            model.addAttribute("awsAccountId", app.getAwsAccountId());

            String tag = (app.getImageTag() != null && !app.getImageTag().isBlank())
                    ? app.getImageTag().trim()
                    : "latest";
            model.addAttribute("imageTag", tag);
        }

        if (paso == PasoDespliegue.BASE_DATOS) {

            String mode = (app.getDbModo() != null)
                    ? app.getDbModo().name().toLowerCase()
                    : "local";
            model.addAttribute("dbMode", mode);

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

            model.addAttribute("dbPassword", null);

            model.addAttribute("dbEndpoint", app.getDbEndpoint());
            model.addAttribute("dbPort", app.getDbPort());
        }

        if (paso == PasoDespliegue.DESPLIEGUE_EC2) {
            model.addAttribute("app", app);
            model.addAttribute("appPort", app.getPuertoAplicacion());
        }
    }

    /**
     * En este método recupero una aplicación por id y lanzo una excepción si no existe.
     *
     * @param appId identificador de la aplicación
     * @return aplicación encontrada
     */
    private Aplicacion getAppOrThrow(Long appId) {
        return aplicacionRepository.findById(appId)
                .orElseThrow(() -> new IllegalArgumentException("Aplicación no encontrada: " + appId));
    }

    /**
     * En este endpoint muestro el paso inicial del asistente (primer commit).
     *
     * @param appId identificador de la aplicación
     * @param model modelo de Spring MVC
     * @return vista del paso 0
     */
    @GetMapping("/paso0")
    public String paso0(@RequestParam Long appId, Model model) {
        cargarModeloPaso(model, appId, PasoDespliegue.PRIMER_COMMIT);
        return "wizard/paso0";
    }

    /**
     * En este endpoint valido el formato del hash de commit.
     *
     * @param commitHash hash introducido por el usuario
     * @return mapa con el estado y un mensaje informativo
     */
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

    /**
     * En este endpoint confirmo el paso 0 y registro su finalización.
     *
     * @param appId identificador de la aplicación
     * @param commitHash hash del commit (opcional)
     * @return redirección al paso 1
     */
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

    /**
     * En este endpoint muestro el paso de configuración de Sonar.
     *
     * @param appId identificador de la aplicación
     * @param model modelo de Spring MVC
     * @return vista del paso 1
     */
    @GetMapping("/paso1")
    public String paso1(@RequestParam Long appId, Model model) {
        String redir = bloquearSiPrevioNoOk(appId, PasoDespliegue.SONAR_ANALISIS);
        if (redir != null) return redir;

        Aplicacion app = getAppOrThrow(appId);

        var controlPaso = controlDespliegueRepository
                .findByAplicacionIdAndPaso(appId, PasoDespliegue.SONAR_ANALISIS)
                .orElse(null);

        model.addAttribute("appId", appId);
        model.addAttribute("app", app);
        model.addAttribute("controlPaso", controlPaso);

        boolean pasoActualCompleto = controlPaso != null && controlPaso.getEstado() == EstadoControl.OK;
        model.addAttribute("pasoActualCompleto", pasoActualCompleto);

        return "wizard/paso1";
    }

    /**
     * En este endpoint guardo la configuración de Sonar y marco el paso como completado.
     *
     * @param appId identificador de la aplicación
     * @param sonarHostUrl URL del servidor de Sonar (o SonarCloud)
     * @param sonarOrganization organización/proyecto en Sonar
     * @param projectKey clave de proyecto
     * @param sonarToken token de acceso
     * @return respuesta vacía en caso de éxito
     */
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
                "Sonar configurado. Project Key: " + pk
        );

        return ResponseEntity.noContent().build();
    }

    /**
     * En este endpoint muestro el paso de integración Sonar ↔ Git.
     *
     * @param appId identificador de la aplicación
     * @param model modelo de Spring MVC
     * @return vista del paso 2
     */
    @GetMapping("/paso2")
    public String paso2(@RequestParam Long appId, Model model) {
        String redir = bloquearSiPrevioNoOk(appId, PasoDespliegue.SONAR_INTEGRACION_GIT);
        if (redir != null) return redir;

        cargarModeloPaso(model, appId, PasoDespliegue.SONAR_INTEGRACION_GIT);
        return "wizard/paso2";
    }

    /**
     * En este endpoint confirmo el paso de integración Sonar ↔ Git y registro su estado.
     *
     * @param appId identificador de la aplicación
     * @param projectKey clave de proyecto (opcional)
     * @return redirección al paso 3
     */
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

    /**
     * En este endpoint muestro el paso de configuración del repositorio Git.
     *
     * @param appId identificador de la aplicación
     * @param model modelo de Spring MVC
     * @return vista del paso 3
     */
    @GetMapping("/paso3")
    public String paso3(@RequestParam Long appId, Model model) {
        String redir = bloquearSiPrevioNoOk(appId, PasoDespliegue.REPOSITORIO_GIT);
        if (redir != null) return redir;

        cargarModeloPaso(model, appId, PasoDespliegue.REPOSITORIO_GIT);

        Aplicacion app = deployWizardService.getAppOrThrow(appId);

        String repoUrlOrSlug = app.getRepositorioGit();
        String repoSugerido = RepoSlugUtil.toSlug(repoUrlOrSlug);
        if (repoSugerido == null) repoSugerido = (repoUrlOrSlug == null ? "" : repoUrlOrSlug.trim());

        model.addAttribute("repoSugerido", repoSugerido);

        return "wizard/paso3";
    }

    /**
     * En este endpoint guardo el proveedor CI/CD y el repositorio normalizado (owner/repo).
     *
     * @param appId identificador de la aplicación
     * @param proveedor proveedor de CI/CD (por ejemplo, github o gitlab)
     * @param repo repositorio en formato URL o slug
     * @return redirección al paso 4
     */
    @PostMapping("/paso3/confirmar")
    public String confirmarPaso3(@RequestParam Long appId,
                                 @RequestParam String proveedor,
                                 @RequestParam String repo) {

        String prov = (proveedor == null ? "" : proveedor.trim().toLowerCase());
        String repoInput = (repo == null ? "" : repo.trim());

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

        deployWizardService.guardarRepoGit(appId, prov, repoNormalizado);

        deployWizardService.marcarPaso(
                appId,
                PasoDespliegue.REPOSITORIO_GIT,
                EstadoControl.OK,
                "Repositorio verificado: " + prov.toUpperCase() + " → " + repoNormalizado
        );

        return "redirect:/wizard/paso4?appId=" + appId;
    }

    /**
     * En este endpoint muestro el paso de configuración de ECR y credenciales AWS.
     *
     * @param appId identificador de la aplicación
     * @param model modelo de Spring MVC
     * @return vista del paso 4
     */
    @GetMapping("/paso4")
    public String paso4(@RequestParam Long appId, Model model) {
        String redir = bloquearSiPrevioNoOk(appId, PasoDespliegue.IMAGEN_ECR);
        if (redir != null) return redir;

        cargarModeloPaso(model, appId, PasoDespliegue.IMAGEN_ECR);
        return "wizard/paso4";
    }

    /**
     * En este endpoint guardo las variables AWS/ECR y marco el paso como completado.
     *
     * @return respuesta vacía en caso de éxito
     */
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

    /**
     * En este endpoint muestro el paso de configuración de base de datos.
     *
     * @param appId identificador de la aplicación
     * @param model modelo de Spring MVC
     * @return vista del paso 5
     */
    @GetMapping("/paso5")
    public String paso5(@RequestParam Long appId, Model model) {
        String redir = bloquearSiPrevioNoOk(appId, PasoDespliegue.BASE_DATOS);
        if (redir != null) return redir;

        cargarModeloPaso(model, appId, PasoDespliegue.BASE_DATOS);
        return "wizard/paso5";
    }

    /**
     * En este endpoint guardo la configuración de base de datos y marco el paso como completado.
     *
     * @return respuesta vacía en caso de éxito; en caso de validación fallida, 400
     */
    @PostMapping("/paso5/confirmar")
    public ResponseEntity<Void> confirmarPaso5(@RequestParam Long appId,
                                               @RequestParam DbModo modo,
                                               @RequestParam TipoBaseDatos tipo,
                                               @RequestParam String dbName,
                                               @RequestParam(required = false) String dbUser,
                                               @RequestParam(required = false) String dbPassword,
                                               @RequestParam(required = false) Integer dbPort,
                                               @RequestParam(required = false) String dbEndpoint) {

        String name = dbName == null ? "" : dbName.trim();
        String user = dbUser == null ? "" : dbUser.trim();
        String pass = dbPassword == null ? "" : dbPassword.trim();
        String endpoint = dbEndpoint == null ? "" : dbEndpoint.trim();

        if (name.isBlank() || modo == null || tipo == null) {
            deployWizardService.marcarPaso(appId, PasoDespliegue.BASE_DATOS, EstadoControl.KO,
                    "Completa DB_MODE, DB_ENGINE y DB_NAME.");
            return ResponseEntity.badRequest().build();
        }

        if (modo == DbModo.REMOTE && tipo == TipoBaseDatos.MONGODB) {
            if (endpoint.isBlank()) {
                deployWizardService.marcarPaso(appId, PasoDespliegue.BASE_DATOS, EstadoControl.KO,
                        "Para Mongo REMOTE debes indicar DB_URI (se guarda en DB_ENDPOINT).");
                return ResponseEntity.badRequest().build();
            }
        }

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
     * En este endpoint genero y devuelvo la guía PDF de base de datos.
     *
     * Además, registro el paso de base de datos como completado cuando el usuario descarga el PDF.
     *
     * @param appId identificador de la aplicación
     * @param mode modo de base de datos (local/remote)
     * @param engine motor de base de datos (postgres/mysql/mongo)
     * @param port puerto (opcional)
     * @return PDF en formato binario como adjunto
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

    /**
     * En este endpoint muestro el paso de configuración de despliegue en EC2.
     *
     * @param appId identificador de la aplicación
     * @param model modelo de Spring MVC
     * @return vista del paso 6
     */
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

    /**
     * En este endpoint guardo las variables necesarias para el despliegue en EC2 y marco el paso como completado.
     *
     * @return respuesta vacía en caso de éxito; en caso de validación fallida, 400
     */
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

    /**
     * En este endpoint muestro el resumen del asistente con el estado de cada paso.
     *
     * @param appId identificador de la aplicación
     * @param model modelo de Spring MVC
     * @return vista de resumen
     */
    @GetMapping("/resumen")
    public String resumen(@RequestParam Long appId, Model model) {

        String redir = bloquearSiPrevioNoOk(appId, PasoDespliegue.DESPLIEGUE_EC2);
        if (redir != null) return redir;

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
        model.addAttribute("app", app);
        return "wizard/resumen";
    }

    /**
     * En este endpoint genero un fichero de texto con las variables agrupadas por pasos del asistente.
     *
     * @param appId identificador de la aplicación
     * @return contenido del fichero como adjunto text/plain
     */
    @GetMapping(value = "/resumen/variables-txt", produces = "text/plain; charset=UTF-8")
    public ResponseEntity<String> descargarVariablesTxt(@RequestParam Long appId) {

        Aplicacion app = deployWizardService.getAppOrThrow(appId);

        StringBuilder sb = new StringBuilder();

        sb.append("========================================\n");
        sb.append("RESUMEN VARIABLES CI/CD (por pasos)\n");
        sb.append("========================================\n");
        sb.append("Fecha: ").append(java.time.LocalDateTime.now()).append("\n");
        sb.append("appId: ").append(appId).append("\n\n");

        sb.append("========== PASO 1 - SONAR ==========\n");
        appendKv(sb, "SONAR_HOST_URL", app.getSonarHostUrl(), false);
        appendKv(sb, "SONAR_ORGANIZATION", app.getSonarOrganization(), false);
        appendKv(sb, "SONAR_PROJECT_KEY", app.getSonarProjectKey(), false);
        appendKv(sb, "SONAR_TOKEN", app.getSonarToken(), true);
        sb.append("\n");

        sb.append("========== PASO 4 - AWS / ECR ==========\n");
        appendKv(sb, "ECR_REPOSITORY", app.getEcrRepository(), false);
        appendKv(sb, "AWS_REGION", app.getAwsRegion(), false);
        appendKv(sb, "AWS_ACCOUNT_ID", app.getAwsAccountId(), false);
        appendKv(sb, "AWS_ACCESS_KEY_ID", app.getAwsAccessKeyId(), true);
        appendKv(sb, "AWS_SECRET_ACCESS_KEY", app.getAwsSecretAccessKey(), true);
        sb.append("\n");

        sb.append("========== PASO 5 - BASE DE DATOS ==========\n");

        String dbMode = (app.getDbModo() != null) ? app.getDbModo().name().toLowerCase() : null;
        appendKv(sb, "DB_MODE", dbMode, false);

        String engine = null;
        if (app.getTipoBaseDatos() != null) {
            engine = switch (app.getTipoBaseDatos()) {
                case POSTGRESQL -> "postgres";
                case MYSQL -> "mysql";
                case MONGODB -> "mongo";
            };
        }
        appendKv(sb, "DB_ENGINE", engine, false);

        appendKv(sb, "DB_NAME", app.getNombreBaseDatos(), false);

        boolean modoRemote = (app.getDbModo() == DbModo.REMOTE);

        Integer port = app.getDbPort();
        if (port == null) port = defaultPort(engine);

        if ("mongo".equals(engine)) {
            appendKv(sb, "DB_PORT", port != null ? String.valueOf(port) : null, false);

            if (modoRemote) {
                appendKv(sb, "DB_HOST", "NO_CREAR", false);
                appendKv(sb, "DB_SSLMODE", "NO_CREAR", false);

                String uri = app.getDbEndpoint();
                appendKv(sb, "DB_URI", (uri != null && !uri.isBlank()) ? uri : "NO_CREAR", false);

            } else {
                appendKv(sb, "DB_HOST", "mongo", false);
                appendKv(sb, "DB_SSLMODE", "NO_CREAR", false);
                appendKv(sb, "DB_URI", "NO_CREAR", false);
            }

            appendKv(sb, "DB_USER", app.getUsuarioBaseDatos(), true);
            appendKv(sb, "DB_PASSWORD", app.getPasswordBaseDatos(), true);

        } else if ("postgres".equals(engine)) {
            appendKv(sb, "DB_PORT", port != null ? String.valueOf(port) : null, false);

            if (modoRemote) {
                appendKv(sb, "DB_HOST", valueOrNoCrear(app.getDbEndpoint()), false);
                appendKv(sb, "DB_SSLMODE", "require", false);
                appendKv(sb, "DB_URI", "NO_CREAR", false);
            } else {
                appendKv(sb, "DB_HOST", "postgres", false);
                appendKv(sb, "DB_SSLMODE", "disable", false);
                appendKv(sb, "DB_URI", "NO_CREAR", false);
            }

            appendKv(sb, "DB_USER", app.getUsuarioBaseDatos(), true);
            appendKv(sb, "DB_PASSWORD", app.getPasswordBaseDatos(), true);

        } else if ("mysql".equals(engine)) {
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
            appendKv(sb, "DB_PORT", "NO_CREAR", false);
            appendKv(sb, "DB_HOST", "NO_CREAR", false);
            appendKv(sb, "DB_SSLMODE", "NO_CREAR", false);
            appendKv(sb, "DB_URI", "NO_CREAR", false);
            appendKv(sb, "DB_USER", "NO_CREAR", true);
            appendKv(sb, "DB_PASSWORD", "NO_CREAR", true);
        }

        sb.append("\n");

        sb.append("========== PASO 6 - EC2 ==========\n");
        appendKv(sb, "EC2_HOST", app.getEc2Host(), false);
        appendKv(sb, "EC2_USER", app.getEc2User(), false);
        appendKv(sb, "APP_PORT", app.getAppPort() != null ? String.valueOf(app.getAppPort()) : null, false);
        appendKv(sb, "EC2_KNOWN_HOSTS", app.getEc2KnownHosts(), false);
        appendKv(sb, "EC2_LLAVE_SSH", app.getEc2LlaveSsh(), true);

        sb.append("\n========================================\n");
        sb.append("NOTA:\n");
        sb.append("- Los valores marcados como SECRET deben crearse como secretos en tu CI/CD.\n");
        sb.append("- Si aparece NO_CREAR, esa variable no aplica en ese modo o motor.\n");
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

    /**
     * En este método añado una línea KEY=VALUE al fichero de variables.
     *
     * Si el valor es nulo o vacío no lo incluyo. Si la variable es sensible, la marco como SECRET en el comentario.
     *
     * @param sb acumulador del contenido
     * @param key nombre de la variable
     * @param value valor asociado
     * @param secret indica si la variable debe tratarse como secreta
     */
    private static void appendKv(StringBuilder sb, String key, String value, boolean secret) {
        if (value == null) return;
        String v = value.trim();
        if (v.isBlank()) return;

        sb.append(key).append("=").append(v);
        if (secret) sb.append("   # SECRET");
        sb.append("\n");
    }

    /**
     * En este método devuelvo el valor recibido o "NO_CREAR" si es nulo o vacío.
     *
     * @param v valor a evaluar
     * @return valor normalizado
     */
    private static String valueOrNoCrear(String v) {
        if (v == null) return "NO_CREAR";
        String t = v.trim();
        return t.isBlank() ? "NO_CREAR" : t;
    }

    /**
     * En este método devuelvo el puerto por defecto según el motor de base de datos.
     *
     * @param engine motor normalizado (postgres/mysql/mongo)
     * @return puerto por defecto o null si no aplica
     */
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
     * DTO de apoyo para renderizar el resumen del asistente.
     *
     * En esta clase encapsulo la información necesaria para mostrar en la vista el número de paso,
     * su título, el estado y el mensaje asociado.
     *
     * @author David Tomé Arnaiz
     */
    public static class PasoView {
        private final int numero;
        private final String titulo;
        private final String estado;
        private final String mensaje;

        /**
         * En este constructor inicializo un paso del resumen con su información de presentación.
         *
         * @param numero número del paso en el asistente
         * @param titulo título a mostrar
         * @param estado estado textual
         * @param mensaje mensaje asociado
         */
        public PasoView(int numero, String titulo, String estado, String mensaje) {
            this.numero = numero;
            this.titulo = titulo;
            this.estado = estado;
            this.mensaje = mensaje;
        }

        /**
         * En este método construyo un {@link PasoView} a partir del control persistido del paso.
         *
         * @param numero número del paso
         * @param titulo título del paso
         * @param c control persistido del paso (puede ser null)
         * @return instancia lista para renderizarse en la vista
         */
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