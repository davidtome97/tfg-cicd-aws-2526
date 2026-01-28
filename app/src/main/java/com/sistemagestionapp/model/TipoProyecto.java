package com.sistemagestionapp.model;

/**
 * En este enumerado defino los tipos de proyecto que puedo gestionar en el sistema.
 *
 * Utilizo estos valores para diferenciar entre proyectos que únicamente generan
 * configuraciones de despliegue y proyectos que incluyen además una aplicación
 * de demostración.
 *
 * @author David Tomé Arnaiz
 */
public enum TipoProyecto {

    /**
     * Proyecto de tipo configuración.
     *
     * Solo incluye ficheros de configuración, como pipelines de CI/CD,
     * docker-compose y ficheros de variables de la aplicación.
     */
    CONFIG,

    /**
     * Proyecto de tipo demostración.
     *
     * Incluye, además de las configuraciones, una aplicación de ejemplo
     * desarrollada en el lenguaje seleccionado.
     */
    DEMO
}
