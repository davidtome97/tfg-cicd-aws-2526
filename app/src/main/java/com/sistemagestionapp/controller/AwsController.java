package com.sistemagestionapp.controller;

import com.sistemagestionapp.model.dto.ResultadoPaso;
import com.sistemagestionapp.service.EcrService;
import com.sistemagestionapp.service.Ec2Service;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/wizard")
public class AwsController {

    private final EcrService ecrService;
    private final Ec2Service ec2Service;

    public AwsController(EcrService ecrService, Ec2Service ec2Service) {
        this.ecrService = ecrService;
        this.ec2Service = ec2Service;
    }

    @GetMapping("/paso4")
    public ResultadoPaso comprobarEcr(
            @RequestParam Long appId,
            @RequestParam String repositoryName,
            @RequestParam String imageTag
    ) {
        return ecrService.comprobarImagen(appId, repositoryName, imageTag);
    }

    @GetMapping("/paso5")
    public ResultadoPaso comprobarPaso5(
            @RequestParam Long appId,
            @RequestParam String host,
            @RequestParam int port
    ) {
        return ec2Service.comprobarEc2(appId, host, port, "/");
    }
}