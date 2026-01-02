package com.sistemagestionapp.service;

import com.sistemagestionapp.model.EstadoControl;
import com.sistemagestionapp.model.PasoDespliegue;
import com.sistemagestionapp.model.dto.ResultadoPaso;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

@Service
public class Ec2Service {

    private final RestTemplate restTemplate;
    private final DeployWizardService deployWizardService;

    public Ec2Service(DeployWizardService deployWizardService) {
        this.deployWizardService = deployWizardService;

        // RestTemplate con timeouts para que no se quede colgado
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(3000); // 3 segundos
        factory.setReadTimeout(3000);    // 3 segundos
        this.restTemplate = new RestTemplate(factory);
    }

    public ResultadoPaso comprobarEc2(Long aplicacionId, String host, int port, String path) {

        ResultadoPaso resultado;

        // Validación host
        if (host == null || host.isBlank()) {
            resultado = new ResultadoPaso("KO", "El host/IP está vacío.");
            persistir(aplicacionId, PasoDespliegue.DESPLIEGUE_EC2, resultado);
            return resultado;
        }

        // Normalizar host
        host = host.trim();
        if (host.startsWith("http://")) host = host.substring("http://".length());
        else if (host.startsWith("https://")) host = host.substring("https://".length());

        // Normalizar path
        path = (path == null || path.isBlank()) ? "/" : path.trim();
        if (!path.startsWith("/")) path = "/" + path;

        // Normalizar puerto (opcional, por si llega 0 o negativo)
        if (port <= 0) {
            resultado = new ResultadoPaso("KO", "El puerto debe ser mayor que 0.");
            persistir(aplicacionId, PasoDespliegue.DESPLIEGUE_EC2, resultado);
            return resultado;
        }

        String url = "http://" + host + ":" + port + path;

        try {
            ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);

            if (response.getStatusCode().is2xxSuccessful()) {
                resultado = new ResultadoPaso(
                        "OK",
                        "La instancia EC2 responde correctamente ("
                                + response.getStatusCode().value() + ") en " + url
                );
            } else {
                resultado = new ResultadoPaso(
                        "KO",
                        "La instancia EC2 respondió con código "
                                + response.getStatusCode().value() + " en " + url
                );
            }

        } catch (ResourceAccessException e) {
            // Errores de conexión / timeout
            resultado = new ResultadoPaso(
                    "KO",
                    "No se pudo conectar a " + url
                            + ". Comprueba IP, puerto o que el puerto esté abierto."
            );

        } catch (Exception e) {
            resultado = new ResultadoPaso(
                    "KO",
                    "Error llamando a " + url + ": " + e.getMessage()
            );
        }

        // Persistimos una sola vez
        persistir(aplicacionId, PasoDespliegue.DESPLIEGUE_EC2, resultado);
        return resultado;
    }

    private void persistir(Long aplicacionId, PasoDespliegue paso, ResultadoPaso r) {
        if (aplicacionId == null) return;

        EstadoControl estado = "OK".equalsIgnoreCase(r.getEstado())
                ? EstadoControl.OK
                : EstadoControl.KO;

        deployWizardService.marcarPaso(aplicacionId, paso, estado, r.getMensaje());
    }
}
