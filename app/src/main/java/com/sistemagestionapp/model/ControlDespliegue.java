package com.sistemagestionapp.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
public class ControlDespliegue {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "aplicacion_id")
    private Aplicacion aplicacion;

    @Enumerated(EnumType.STRING)
    private PasoDespliegue paso;

    @Enumerated(EnumType.STRING)
    private EstadoControl estado = EstadoControl.PENDIENTE;

    private String mensaje;

    private LocalDateTime fechaEjecucion;

    public ControlDespliegue() {}

    public Long getId() { return id; }

    public Aplicacion getAplicacion() { return aplicacion; }
    public void setAplicacion(Aplicacion aplicacion) { this.aplicacion = aplicacion; }

    public PasoDespliegue getPaso() { return paso; }
    public void setPaso(PasoDespliegue paso) { this.paso = paso; }

    public EstadoControl getEstado() { return estado; }
    public void setEstado(EstadoControl estado) { this.estado = estado; }

    public String getMensaje() { return mensaje; }
    public void setMensaje(String mensaje) { this.mensaje = mensaje; }

    public LocalDateTime getFechaEjecucion() { return fechaEjecucion; }
    public void setFechaEjecucion(LocalDateTime fechaEjecucion) { this.fechaEjecucion = fechaEjecucion; }
}