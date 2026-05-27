package com.sistemagestionapp.controller;

import com.sistemagestionapp.model.dto.ResultadoPaso;
import com.sistemagestionapp.service.EcrService;
import com.sistemagestionapp.service.Ec2Service;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * En este controlador REST expongo endpoints del asistente de despliegue relacionados con infraestructura AWS.
 *
 * Me utilizan desde el frontend mediante llamadas AJAX para validar:
 * - la existencia de imágenes en Amazon ECR,
 * - y la accesibilidad de una aplicación desplegada en una instancia EC2.
 *
 * Devuelvo siempre un {@link ResultadoPaso} en formato JSON para informar del estado del paso.
 *
 * @author David Tomé Arnaiz
 */
@RestController
@RequestMapping("/api/wizard")
public class AwsController {

    private final EcrService ecrService;
    private final Ec2Service ec2Service;

    /**
     * En este constructor inyecto los servicios necesarios para trabajar con ECR y EC2.
     *
     * Mantengo el controlador centrado en la capa de presentación y delego la lógica de negocio en la capa de servicio.
     *
     * @param ecrService servicio con la lógica de validación relacionada con ECR
     * @param ec2Service servicio con la lógica de validación relacionada con EC2
     * @author David Tomé Arnaiz
     */
    public AwsController(EcrService ecrService, Ec2Service ec2Service) {
        this.ecrService = ecrService;
        this.ec2Service = ec2Service;
    }

    /**
     * En este endpoint valido el paso asociado a ECR comprobando la existencia de una imagen Docker.
     *
     * Verifico que, para el repositorio y tag indicados, existe una imagen disponible en Amazon ECR.
     *
     * @param appId identificador de la aplicación
     * @param repositoryName nombre del repositorio en ECR
     * @param imageTag tag de la imagen (por ejemplo, latest o v1.0.0)
     * @return resultado de la comprobación del paso en formato JSON
     * @author David Tomé Arnaiz
     */
    @GetMapping("/paso4")
    public ResultadoPaso comprobarEcr(
            @RequestParam Long appId,
            @RequestParam String repositoryName,
            @RequestParam String imageTag
    ) {
        return ecrService.comprobarImagen(appId, repositoryName, imageTag);
    }

    /**
     * En este endpoint valido el paso asociado a EC2 comprobando la accesibilidad externa de la aplicación desplegada.
     *
     * Realizo la comprobación contra el host y el puerto indicados, normalmente consultando la ruta raíz ("/").
     *
     * @param appId identificador de la aplicación
     * @param host dirección IP o DNS público de la instancia EC2
     * @param port puerto en el que escucha la aplicación
     * @return resultado de la comprobación del paso en formato JSON
     * @author David Tomé Arnaiz
     */
    @GetMapping("/paso5")
    public ResultadoPaso comprobarPaso5(
            @RequestParam Long appId,
            @RequestParam String host,
            @RequestParam int port
    ) {
        return ec2Service.comprobarEc2(appId, host, port, "/");
    }
}