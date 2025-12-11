package com.sistemagestionapp.service;

import com.sistemagestionapp.model.dto.ResultadoPaso;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.ecr.EcrClient;
import software.amazon.awssdk.services.ecr.model.*;

@Service
public class EcrService {

    // Tu región de ECR: eu-west-1 (Irlanda)
    private static final Region REGION = Region.EU_WEST_1;

    private final EcrClient ecrClient;

    public EcrService() {
        this.ecrClient = EcrClient.builder()
                .region(REGION)
                .credentialsProvider(DefaultCredentialsProvider.create())
                .build();
    }

    /**
     * Comprueba si existe una imagen NAME:TAG en ECR.
     *
     * @param repositoryName nombre del repo ECR (ej: davidtome97/tfg-cicd-aws-2526)
     * @param imageTag       tag de la imagen (ej: latest)
     */
    public ResultadoPaso comprobarImagen(String repositoryName, String imageTag) {
        try {
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
                return new ResultadoPaso(
                        "KO",
                        "No se encontró ninguna imagen con el tag '" + imageTag +
                                "' en el repositorio '" + repositoryName + "'."
                );
            }

            String digest = response.imageDetails().get(0).imageDigest();
            return new ResultadoPaso(
                    "OK",
                    "Imagen encontrada en ECR. Digest: " + digest
            );
        }
        catch (ImageNotFoundException e) {
            return new ResultadoPaso(
                    "KO",
                    "La imagen '" + imageTag + "' no existe en el repositorio '" + repositoryName + "'."
            );
        }
        catch (RepositoryNotFoundException e) {
            return new ResultadoPaso(
                    "KO",
                    "El repositorio ECR '" + repositoryName + "' no existe."
            );
        }
        catch (EcrException e) {
            return new ResultadoPaso(
                    "KO",
                    "Error de ECR (" +
                            e.awsErrorDetails().errorCode() + "): " +
                            e.awsErrorDetails().errorMessage()
            );
        }
        catch (Exception e) {
            return new ResultadoPaso(
                    "KO",
                    "Error general accediendo a ECR: " + e.getMessage()
            );
        }
    }
}
