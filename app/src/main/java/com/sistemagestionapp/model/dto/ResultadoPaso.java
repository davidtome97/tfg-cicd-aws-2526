package com.sistemagestionapp.model.dto;

public class ResultadoPaso {

    private String estado;     // OK / KO
    private String mensaje;    // detalle del resultado

    public ResultadoPaso() {}

    public ResultadoPaso(String estado, String mensaje) {
        this.estado = estado;
        this.mensaje = mensaje;
    }

    public String getEstado() {
        return estado;
    }

    public void setEstado(String estado) {
        this.estado = estado;
    }

    public String getMensaje() {
        return mensaje;
    }

    public void setMensaje(String mensaje) {
        this.mensaje = mensaje;
    }
}
