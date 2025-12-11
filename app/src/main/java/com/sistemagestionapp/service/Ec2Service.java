package com.sistemagestionapp.service;

import com.sistemagestionapp.model.dto.ResultadoPaso;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

@Service
public class Ec2Service {

    private final RestTemplate restTemplate;

    public Ec2Service() {
        // RestTemplate con timeouts para que no se quede colgado
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(3000); // 3 segundos
        factory.setReadTimeout(3000);    // 3 segundos
        this.restTemplate = new RestTemplate(factory);
    }

    public ResultadoPaso comprobarEc2(String host, int port, String path) {

        if (host == null || host.isBlank()) {
            return new ResultadoPaso("KO", "El host/IP está vacío.");
        }

        // Quitar http:// o https:// si el usuario lo pone por error
        host = host.trim();
        if (host.startsWith("http://")) {
            host = host.substring("http://".length());
        } else if (host.startsWith("https://")) {
            host = host.substring("https://".length());
        }

        // Normalizar path
        if (path == null || path.isBlank()) {
            path = "/";
        }
        if (!path.startsWith("/")) {
            path = "/" + path;
        }

        String url = "http://" + host + ":" + port + path;

        try {
            ResponseEntity<String> response =
                    restTemplate.getForEntity(url, String.class);

            if (response.getStatusCode().is2xxSuccessful()) {
                return new ResultadoPaso(
                        "OK",
                        "La instancia EC2 responde correctamente ("
                                + response.getStatusCode().value() + ") en " + url
                );
            } else {
                return new ResultadoPaso(
                        "KO",
                        "La instancia EC2 respondió con código "
                                + response.getStatusCode().value() + " en " + url
                );
            }
        }
        // Errores de conexión / timeout
        catch (ResourceAccessException e) {
            return new ResultadoPaso(
                    "KO",
                    "No se pudo conectar a " + url
                            + ". Comprueba IP, puerto o que el puerto esté abierto."
            );
        }
        // Cualquier otro error
        catch (Exception e) {
            return new ResultadoPaso(
                    "KO",
                    "Error llamando a " + url + ": " + e.getMessage()
            );
        }
    }
}
