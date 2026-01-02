package com.sistemagestionapp.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(
        name = "control_despliegue",
        uniqueConstraints = {
                @UniqueConstraint(columnNames = {"aplicacion_id", "paso"})
        }
)
public class ControlDespliegue {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Aplicación a la que pertenece este paso
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "aplicacion_id", nullable = false)
    private Aplicacion aplicacion;

    /**
     * Paso del despliegue (Sonar, Git, ECR, EC2, etc.)
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PasoDespliegue paso;

    /**
     * Estado del paso
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EstadoControl estado = EstadoControl.PENDIENTE;

    /**
     * Mensaje de error o info del paso
     */
    @Column(length = 1000)
    private String mensaje;

    /**
     * Fecha de última ejecución del paso
     */
    private LocalDateTime fechaEjecucion;

    public ControlDespliegue() {
    }

    /* =====================
       Getters y Setters
       ===================== */

    public Long getId() {
        return id;
    }

    public Aplicacion getAplicacion() {
        return aplicacion;
    }

    public void setAplicacion(Aplicacion aplicacion) {
        this.aplicacion = aplicacion;
    }

    public PasoDespliegue getPaso() {
        return paso;
    }

    public void setPaso(PasoDespliegue paso) {
        this.paso = paso;
    }

    public EstadoControl getEstado() {
        return estado;
    }

    public void setEstado(EstadoControl estado) {
        this.estado = estado;
    }

    public String getMensaje() {
        return mensaje;
    }

    public void setMensaje(String mensaje) {
        this.mensaje = mensaje;
    }

    public LocalDateTime getFechaEjecucion() {
        return fechaEjecucion;
    }

    public void setFechaEjecucion(LocalDateTime fechaEjecucion) {
        this.fechaEjecucion = fechaEjecucion;
    }
}