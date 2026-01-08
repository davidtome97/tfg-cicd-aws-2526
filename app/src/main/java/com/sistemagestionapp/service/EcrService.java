package com.sistemagestionapp.service;

import com.sistemagestionapp.model.EstadoControl;
import com.sistemagestionapp.model.PasoDespliegue;
import com.sistemagestionapp.model.dto.ResultadoPaso;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.ecr.EcrClient;
import software.amazon.awssdk.services.ecr.model.*;

/**
 * En este servicio compruebo si existe una imagen Docker en Amazon ECR con un repositorio y un tag concretos.
 * Lo uso en el asistente (paso 4) para validar que el pipeline ha subido la imagen correctamente a ECR.
 */
@Service
public class EcrService {

    // Aquí fijo la región donde tengo el ECR (en mi caso eu-west-1).
    // Si trabajara en otra región, tendría que cambiarlo o leerlo de configuración.
    private static final Region REGION = Region.EU_WEST_1;

    // Este servicio lo uso para guardar el estado del paso (OK/KO) y su mensaje.
    private final DeployWizardService deployWizardService;

    public EcrService(DeployWizardService deployWizardService) {
        this.deployWizardService = deployWizardService;
    }

    /**
     * En este método compruebo si existe una imagen NAME:TAG en ECR.
     * Si existe, devuelvo OK y muestro el digest. Si no existe, devuelvo KO con un mensaje claro.
     */
    public ResultadoPaso comprobarImagen(Long aplicacionId, String repositoryName, String imageTag) {

        // Normalizo lo que introduce el usuario para evitar errores por espacios.
        repositoryName = (repositoryName == null) ? null : repositoryName.trim();
        imageTag = (imageTag == null) ? null : imageTag.trim();

        ResultadoPaso resultado;

        // Valido que me hayan pasado repositorio y tag, porque sin eso no puedo consultar ECR.
        if (repositoryName == null || repositoryName.isBlank()
                || imageTag == null || imageTag.isBlank()) {

            resultado = new ResultadoPaso(
                    "KO",
                    "Repositorio o tag de imagen vacío."
            );
            persistir(aplicacionId, PasoDespliegue.IMAGEN_ECR, resultado);
            return resultado;
        }

        // Creo el cliente de ECR por petición y lo cierro al terminar.
        // Así evito problemas de recursos y me aseguro de usar credenciales actualizadas.
        try (EcrClient ecrClient = EcrClient.builder()
                .region(REGION)
                .credentialsProvider(DefaultCredentialsProvider.create())
                .build()) {

            // Construyo la petición para pedir a ECR el detalle de una imagen por su tag.
            DescribeImagesRequest request = DescribeImagesRequest.builder()
                    .repositoryName(repositoryName)
                    .imageIds(
                            ImageIdentifier.builder()
                                    .imageTag(imageTag)
                                    .build()
                    )
                    .build();

            // Ejecuto la consulta a ECR.
            DescribeImagesResponse response = ecrClient.describeImages(request);

            // Si no devuelve detalles, considero que no existe esa imagen con ese tag.
            if (response.imageDetails() == null || response.imageDetails().isEmpty()) {
                resultado = new ResultadoPaso(
                        "KO",
                        "No se encontró ninguna imagen con el tag '" + imageTag +
                                "' en el repositorio '" + repositoryName + "'."
                );
            } else {
                // Si existe, cojo el digest (identificador único de la imagen) y lo muestro en el mensaje.
                String digest = response.imageDetails().get(0).imageDigest();
                resultado = new ResultadoPaso(
                        "OK",
                        "Imagen encontrada en ECR. Digest: " + digest
                );
            }

        } catch (ImageNotFoundException e) {
            // Este caso ocurre cuando el repositorio existe pero el tag solicitado no está.
            resultado = new ResultadoPaso(
                    "KO",
                    "La imagen '" + imageTag + "' no existe en el repositorio '" + repositoryName + "'."
            );

        } catch (RepositoryNotFoundException e) {
            // Este caso ocurre cuando el repositorio indicado no existe en ECR.
            resultado = new ResultadoPaso(
                    "KO",
                    "El repositorio ECR '" + repositoryName + "' no existe."
            );

        } catch (EcrException e) {
            // Aquí capturo errores típicos de AWS: permisos, credenciales, región incorrecta, etc.
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
            // Capturo cualquier otro error inesperado para no romper el asistente.
            resultado = new ResultadoPaso(
                    "KO",
                    "Error general accediendo a ECR: " + e.getMessage()
            );
        }

        // Guardo el resultado para que quede registrado el estado del paso 4.
        persistir(aplicacionId, PasoDespliegue.IMAGEN_ECR, resultado);
        return resultado;
    }

    /**
     * En este método transformo el ResultadoPaso (OK/KO) al enum EstadoControl y lo guardo en base de datos.
     * Así el asistente puede bloquear/desbloquear el acceso a los pasos.
     */
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