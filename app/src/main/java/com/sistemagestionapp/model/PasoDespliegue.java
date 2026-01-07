package com.sistemagestionapp.model;

public enum PasoDespliegue {

    PRIMER_COMMIT(null),
    SONAR_ANALISIS(PRIMER_COMMIT),
    SONAR_INTEGRACION_GIT(SONAR_ANALISIS),
    REPOSITORIO_GIT(SONAR_INTEGRACION_GIT),
    IMAGEN_ECR(REPOSITORIO_GIT),
    BASE_DATOS(IMAGEN_ECR),
    DESPLIEGUE_EC2(BASE_DATOS),
    RESUMEN_FINAL(DESPLIEGUE_EC2);

    private final PasoDespliegue pasoPrevio;

    PasoDespliegue(PasoDespliegue pasoPrevio) {
        this.pasoPrevio = pasoPrevio;
    }

    public PasoDespliegue getPasoPrevio() {
        return pasoPrevio;
    }
}