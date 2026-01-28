package com.sistemagestionapp.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * En esta entidad represento el estado de un paso concreto del asistente de despliegue
 * para una aplicación determinada.
 *
 * Utilizo esta clase para registrar qué paso se ha ejecutado, su estado (pendiente, correcto
 * o con error), un mensaje descriptivo y la fecha de la última ejecución. De este modo puedo
 * controlar el progreso del asistente y evitar inconsistencias entre pasos.
 *
 * Garantizo que solo exista un control por aplicación y paso mediante una restricción de unicidad.
 *
 * @author David Tomé Arnaiz
 */
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
     * Aplicación a la que pertenece este control de despliegue.
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "aplicacion_id", nullable = false)
    private Aplicacion aplicacion;

    /**
     * Paso del asistente de despliegue al que corresponde este control.
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PasoDespliegue paso;

    /**
     * Estado actual del paso.
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EstadoControl estado = EstadoControl.PENDIENTE;

    /**
     * Mensaje informativo o de error asociado a la ejecución del paso.
     */
    @Column(length = 1000)
    private String mensaje;

    /**
     * Fecha y hora de la última ejecución del paso.
     */
    private LocalDateTime fechaEjecucion;

    public ControlDespliegue() {
    }

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