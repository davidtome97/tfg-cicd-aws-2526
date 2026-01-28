package com.sistemagestionapp.model;

/**
 * En este enumerado defino los motores de base de datos soportados por el asistente de despliegue.
 *
 * Utilizo estos valores para adaptar la generación de configuraciones, variables de entorno
 * y guías de despliegue según el motor seleccionado.
 *
 * @author David Tomé Arnaiz
 */
public enum TipoBaseDatos {

    /**
     * Base de datos MySQL.
     */
    MYSQL,

    /**
     * Base de datos PostgreSQL.
     */
    POSTGRESQL,

    /**
     * Base de datos MongoDB.
     */
    MONGODB
}
