package com.sistemagestionapp.controller;

import com.sistemagestionapp.model.dto.ResultadoPaso;
import com.sistemagestionapp.service.EcrService;
import com.sistemagestionapp.service.Ec2Service;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/wizard")
public class AwsController {

    private final EcrService ecrService;
    private final Ec2Service ec2Service;

    public AwsController(EcrService ecrService, Ec2Service ec2Service) {
        this.ecrService = ecrService;
        this.ec2Service = ec2Service;
    }

    // Paso 4: comprobar imagen en ECR
    @GetMapping("/paso4")
    public ResultadoPaso comprobarEcr(
            @RequestParam String repositoryName,
            @RequestParam String imageTag
    ) {
        return ecrService.comprobarImagen(repositoryName, imageTag);
    }

    // Paso 5: comprobar health-check HTTP en EC2
    @GetMapping("/paso5")
    public ResultadoPaso comprobarPaso5(
            @RequestParam String host,
            @RequestParam int port
    ) {
        return ec2Service.comprobarEc2(host, port, "/");
    }
}
