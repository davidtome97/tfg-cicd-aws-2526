package com.sistemagestionapp.service;

import com.sistemagestionapp.model.EstadoControl;
import com.sistemagestionapp.model.PasoDespliegue;
import com.sistemagestionapp.model.dto.ResultadoPaso;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.ecr.EcrClient;
import software.amazon.awssdk.services.ecr.model.*;

@Service
public class EcrService {

    private static final Region REGION = Region.EU_WEST_1;

    private final DeployWizardService deployWizardService;

    public EcrService(DeployWizardService deployWizardService) {
        this.deployWizardService = deployWizardService;
    }

    /**
     * Comprueba si existe una imagen NAME:TAG en ECR.
     */
    public ResultadoPaso comprobarImagen(Long aplicacionId, String repositoryName, String imageTag) {

        // Normalizar entrada
        repositoryName = (repositoryName == null) ? null : repositoryName.trim();
        imageTag = (imageTag == null) ? null : imageTag.trim();

        ResultadoPaso resultado;

        // Validación básica
        if (repositoryName == null || repositoryName.isBlank()
                || imageTag == null || imageTag.isBlank()) {

            resultado = new ResultadoPaso(
                    "KO",
                    "Repositorio o tag de imagen vacío."
            );
            persistir(aplicacionId, PasoDespliegue.IMAGEN_ECR, resultado);
            return resultado;
        }

        // 👉 CLAVE: crear el cliente POR PETICIÓN
        try (EcrClient ecrClient = EcrClient.builder()
                .region(REGION)
                .credentialsProvider(DefaultCredentialsProvider.create())
                .build()) {

            DescribeImagesRequest request = DescribeImagesRequest.builder()
                    .repositoryName(repositoryName)
                    .imageIds(
                            ImageIdentifier.builder()
                                    .imageTag(imageTag)
                                    .build()
                    )
                    .build();

            DescribeImagesResponse response = ecrClient.describeImages(request);

            if (response.imageDetails() == null || response.imageDetails().isEmpty()) {
                resultado = new ResultadoPaso(
                        "KO",
                        "No se encontró ninguna imagen con el tag '" + imageTag +
                                "' en el repositorio '" + repositoryName + "'."
                );
            } else {
                String digest = response.imageDetails().get(0).imageDigest();
                resultado = new ResultadoPaso(
                        "OK",
                        "Imagen encontrada en ECR. Digest: " + digest
                );
            }

        } catch (ImageNotFoundException e) {
            resultado = new ResultadoPaso(
                    "KO",
                    "La imagen '" + imageTag + "' no existe en el repositorio '" + repositoryName + "'."
            );

        } catch (RepositoryNotFoundException e) {
            resultado = new ResultadoPaso(
                    "KO",
                    "El repositorio ECR '" + repositoryName + "' no existe."
            );

        } catch (EcrException e) {
            String code = (e.awsErrorDetails() != null)
                    ? e.awsErrorDetails().errorCode()
                    : "ECR_ERROR";

            String msg = (e.awsErrorDetails() != null)
                    ? e.awsErrorDetails().errorMessage()
                    : e.getMessage();

            resultado = new ResultadoPaso(
                    "KO",
                    "Error de ECR (" + code + "): " + msg
            );

        } catch (Exception e) {
            resultado = new ResultadoPaso(
                    "KO",
                    "Error general accediendo a ECR: " + e.getMessage()
            );
        }

        persistir(aplicacionId, PasoDespliegue.IMAGEN_ECR, resultado);
        return resultado;
    }

    private void persistir(Long aplicacionId, PasoDespliegue paso, ResultadoPaso r) {
        if (aplicacionId == null) return;

        EstadoControl estado =
                "OK".equalsIgnoreCase(r.getEstado())
                        ? EstadoControl.OK
                        : EstadoControl.KO;

        deployWizardService.marcarPaso(
                aplicacionId,
                paso,
                estado,
                r.getMensaje()
        );
    }
}