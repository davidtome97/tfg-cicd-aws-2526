package com.sistemagestionapp.model.dto;

/**
 * En esta clase represento el resultado de la validación o ejecución de un paso del asistente.
 *
 * Almaceno el estado del paso (por ejemplo, OK o KO) y un mensaje descriptivo que permite
 * informar al usuario del resultado de la operación realizada.
 *
 * @author David Tomé Arnaiz
 */
public class ResultadoPaso {

    private String estado;
    private String mensaje;

    /**
     * Constructor vacío necesario para la serialización y deserialización.
     */
    public ResultadoPaso() {
    }

    /**
     * En este constructor inicializo el resultado de un paso con su estado y mensaje.
     *
     * @param estado estado del paso (OK o KO)
     * @param mensaje mensaje descriptivo del resultado
     */
    public ResultadoPaso(String estado, String mensaje) {
        this.estado = estado;
        this.mensaje = mensaje;
    }

    /**
     * Devuelvo el estado del paso.
     *
     * @return estado del paso
     */
    public String getEstado() {
        return estado;
    }

    /**
     * Actualizo el estado del paso.
     *
     * @param estado nuevo estado del paso
     */
    public void setEstado(String estado) {
        this.estado = estado;
    }

    /**
     * Devuelvo el mensaje asociado al resultado del paso.
     *
     * @return mensaje descriptivo
     */
    public String getMensaje() {
        return mensaje;
    }

    /**
     * Actualizo el mensaje asociado al resultado del paso.
     *
     * @param mensaje nuevo mensaje descriptivo
     */
    public void setMensaje(String mensaje) {
        this.mensaje = mensaje;
    }
}