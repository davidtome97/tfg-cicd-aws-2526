package com.sistemagestionapp.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * En esta entidad represento una aplicación gestionada por el sistema.
 *
 * Centralizo toda la información necesaria para el asistente de despliegue, incluyendo:
 * - datos generales del proyecto,
 * - configuración de SonarCloud,
 * - repositorio Git y proveedor CI/CD,
 * - variables de AWS/ECR,
 * - configuración de base de datos,
 * - y parámetros de despliegue en EC2.
 *
 * Cada instancia de esta entidad pertenece a un usuario y mantiene relación con los
 * controles de estado de cada paso del asistente.
 *
 * @author David Tomé Arnaiz
 */
@Entity
@Table(name = "aplicacion")
public class Aplicacion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /* =========================
       DATOS GENERALES
       ========================= */

    /**
     * Nombre identificativo de la aplicación.
     */
    @Column(nullable = false, length = 150)
    private String nombre;

    /**
     * Descripción opcional de la aplicación.
     */
    @Column(length = 500)
    private String descripcion;

    /**
     * Lenguaje principal de la aplicación.
     */
    @Enumerated(EnumType.STRING)
    @Column(length = 30)
    private Lenguaje lenguaje;

    /**
     * Proveedor de CI/CD utilizado (por ejemplo, GitHub o GitLab).
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "proveedor_ci_cd", length = 30)
    private ProveedorCiCd proveedorCiCd;

    /**
     * Repositorio Git asociado a la aplicación.
     */
    @Column(name = "repositorio_git", length = 255)
    private String repositorioGit;

    /**
     * Puerto público en el que se expone la aplicación.
     */
    @Column(name = "puerto_aplicacion")
    private Integer puertoAplicacion;

    /* =========================
       BASE DE DATOS (PASO 5)
       ========================= */

    /**
     * Tipo de base de datos utilizado.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_base_datos", length = 30)
    private TipoBaseDatos tipoBaseDatos;

    /**
     * Modo de base de datos (LOCAL o REMOTE).
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "db_modo", length = 20)
    private DbModo dbModo = DbModo.LOCAL;

    /**
     * Nombre de la base de datos.
     */
    @Column(name = "nombre_base_datos", length = 120)
    private String nombreBaseDatos;

    /**
     * Usuario de acceso a la base de datos.
     */
    @Column(name = "usuario_base_datos", length = 120)
    private String usuarioBaseDatos;

    /**
     * Contraseña de la base de datos.
     *
     * La almaceno como TEXT para evitar problemas con Large Objects en PostgreSQL.
     */
    @Column(name = "password_base_datos", columnDefinition = "TEXT")
    private String passwordBaseDatos;

    /**
     * Puerto de conexión a la base de datos (opcional).
     */
    @Column(name = "db_port")
    private Integer dbPort;

    /**
     * Endpoint o URI de conexión a la base de datos en modo remoto.
     */
    @Column(name = "db_endpoint", columnDefinition = "TEXT")
    private String dbEndpoint;

    /* =========================
       SONAR (PASO 1)
       ========================= */

    /**
     * Project Key del proyecto en SonarCloud.
     */
    @Column(name = "sonar_project_key", length = 255)
    private String sonarProjectKey;

    /**
     * URL del servidor de Sonar (por defecto SonarCloud).
     */
    @Column(name = "sonar_host_url", length = 255)
    private String sonarHostUrl;

    /**
     * Organización de SonarCloud.
     */
    @Column(name = "sonar_organization", length = 255)
    private String sonarOrganization;

    /**
     * Token de acceso a SonarCloud.
     *
     * No utilizo @Lob para evitar problemas de auto-commit en PostgreSQL.
     */
    @Column(name = "sonar_token", columnDefinition = "TEXT")
    private String sonarToken;

    /* =========================
       AWS / ECR (PASO 4)
       ========================= */

    /**
     * Nombre del repositorio ECR.
     */
    @Column(name = "ecr_repository", length = 255)
    private String ecrRepository;

    /**
     * Nombre de la imagen Docker en ECR.
     */
    @Column(name = "nombre_imagen_ecr", length = 255)
    private String nombreImagenEcr;

    /**
     * Tag de la imagen Docker.
     */
    @Column(name = "image_tag", length = 80)
    private String imageTag;

    /**
     * Región de AWS.
     */
    @Column(name = "aws_region", length = 50)
    private String awsRegion;

    /**
     * Identificador de la cuenta AWS.
     */
    @Column(name = "aws_account_id", length = 50)
    private String awsAccountId;

    /**
     * Access Key de AWS.
     */
    @Column(name = "aws_access_key_id", length = 128)
    private String awsAccessKeyId;

    /**
     * Secret Access Key de AWS.
     *
     * La almaceno como TEXT para evitar problemas con OID en PostgreSQL.
     */
    @Column(name = "aws_secret_access_key", columnDefinition = "TEXT")
    private String awsSecretAccessKey;

    /* =========================
       DESPLIEGUE
       ========================= */

    /**
     * Tipo de proyecto gestionado por el asistente.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_proyecto", length = 30)
    private TipoProyecto tipoProyecto = TipoProyecto.CONFIG;

    /* =========================
       METADATOS
       ========================= */

    /**
     * Fecha de creación de la aplicación.
     */
    @Column(name = "fecha_creacion", nullable = false, updatable = false)
    private LocalDateTime fechaCreacion;

    /**
     * Usuario propietario de la aplicación.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id")
    private Usuario propietario;

    /* =========================
       EC2 (PASO 6)
       ========================= */

    /**
     * Host público de la instancia EC2.
     */
    @Column(name = "ec2_host", length = 255)
    private String ec2Host;

    /**
     * Usuario SSH de la instancia EC2.
     */
    @Column(name = "ec2_user", length = 100)
    private String ec2User;

    /**
     * Known hosts necesarios para evitar prompts SSH.
     */
    @Column(name = "ec2_known_hosts", columnDefinition = "TEXT")
    private String ec2KnownHosts;

    /**
     * Clave privada SSH utilizada para el despliegue.
     */
    @Column(name = "ec2_llave_ssh", columnDefinition = "TEXT")
    private String ec2LlaveSsh;

    /**
     * Puerto de la aplicación desplegada en EC2.
     */
    @Column(name = "app_port")
    private Integer appPort;

    /**
     * Inicializo la fecha de creación antes de persistir la entidad.
     */
    @PrePersist
    public void prePersist() {
        if (fechaCreacion == null) {
            fechaCreacion = LocalDateTime.now();
        }
    }

    /* =========================
       RELACIONES
       ========================= */

    /**
     * Controles de estado asociados a los pasos del asistente.
     */
    @OneToMany(
            mappedBy = "aplicacion",
            cascade = CascadeType.ALL,
            orphanRemoval = true
    )
    private List<ControlDespliegue> controles = new ArrayList<>();

    /* =========================
       CONSTRUCTORES / CICLO VIDA
       ========================= */

    public Aplicacion() {
    }

    /**
     * Normalizo valores por defecto antes de actualizar la entidad.
     */
    @PreUpdate
    public void preUpdate() {
        normalizarDefaults();
    }

    private void normalizarDefaults() {
        if (sonarHostUrl == null || sonarHostUrl.isBlank()) {
            sonarHostUrl = "https://sonarcloud.io";
        }
        if (dbModo == null) {
            dbModo = DbModo.LOCAL;
        }
        if (tipoProyecto == null) {
            tipoProyecto = TipoProyecto.CONFIG;
        }
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }

    public String getDescripcion() { return descripcion; }
    public void setDescripcion(String descripcion) { this.descripcion = descripcion; }

    public Lenguaje getLenguaje() { return lenguaje; }
    public void setLenguaje(Lenguaje lenguaje) { this.lenguaje = lenguaje; }

    public ProveedorCiCd getProveedorCiCd() { return proveedorCiCd; }
    public void setProveedorCiCd(ProveedorCiCd proveedorCiCd) { this.proveedorCiCd = proveedorCiCd; }

    public String getRepositorioGit() { return repositorioGit; }
    public void setRepositorioGit(String repositorioGit) { this.repositorioGit = repositorioGit; }

    public Integer getPuertoAplicacion() { return puertoAplicacion; }
    public void setPuertoAplicacion(Integer puertoAplicacion) { this.puertoAplicacion = puertoAplicacion; }

    public TipoBaseDatos getTipoBaseDatos() { return tipoBaseDatos; }
    public void setTipoBaseDatos(TipoBaseDatos tipoBaseDatos) { this.tipoBaseDatos = tipoBaseDatos; }

    public DbModo getDbModo() { return dbModo; }
    public void setDbModo(DbModo dbModo) { this.dbModo = dbModo; }

    public String getNombreBaseDatos() { return nombreBaseDatos; }
    public void setNombreBaseDatos(String nombreBaseDatos) { this.nombreBaseDatos = nombreBaseDatos; }

    public String getUsuarioBaseDatos() { return usuarioBaseDatos; }
    public void setUsuarioBaseDatos(String usuarioBaseDatos) { this.usuarioBaseDatos = usuarioBaseDatos; }

    public String getPasswordBaseDatos() { return passwordBaseDatos; }
    public void setPasswordBaseDatos(String passwordBaseDatos) { this.passwordBaseDatos = passwordBaseDatos; }

    public Integer getDbPort() { return dbPort; }
    public void setDbPort(Integer dbPort) { this.dbPort = dbPort; }

    public String getDbEndpoint() { return dbEndpoint; }
    public void setDbEndpoint(String dbEndpoint) { this.dbEndpoint = dbEndpoint; }

    public String getSonarProjectKey() { return sonarProjectKey; }
    public void setSonarProjectKey(String sonarProjectKey) { this.sonarProjectKey = sonarProjectKey; }

    public String getSonarHostUrl() { return sonarHostUrl; }
    public void setSonarHostUrl(String sonarHostUrl) { this.sonarHostUrl = sonarHostUrl; }

    public String getSonarOrganization() { return sonarOrganization; }
    public void setSonarOrganization(String sonarOrganization) { this.sonarOrganization = sonarOrganization; }

    public String getSonarToken() { return sonarToken; }
    public void setSonarToken(String sonarToken) { this.sonarToken = sonarToken; }

    public String getEcrRepository() { return ecrRepository; }
    public void setEcrRepository(String ecrRepository) { this.ecrRepository = ecrRepository; }

    public String getNombreImagenEcr() { return nombreImagenEcr; }
    public void setNombreImagenEcr(String nombreImagenEcr) { this.nombreImagenEcr = nombreImagenEcr; }

    public String getImageTag() { return imageTag; }
    public void setImageTag(String imageTag) { this.imageTag = imageTag; }

    public String getAwsRegion() { return awsRegion; }
    public void setAwsRegion(String awsRegion) { this.awsRegion = awsRegion; }

    public String getAwsAccountId() { return awsAccountId; }
    public void setAwsAccountId(String awsAccountId) { this.awsAccountId = awsAccountId; }

    public String getAwsAccessKeyId() { return awsAccessKeyId; }
    public void setAwsAccessKeyId(String awsAccessKeyId) { this.awsAccessKeyId = awsAccessKeyId; }

    public String getAwsSecretAccessKey() { return awsSecretAccessKey; }
    public void setAwsSecretAccessKey(String awsSecretAccessKey) { this.awsSecretAccessKey = awsSecretAccessKey; }

    public TipoProyecto getTipoProyecto() { return tipoProyecto; }
    public void setTipoProyecto(TipoProyecto tipoProyecto) { this.tipoProyecto = tipoProyecto; }

    public LocalDateTime getFechaCreacion() { return fechaCreacion; }
    public void setFechaCreacion(LocalDateTime fechaCreacion) { this.fechaCreacion = fechaCreacion; }

    public Usuario getPropietario() { return propietario; }
    public void setPropietario(Usuario propietario) { this.propietario = propietario; }

    public String getEc2Host() { return ec2Host; }
    public void setEc2Host(String ec2Host) { this.ec2Host = ec2Host; }

    public String getEc2User() { return ec2User; }
    public void setEc2User(String ec2User) { this.ec2User = ec2User; }

    public String getEc2KnownHosts() { return ec2KnownHosts; }
    public void setEc2KnownHosts(String ec2KnownHosts) { this.ec2KnownHosts = ec2KnownHosts; }

    public String getEc2LlaveSsh() { return ec2LlaveSsh; }
    public void setEc2LlaveSsh(String ec2LlaveSsh) { this.ec2LlaveSsh = ec2LlaveSsh; }

    public Integer getAppPort() { return appPort; }
    public void setAppPort(Integer appPort) { this.appPort = appPort; }

    public List<ControlDespliegue> getControles() { return controles; }
    public void setControles(List<ControlDespliegue> controles) { this.controles = controles; }
}