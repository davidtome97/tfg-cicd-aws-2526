package com.sistemagestionapp.controller;

import com.sistemagestionapp.model.dto.ResultadoPaso;
import com.sistemagestionapp.service.EcrService;
import com.sistemagestionapp.service.Ec2Service;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controlador REST encargado de los pasos del asistente
 * relacionados con la infraestructura AWS.
 *
 * Desde aquí expongo endpoints que son llamados por AJAX
 * desde el asistente de despliegue para comprobar:
 * - La existencia de imágenes en Amazon ECR (Paso 4)
 * - La accesibilidad de una aplicación desplegada en EC2 (Paso 5)
 *
 * Devuelvo siempre un ResultadoPaso en formato JSON.
 */
@RestController
@RequestMapping("/api/wizard")
public class AwsController {

    private final EcrService ecrService;
    private final Ec2Service ec2Service;

    /**
     * Inyecto los servicios de ECR y EC2.
     * Toda la lógica de negocio se delega a estos servicios,
     * manteniendo el controlador lo más ligero posible.
     */
    public AwsController(EcrService ecrService, Ec2Service ec2Service) {
        this.ecrService = ecrService;
        this.ec2Service = ec2Service;
    }

    /**
     * Paso 4 del asistente.
     *
     * Compruebo que existe una imagen Docker en Amazon ECR
     * con el nombre de repositorio y el tag indicados.
     *
     * Este endpoint se llama desde el frontend cuando el usuario
     * pulsa el botón "Comprobar" en el paso 4 del asistente.
     *
     * @param appId          identificador de la aplicación
     * @param repositoryName nombre del repositorio en ECR
     * @param imageTag       tag de la imagen (por ejemplo: latest o v1.0.0)
     * @return ResultadoPaso con estado OK o KO y un mensaje explicativo
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
     * Paso 5 del asistente.
     *
     * Compruebo que la aplicación desplegada en una instancia EC2
     * es accesible desde el exterior.
     *
     * Realizo una comprobación de conectividad contra el host y puerto
     * indicados, normalmente accediendo a la ruta raíz ("/").
     *
     * Este paso valida que el despliegue en EC2 se ha realizado
     * correctamente y que la aplicación está en ejecución.
     *
     * @param appId identificador de la aplicación
     * @param host  dirección IP o DNS público de la EC2
     * @param port  puerto en el que escucha la aplicación
     * @return ResultadoPaso con estado OK o KO y mensaje de resultado
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