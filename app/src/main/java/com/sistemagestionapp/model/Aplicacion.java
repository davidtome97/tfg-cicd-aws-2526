package com.sistemagestionapp.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
public class Aplicacion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String nombre;
    private String descripcion;

    @Enumerated(EnumType.STRING)
    private Lenguaje lenguaje;



    @Enumerated(EnumType.STRING)
    private ProveedorCiCd proveedorCiCd;

    @Enumerated(EnumType.STRING)
    private TipoBaseDatos tipoBaseDatos;

    private String nombreBaseDatos;
    private String usuarioBaseDatos;
    private String passwordBaseDatos;

    private Integer puertoAplicacion;

    private String repositorioGit;
    private String sonarProjectKey;
    private String nombreImagenEcr;

    private LocalDateTime fechaCreacion = LocalDateTime.now();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id")
    private Usuario propietario;

    @Column(name = "db_modo")
    private String dbModo; // local | remote

    @OneToMany(mappedBy = "aplicacion", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ControlDespliegue> controles = new ArrayList<>();

    public Aplicacion() {}

    @PrePersist
    public void prePersist() {
        if (fechaCreacion == null) {
            fechaCreacion = LocalDateTime.now();
        }
    }

    // --- Getters y setters ---

    public String getDbModo() {
        return dbModo;
    }

    public void setDbModo(String dbModo) {
        this.dbModo = dbModo;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }

    public String getDescripcion() { return descripcion; }
    public void setDescripcion(String descripcion) { this.descripcion = descripcion; }

    public Lenguaje getLenguaje() { return lenguaje; }
    public void setLenguaje(Lenguaje lenguaje) { this.lenguaje = lenguaje; }

    public ProveedorCiCd getProveedorCiCd() { return proveedorCiCd; }
    public void setProveedorCiCd(ProveedorCiCd proveedorCiCd) { this.proveedorCiCd = proveedorCiCd; }

    public TipoBaseDatos getTipoBaseDatos() { return tipoBaseDatos; }
    public void setTipoBaseDatos(TipoBaseDatos tipoBaseDatos) { this.tipoBaseDatos = tipoBaseDatos; }

    public String getNombreBaseDatos() { return nombreBaseDatos; }
    public void setNombreBaseDatos(String nombreBaseDatos) { this.nombreBaseDatos = nombreBaseDatos; }

    public String getUsuarioBaseDatos() { return usuarioBaseDatos; }
    public void setUsuarioBaseDatos(String usuarioBaseDatos) { this.usuarioBaseDatos = usuarioBaseDatos; }

    public String getPasswordBaseDatos() { return passwordBaseDatos; }
    public void setPasswordBaseDatos(String passwordBaseDatos) { this.passwordBaseDatos = passwordBaseDatos; }

    public Integer getPuertoAplicacion() { return puertoAplicacion; }
    public void setPuertoAplicacion(Integer puertoAplicacion) { this.puertoAplicacion = puertoAplicacion; }

    public String getRepositorioGit() { return repositorioGit; }
    public void setRepositorioGit(String repositorioGit) { this.repositorioGit = repositorioGit; }

    public String getSonarProjectKey() { return sonarProjectKey; }
    public void setSonarProjectKey(String sonarProjectKey) { this.sonarProjectKey = sonarProjectKey; }

    public String getNombreImagenEcr() { return nombreImagenEcr; }
    public void setNombreImagenEcr(String nombreImagenEcr) { this.nombreImagenEcr = nombreImagenEcr; }

    public LocalDateTime getFechaCreacion() { return fechaCreacion; }
    public void setFechaCreacion(LocalDateTime fechaCreacion) { this.fechaCreacion = fechaCreacion; }

    public Usuario getPropietario() { return propietario; }
    public void setPropietario(Usuario propietario) { this.propietario = propietario; }

    public List<ControlDespliegue> getControles() { return controles; }
    public void setControles(List<ControlDespliegue> controles) { this.controles = controles; }
}