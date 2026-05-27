package com.sistemagestionapp.model;

/**
 * En este enumerado defino los pasos que componen el asistente de despliegue.
 *
 * Cada valor representa una fase concreta del proceso y mantiene una referencia
 * al paso previo, lo que me permite controlar el orden de ejecución, validar el
 * progreso y bloquear el acceso a pasos posteriores si los anteriores no se han
 * completado correctamente.
 *
 * @author David Tomé Arnaiz
 */
public enum PasoDespliegue {

    /**
     * Inicialización del repositorio y realización del primer commit.
     */
    PRIMER_COMMIT(null),

    /**
     * Configuración y validación de SonarCloud.
     */
    SONAR_ANALISIS(PRIMER_COMMIT),

    /**
     * Validación de la integración entre SonarCloud y el repositorio Git.
     */
    SONAR_INTEGRACION_GIT(SONAR_ANALISIS),

    /**
     * Configuración y validación del repositorio Git.
     */
    REPOSITORIO_GIT(SONAR_INTEGRACION_GIT),

    /**
     * Validación de la imagen Docker publicada en Amazon ECR.
     */
    IMAGEN_ECR(REPOSITORIO_GIT),

    /**
     * Configuración de la base de datos de la aplicación.
     */
    BASE_DATOS(IMAGEN_ECR),

    /**
     * Configuración del despliegue de la aplicación en una instancia EC2.
     */
    DESPLIEGUE_EC2(BASE_DATOS),

    /**
     * Paso final de resumen del asistente.
     *
     * Este paso no requiere validación propia, ya que su estado se calcula a partir
     * de los pasos anteriores.
     */
    RESUMEN_FINAL(DESPLIEGUE_EC2);

    private final PasoDespliegue pasoPrevio;

    PasoDespliegue(PasoDespliegue pasoPrevio) {
        this.pasoPrevio = pasoPrevio;
    }

    /**
     * Devuelvo el paso previo asociado al paso actual.
     *
     * Este método se utiliza para comprobar dependencias entre pasos y
     * garantizar que el asistente se ejecuta de forma secuencial.
     *
     * @return paso previo o {@code null} si no existe
     */
    public PasoDespliegue getPasoPrevio() {
        return pasoPrevio;
    }
}