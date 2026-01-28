package com.sistemagestionapp.model;

/**
 * En este enumerado defino los proveedores de integración y despliegue continuos
 * soportados por el sistema.
 *
 * Utilizo estos valores para identificar la plataforma de CI/CD asociada a cada
 * aplicación y adaptar la generación de configuraciones y guías del asistente.
 *
 * @author David Tomé Arnaiz
 */
public enum ProveedorCiCd {

    /**
     * Proveedor basado en GitHub Actions.
     */
    GITHUB,

    /**
     * Proveedor basado en GitLab CI/CD.
     */
    GITLAB,

    /**
     * Proveedor basado en Jenkins.
     */
    JENKINS
}
