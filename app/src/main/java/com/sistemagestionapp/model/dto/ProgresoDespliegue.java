package com.sistemagestionapp.model.dto;

/**
 * En esta clase encapsulo el progreso del asistente de despliegue para una aplicación.
 *
 * Almaceno el número de pasos completados correctamente y el número total de pasos,
 * de forma que pueda calcularse y mostrarse el progreso en la interfaz de usuario.
 *
 * @author David Tomé Arnaiz
 */
public class ProgresoDespliegue {

    private final long ok;
    private final int total;

    /**
     * En este constructor inicializo el progreso del despliegue.
     *
     * @param ok número de pasos completados correctamente
     * @param total número total de pasos del asistente
     */
    public ProgresoDespliegue(long ok, int total) {
        this.ok = ok;
        this.total = total;
    }

    /**
     * Devuelvo el número de pasos completados correctamente.
     *
     * @return pasos en estado OK
     */
    public long getOk() {
        return ok;
    }

    /**
     * Devuelvo el número total de pasos del asistente.
     *
     * @return total de pasos
     */
    public int getTotal() {
        return total;
    }
}