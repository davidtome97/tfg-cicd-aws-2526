package com.sistemagestionapp.service;

import com.sistemagestionapp.model.*;
import com.sistemagestionapp.repository.AplicacionRepository;
import com.sistemagestionapp.repository.ControlDespliegueRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;

@Service
public class DeployWizardService {

    private final AplicacionRepository aplicacionRepository;
    private final ControlDespliegueRepository controlDespliegueRepository;

    private static final EnumSet<PasoDespliegue> PASOS_REALES = EnumSet.of(
            PasoDespliegue.PRIMER_COMMIT,
            PasoDespliegue.SONAR_ANALISIS,
            PasoDespliegue.SONAR_INTEGRACION_GIT,
            PasoDespliegue.REPOSITORIO_GIT,
            PasoDespliegue.IMAGEN_ECR,
            PasoDespliegue.BASE_DATOS,
            PasoDespliegue.DESPLIEGUE_EC2
    );

    public DeployWizardService(AplicacionRepository aplicacionRepository,
                               ControlDespliegueRepository controlDespliegueRepository) {
        this.aplicacionRepository = aplicacionRepository;
        this.controlDespliegueRepository = controlDespliegueRepository;
    }

    // =========================
    // APP
    // =========================

    @Transactional(readOnly = true)
    public Aplicacion obtenerAplicacion(Long appId) {
        return aplicacionRepository.findById(appId)
                .orElseThrow(() -> new IllegalArgumentException("Aplicación no encontrada: " + appId));
    }

    @Transactional(readOnly = true)
    public Aplicacion getAppOrThrow(Long appId) {
        return obtenerAplicacion(appId);
    }

    // =========================
    // SONAR
    // =========================

    @Transactional
    public void guardarProjectKey(Long appId, String projectKey) {
        Aplicacion app = obtenerAplicacion(appId);

        String pk = projectKey == null ? "" : projectKey.trim();
        if (pk.isBlank()) {
            throw new IllegalArgumentException("El projectKey no puede estar vacío.");
        }

        app.setSonarProjectKey(pk);
        aplicacionRepository.save(app);
    }

    @Transactional(readOnly = true)
    public String obtenerProjectKey(Long appId) {
        return aplicacionRepository.findById(appId)
                .map(Aplicacion::getSonarProjectKey)
                .orElse(null);
    }

    // =========================
    // PASO 3: REPO
    // =========================

    @Transactional
    public void guardarRepoGit(Long appId, String proveedor, String repo) {
        Aplicacion app = obtenerAplicacion(appId);

        if (repo != null) app.setRepositorioGit(repo.trim());

        if ("github".equalsIgnoreCase(proveedor)) {
            app.setProveedorCiCd(ProveedorCiCd.GITHUB);
        } else if ("gitlab".equalsIgnoreCase(proveedor)) {
            app.setProveedorCiCd(ProveedorCiCd.GITLAB);
        } else if ("jenkins".equalsIgnoreCase(proveedor)) {
            app.setProveedorCiCd(ProveedorCiCd.JENKINS);
        }

        aplicacionRepository.save(app);
    }

    // =========================
    // PASO 4: AWS/ECR
    // =========================

    @Transactional
    public void guardarAwsEcrVars(Long appId,
                                  String ecrRepository,
                                  String awsRegion,
                                  String awsAccessKeyId,
                                  String awsSecretAccessKey,
                                  String awsAccountId) {

        Aplicacion app = obtenerAplicacion(appId);

        app.setEcrRepository(trimToNull(ecrRepository));
        app.setAwsRegion(trimToNull(awsRegion));
        app.setAwsAccessKeyId(trimToNull(awsAccessKeyId));
        app.setAwsSecretAccessKey(trimToNull(awsSecretAccessKey));
        app.setAwsAccountId(trimToNull(awsAccountId));

        aplicacionRepository.save(app);
    }

    // =========================
    // PASO 5: BBDD
    // =========================

    @Transactional
    public void guardarPaso5Bd(Long appId,
                               DbModo modo,
                               TipoBaseDatos tipo,
                               String dbName,
                               String dbUser,
                               String dbPassword,
                               Integer port,
                               String endpoint) {

        Aplicacion app = obtenerAplicacion(appId);

        app.setDbModo(modo);
        app.setTipoBaseDatos(tipo);

        app.setNombreBaseDatos(dbName == null ? null : dbName.trim());
        app.setUsuarioBaseDatos(dbUser == null ? null : dbUser.trim());
        app.setPasswordBaseDatos(dbPassword == null ? null : dbPassword.trim());

        // ✅ Guardar endpoint/port (IMPORTANTE para REMOTE y para DB_URI en Mongo remoto)
        app.setDbPort(port);
        app.setDbEndpoint(endpoint == null ? null : endpoint.trim());

        aplicacionRepository.save(app);
    }

