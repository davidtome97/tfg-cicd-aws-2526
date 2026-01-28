package com.sistemagestionapp.service;

import com.sistemagestionapp.model.Aplicacion;
import com.sistemagestionapp.model.ControlDespliegue;
import com.sistemagestionapp.model.DbModo;
import com.sistemagestionapp.model.EstadoControl;
import com.sistemagestionapp.model.PasoDespliegue;
import com.sistemagestionapp.model.ProveedorCiCd;
import com.sistemagestionapp.model.TipoBaseDatos;
import com.sistemagestionapp.repository.AplicacionRepository;
import com.sistemagestionapp.repository.ControlDespliegueRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;

/**
 * En este servicio centralizo la lógica principal del asistente de despliegue.
 *
 * Gestiono la persistencia de la configuración de cada paso (Sonar, Git, AWS/ECR, base de datos y EC2),
 * así como el registro del estado de ejecución mediante {@link ControlDespliegue}. También utilizo un
 * conjunto de pasos "reales" para calcular el progreso y para decidir cuándo marcar el resumen final.
 *
 * @author David Tomé Arnaiz
 */
@Service
public class DeployWizardService {

    private final AplicacionRepository aplicacionRepository;
    private final ControlDespliegueRepository controlDespliegueRepository;

    /**
     * En este conjunto defino los pasos que considero para el progreso real del asistente.
     *
     * Excluyo el resumen final porque su estado se calcula a partir del resto de pasos.
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
     * En este constructor inyecto los repositorios necesarios para gestionar las aplicaciones
     * y el estado de los pasos del asistente.
     *
     * @param aplicacionRepository repositorio de aplicaciones
     * @param controlDespliegueRepository repositorio de controles de despliegue
     */
    public DeployWizardService(AplicacionRepository aplicacionRepository,
                               ControlDespliegueRepository controlDespliegueRepository) {
        this.aplicacionRepository = aplicacionRepository;
        this.controlDespliegueRepository = controlDespliegueRepository;
    }

    /**
     * Obtengo una aplicación a partir de su identificador.
     *
     * @param appId identificador de la aplicación
     * @return aplicación encontrada
     * @throws IllegalArgumentException si la aplicación no existe
     */
    @Transactional(readOnly = true)
    public Aplicacion obtenerAplicacion(Long appId) {
        return aplicacionRepository.findById(appId)
                .orElseThrow(() -> new IllegalArgumentException("Aplicación no encontrada: " + appId));
    }

    /**
     * Devuelvo la aplicación o lanzo excepción si no existe.
     *
     * @param appId identificador de la aplicación
     * @return aplicación encontrada
     */
    @Transactional(readOnly = true)
    public Aplicacion getAppOrThrow(Long appId) {
        return obtenerAplicacion(appId);
    }

    /**
     * Guardo el Project Key de SonarCloud para una aplicación.
     *
     * @param appId identificador de la aplicación
     * @param projectKey project key de SonarCloud
     * @throws IllegalArgumentException si el projectKey está vacío
     */
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

    /**
     * Obtengo el Project Key de SonarCloud almacenado para una aplicación.
     *
     * @param appId identificador de la aplicación
     * @return project key almacenado o {@code null} si no existe
     */
    @Transactional(readOnly = true)
    public String obtenerProjectKey(Long appId) {
        return aplicacionRepository.findById(appId)
                .map(Aplicacion::getSonarProjectKey)
                .orElse(null);
    }

    /**
     * Guardo el proveedor CI/CD y el repositorio Git asociados a una aplicación.
     *
     * @param appId identificador de la aplicación
     * @param proveedor proveedor en formato texto (github, gitlab o jenkins)
     * @param repo repositorio normalizado (por ejemplo, owner/repo)
     */
    @Transactional
    public void guardarRepoGit(Long appId, String proveedor, String repo) {
        Aplicacion app = obtenerAplicacion(appId);

        if (repo != null) {
            app.setRepositorioGit(repo.trim());
        }

        if ("github".equalsIgnoreCase(proveedor)) {
            app.setProveedorCiCd(ProveedorCiCd.GITHUB);
        } else if ("gitlab".equalsIgnoreCase(proveedor)) {
            app.setProveedorCiCd(ProveedorCiCd.GITLAB);
        } else if ("jenkins".equalsIgnoreCase(proveedor)) {
            app.setProveedorCiCd(ProveedorCiCd.JENKINS);
        }

        aplicacionRepository.save(app);
    }

