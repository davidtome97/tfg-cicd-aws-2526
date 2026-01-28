package com.sistemagestionapp.service;

import com.sistemagestionapp.model.EstadoControl;
import com.sistemagestionapp.model.PasoDespliegue;
import com.sistemagestionapp.model.dto.ResultadoPaso;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.ecr.EcrClient;
import software.amazon.awssdk.services.ecr.model.DescribeImagesRequest;
import software.amazon.awssdk.services.ecr.model.DescribeImagesResponse;
import software.amazon.awssdk.services.ecr.model.EcrException;
import software.amazon.awssdk.services.ecr.model.ImageIdentifier;
import software.amazon.awssdk.services.ecr.model.ImageNotFoundException;
import software.amazon.awssdk.services.ecr.model.RepositoryNotFoundException;

/**
 * En este servicio compruebo si existe una imagen Docker en Amazon ECR a partir de un repositorio y un tag.
 *
 * Utilizo esta comprobación en el paso 4 del asistente para validar que el pipeline ha publicado la imagen
 * correctamente en ECR. Registro el resultado (OK/KO) en el asistente mediante {@link DeployWizardService}.
 *
 * @author David Tomé Arnaiz
 */
@Service
public class EcrService {

    /**
     * Defino la región de AWS utilizada para consultar ECR.
     *
     * Si el proyecto necesita soportar varias regiones, este valor debería obtenerse de configuración
     * o de los datos guardados para cada aplicación.
     */
    private static final Region REGION = Region.EU_WEST_1;

    private final DeployWizardService deployWizardService;

    /**
     * En este constructor inyecto el servicio del asistente para poder registrar el estado del paso.
     *
     * @param deployWizardService servicio que utilizo para persistir el resultado del paso
     */
    public EcrService(DeployWizardService deployWizardService) {
        this.deployWizardService = deployWizardService;
    }

    /**
     * Compruebo si existe una imagen en ECR con un repositorio y un tag concretos.
     *
     * Si la imagen existe, devuelvo OK e incluyo el digest en el mensaje. Si no existe, devuelvo KO con un
     * mensaje descriptivo. En cualquier caso, persisto el resultado asociado al paso 4 del asistente.
     *
     * @param aplicacionId identificador de la aplicación
     * @param repositoryName nombre del repositorio en ECR
     * @param imageTag tag de la imagen (por ejemplo: latest o v1.0.0)
     * @return resultado de la comprobación en formato {@link ResultadoPaso}
     */
    public ResultadoPaso comprobarImagen(Long aplicacionId, String repositoryName, String imageTag) {
        String repo = (repositoryName == null) ? null : repositoryName.trim();
        String tag = (imageTag == null) ? null : imageTag.trim();

        ResultadoPaso resultado;

        if (repo == null || repo.isBlank() || tag == null || tag.isBlank()) {
            resultado = new ResultadoPaso("KO", "Repositorio o tag de imagen vacío.");
            persistir(aplicacionId, PasoDespliegue.IMAGEN_ECR, resultado);
            return resultado;
        }

        try (EcrClient ecrClient = EcrClient.builder()
                .region(REGION)
                .credentialsProvider(DefaultCredentialsProvider.create())
                .build()) {

            DescribeImagesRequest request = DescribeImagesRequest.builder()
                    .repositoryName(repo)
                    .imageIds(
                            ImageIdentifier.builder()
                                    .imageTag(tag)
                                    .build()
                    )
                    .build();

            DescribeImagesResponse response = ecrClient.describeImages(request);

            if (response.imageDetails() == null || response.imageDetails().isEmpty()) {
                resultado = new ResultadoPaso(
                        "KO",
                        "No se encontró ninguna imagen con el tag '" + tag + "' en el repositorio '" + repo + "'."
                );
            } else {
                String digest = response.imageDetails().get(0).imageDigest();
                resultado = new ResultadoPaso("OK", "Imagen encontrada en ECR. Digest: " + digest);
            }

        } catch (ImageNotFoundException e) {
            resultado = new ResultadoPaso(
                    "KO",
                    "La imagen '" + tag + "' no existe en el repositorio '" + repo + "'."
            );

        } catch (RepositoryNotFoundException e) {
            resultado = new ResultadoPaso(
                    "KO",
                    "El repositorio ECR '" + repo + "' no existe."
            );

        } catch (EcrException e) {
            String code = (e.awsErrorDetails() != null)
                    ? e.awsErrorDetails().errorCode()
                    : "ECR_ERROR";

            String msg = (e.awsErrorDetails() != null)
                    ? e.awsErrorDetails().errorMessage()
                    : e.getMessage();

            resultado = new ResultadoPaso("KO", "Error de ECR (" + code + "): " + msg);

        } catch (Exception e) {
            resultado = new ResultadoPaso("KO", "Error general accediendo a ECR: " + e.getMessage());
        }

        persistir(aplicacionId, PasoDespliegue.IMAGEN_ECR, resultado);
        return resultado;
    }

    /**
     * Persisto el resultado del paso convirtiendo el estado textual (OK/KO) al enum {@link EstadoControl}.
     *
     * @param aplicacionId identificador de la aplicación
     * @param paso paso del asistente a registrar
     * @param r resultado obtenido en la comprobación
     */
    private void persistir(Long aplicacionId, PasoDespliegue paso, ResultadoPaso r) {
        if (aplicacionId == null) {
            return;
        }

        EstadoControl estado = "OK".equalsIgnoreCase(r.getEstado())
                ? EstadoControl.OK
                : EstadoControl.KO;

        deployWizardService.marcarPaso(aplicacionId, paso, estado, r.getMensaje());
    }
}