    // =========================
    // PASO 6: EC2 (NUEVO)
    // =========================

    /**
     * Método "principal" que tú ya estabas usando en algunos sitios.
     */
    @Transactional
    public void guardarVarsEc2(Long appId,
                               String ec2Host,
                               String ec2User,
                               String ec2KnownHosts,
                               Integer appPort,
                               String ec2LlaveSsh) {

        Aplicacion app = obtenerAplicacion(appId);

        app.setEc2Host(normalizar(ec2Host));
        app.setEc2User(normalizar(ec2User));
        app.setEc2KnownHosts(normalizar(ec2KnownHosts));
        app.setAppPort(appPort);

        // 🔐 NO machacar ssh key
        if (!esPlaceholderSecreto(ec2LlaveSsh)) {
            app.setEc2LlaveSsh(ec2LlaveSsh.trim());
        }

        aplicacionRepository.save(app);
    }

    /**
     * ✅ ESTE ES EL QUE TE ESTÁ FALLANDO EN EL CONTROLLER:
     * DeployWizardController llama a guardarPaso6Ec2(...), así que lo dejamos como "alias"
     * y delega en guardarVarsEc2 con el ORDEN CORRECTO.
     */
    @Transactional
    public void guardarPaso6Ec2(Long appId,
                                String ec2Host,
                                String ec2User,
                                String ec2KnownHosts,
                                String ec2LlaveSsh,
                                Integer appPort) {

        guardarVarsEc2(appId, ec2Host, ec2User, ec2KnownHosts, appPort, ec2LlaveSsh);
    }

    // =========================
    // CONTROLES (estado pasos)
    // =========================

    @Transactional(readOnly = true)
    public List<ControlDespliegue> obtenerControlesOrdenados(Long appId) {
        return controlDespliegueRepository.findByAplicacionIdOrderByPasoAsc(appId);
    }

    @Transactional(readOnly = true)
    public Optional<ControlDespliegue> obtenerControl(Long appId, PasoDespliegue paso) {
        return controlDespliegueRepository.findByAplicacionIdAndPaso(appId, paso);
    }

    @Transactional
    public void marcarPaso(Long appId, PasoDespliegue paso, EstadoControl estado, String mensaje) {
        Aplicacion app = obtenerAplicacion(appId);

        ControlDespliegue control = controlDespliegueRepository
                .findByAplicacionIdAndPaso(appId, paso)
                .orElseGet(() -> {
                    ControlDespliegue cd = new ControlDespliegue();
                    cd.setAplicacion(app);
                    cd.setPaso(paso);
                    cd.setEstado(EstadoControl.PENDIENTE);
                    return cd;
                });

        control.setEstado(estado);
        control.setMensaje(mensaje);
        control.setFechaEjecucion(LocalDateTime.now());
        controlDespliegueRepository.save(control);

        actualizarResumenFinalSiProcede(appId);
    }

    public int totalPasos() {
        return PASOS_REALES.size();
    }

    private static boolean esPlaceholderSecreto(String s) {
        if (s == null) return true;
        String v = s.trim();
        return v.isEmpty()
                || v.equalsIgnoreCase("(secreto guardado)")
                || v.equalsIgnoreCase("(pegar de nuevo)");
    }

    private static String normalizar(String s) {
        if (s == null) return null;
        String v = s.trim();
        return v.isEmpty() ? null : v;
    }

    private void actualizarResumenFinalSiProcede(Long appId) {

        boolean todosOk = PASOS_REALES.stream().allMatch(p ->
                controlDespliegueRepository.findByAplicacionIdAndPaso(appId, p)
                        .map(c -> c.getEstado() == EstadoControl.OK)
                        .orElse(false)
        );

        if (!todosOk) return;

        ControlDespliegue resumen = controlDespliegueRepository
                .findByAplicacionIdAndPaso(appId, PasoDespliegue.RESUMEN_FINAL)
                .orElseGet(() -> {
                    Aplicacion app = obtenerAplicacion(appId);
                    ControlDespliegue cd = new ControlDespliegue();
                    cd.setAplicacion(app);
                    cd.setPaso(PasoDespliegue.RESUMEN_FINAL);
                    cd.setEstado(EstadoControl.PENDIENTE);
                    return cd;
                });

        resumen.setEstado(EstadoControl.OK);
        resumen.setMensaje("Todos los pasos completados correctamente.");
        resumen.setFechaEjecucion(LocalDateTime.now());
        controlDespliegueRepository.save(resumen);
    }

    private static String trimToNull(String s) {
        if (s == null) return null;
        String t = s.trim();
        return t.isBlank() ? null : t;
    }
}
