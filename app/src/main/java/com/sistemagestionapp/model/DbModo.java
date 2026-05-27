package com.sistemagestionapp.model;

/**
 * En este enumerado defino los modos de configuración de la base de datos
 * soportados por el asistente de despliegue.
 *
 * Utilizo LOCAL cuando la base de datos se despliega junto a la aplicación
 * (por ejemplo, mediante contenedores), y REMOTE cuando la base de datos
 * se encuentra en un servicio externo.
 *
 * @author David Tomé Arnaiz
 */
public enum DbModo {

    /**
     * Base de datos desplegada en modo local junto a la aplicación.
     */
    LOCAL,

    /**
     * Base de datos alojada en un servicio remoto.
     */
    REMOTE
}