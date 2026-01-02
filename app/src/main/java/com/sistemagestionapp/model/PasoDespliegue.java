package com.sistemagestionapp.model;

public enum PasoDespliegue {
    PRIMER_COMMIT,
    SONAR_ANALISIS,
    SONAR_INTEGRACION_GIT,
    REPOSITORIO_GIT,
    IMAGEN_ECR,
    DESPLIEGUE_EC2,
    BASE_DATOS,
    RESUMEN_FINAL
}