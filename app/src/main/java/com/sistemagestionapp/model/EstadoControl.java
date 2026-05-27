package com.sistemagestionapp.model;

/**
 * En este enumerado represento los posibles estados de un paso del asistente de despliegue.
 *
 * Utilizo estos valores para controlar el flujo del asistente, mostrar el progreso
 * al usuario y decidir si se permite avanzar al siguiente paso.
 *
 * @author David Tomé Arnaiz
 */
public enum EstadoControl {

    /**
     * El paso aún no ha sido ejecutado o confirmado.
     */
    PENDIENTE,

    /**
     * El paso se ha completado correctamente.
     */
    OK,

    /**
     * El paso se ha ejecutado con error.
     */
    KO
}