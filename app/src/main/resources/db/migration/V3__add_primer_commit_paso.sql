ALTER TABLE control_despliegue
DROP CONSTRAINT IF EXISTS control_despliegue_paso_check;

ALTER TABLE control_despliegue
    ADD CONSTRAINT control_despliegue_paso_check
        CHECK (paso IN (
                        'PRIMER_COMMIT',
                        'SONAR_ANALISIS',
                        'SONAR_INTEGRACION_GIT',
                        'REPOSITORIO_GIT',
                        'IMAGEN_ECR',
                        'DESPLIEGUE_EC2',
                        'BASE_DATOS',
                        'RESUMEN_FINAL'
            ));