    /**
     * Guardo las variables de AWS y ECR asociadas a una aplicación.
     *
     * @param appId identificador de la aplicación
     * @param ecrRepository repositorio ECR
     * @param awsRegion región AWS
     * @param awsAccessKeyId access key de AWS
     * @param awsSecretAccessKey secret access key de AWS
     * @param awsAccountId account id de AWS
     */
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

    /**
     * Guardo la configuración de base de datos asociada al paso 5 del asistente.
     *
     * @param appId identificador de la aplicación
     * @param modo modo de base de datos (LOCAL o REMOTE)
     * @param tipo motor de base de datos
     * @param dbName nombre de base de datos
     * @param dbUser usuario de base de datos
     * @param dbPassword contraseña de base de datos
     * @param port puerto de conexión
     * @param endpoint endpoint o URI (especialmente relevante en modo REMOTE)
     */
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

        app.setDbPort(port);
        app.setDbEndpoint(endpoint == null ? null : endpoint.trim());

        aplicacionRepository.save(app);
    }

    /**
     * Guardo las variables necesarias para el despliegue en EC2 (paso 6).
     *
     * Evito sobrescribir la clave SSH si el valor recibido corresponde a un placeholder
     * que indica que el secreto ya está almacenado.
     *
     * @param appId identificador de la aplicación
     * @param ec2Host host o DNS público de la EC2
     * @param ec2User usuario SSH de la EC2
     * @param ec2KnownHosts known_hosts generado para evitar prompts
     * @param appPort puerto público de la aplicación
     * @param ec2LlaveSsh clave privada SSH (opcional, puede conservarse si ya estaba guardada)
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

        if (!esPlaceholderSecreto(ec2LlaveSsh)) {
            app.setEc2LlaveSsh(ec2LlaveSsh.trim());
        }

        aplicacionRepository.save(app);
    }

    /**
     * Mantengo este método como alias para compatibilidad con el controlador,
     * delegando en {@link #guardarVarsEc2(Long, String, String, String, Integer, String)}.
     *
     * @param appId identificador de la aplicación
     * @param ec2Host host o DNS público de la EC2
     * @param ec2User usuario SSH
     * @param ec2KnownHosts known_hosts
     * @param ec2LlaveSsh clave privada SSH
     * @param appPort puerto público de la aplicación
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

    /**
     * Obtengo los controles de despliegue de una aplicación ordenados por paso.
     *
     * @param appId identificador de la aplicación
     * @return lista de controles ordenados
     */
    @Transactional(readOnly = true)
    public List<ControlDespliegue> obtenerControlesOrdenados(Long appId) {
        return controlDespliegueRepository.findByAplicacionIdOrderByPasoAsc(appId);
    }

    /**
     * Obtengo el control asociado a un paso concreto de una aplicación.
     *
     * @param appId identificador de la aplicación
     * @param paso paso del asistente
     * @return control del paso si existe
     */
    @Transactional(readOnly = true)
    public Optional<ControlDespliegue> obtenerControl(Long appId, PasoDespliegue paso) {
        return controlDespliegueRepository.findByAplicacionIdAndPaso(appId, paso);
    }

    /**
     * Registro el resultado de un paso del asistente.
     *
     * Creo el control si no existe, actualizo su estado, mensaje y fecha de ejecución,
     * y compruebo si procede actualizar el resumen final.
     *
     * @param appId identificador de la aplicación
     * @param paso paso del asistente
     * @param estado estado a registrar
     * @param mensaje mensaje descriptivo asociado al estado
     */
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

    /**
     * Devuelvo el número total de pasos considerados para el progreso real del asistente.
     *
     * @return número de pasos reales
     */
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

        if (!todosOk) {
            return;
        }

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