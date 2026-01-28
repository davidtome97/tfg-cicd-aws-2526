package com.sistemagestionapp.service;

import com.sistemagestionapp.model.EstadoControl;
import com.sistemagestionapp.model.PasoDespliegue;
import com.sistemagestionapp.model.dto.ResultadoPaso;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

/**
 * En este servicio compruebo si una aplicación desplegada en una instancia EC2 es accesible por HTTP.
 *
 * Utilizo esta comprobación en el paso de despliegue para validar conectividad desde el exterior.
 * Para evitar bloqueos en la interfaz, configuro timeouts cortos y registro el resultado del paso
 * en la base de datos mediante {@link DeployWizardService}.
 *
 * @author David Tomé Arnaiz
 */
@Service
public class Ec2Service {

    private final RestTemplate restTemplate;
    private final DeployWizardService deployWizardService;

    /**
     * En este constructor inyecto el servicio del asistente y configuro el cliente HTTP con timeouts.
     *
     * @param deployWizardService servicio que utilizo para registrar el estado del paso
     */
    public Ec2Service(DeployWizardService deployWizardService) {
        this.deployWizardService = deployWizardService;

        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(3000);
        factory.setReadTimeout(3000);
        this.restTemplate = new RestTemplate(factory);
    }

    /**
     * Compruebo la accesibilidad HTTP de una aplicación en EC2.
     *
     * Normalizo el host eliminando posibles prefijos de protocolo y fuerzo un path válido. Si la instancia
     * responde con un código 2xx, considero la comprobación correcta. En caso de error de conectividad,
     * timeout o respuesta no satisfactoria, devuelvo KO.
     *
     * Independientemente del resultado, persisto el estado del paso para que el asistente recuerde el progreso.
     *
     * @param aplicacionId identificador de la aplicación
     * @param host dirección IP o DNS público de la EC2
     * @param port puerto expuesto por la aplicación
     * @param path ruta HTTP a consultar (si es nula o vacía, utilizo "/")
     * @return resultado de la comprobación en formato {@link ResultadoPaso}
     */
    public ResultadoPaso comprobarEc2(Long aplicacionId, String host, int port, String path) {
        ResultadoPaso resultado;

        if (host == null || host.isBlank()) {
            resultado = new ResultadoPaso("KO", "El host/IP está vacío.");
            persistir(aplicacionId, PasoDespliegue.DESPLIEGUE_EC2, resultado);
            return resultado;
        }

        String hostNormalizado = host.trim();
        if (hostNormalizado.startsWith("http://")) {
            hostNormalizado = hostNormalizado.substring("http://".length());
        } else if (hostNormalizado.startsWith("https://")) {
            hostNormalizado = hostNormalizado.substring("https://".length());
        }

        String pathNormalizado = (path == null || path.isBlank()) ? "/" : path.trim();
        if (!pathNormalizado.startsWith("/")) {
            pathNormalizado = "/" + pathNormalizado;
        }

        if (port <= 0) {
            resultado = new ResultadoPaso("KO", "El puerto debe ser mayor que 0.");
            persistir(aplicacionId, PasoDespliegue.DESPLIEGUE_EC2, resultado);
            return resultado;
        }

        String url = "http://" + hostNormalizado + ":" + port + pathNormalizado;

        try {
            ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);

            if (response.getStatusCode().is2xxSuccessful()) {
                resultado = new ResultadoPaso(
                        "OK",
                        "La instancia EC2 responde correctamente (" + response.getStatusCode().value() + ") en " + url
                );
            } else {
                resultado = new ResultadoPaso(
                        "KO",
                        "La instancia EC2 respondió con código " + response.getStatusCode().value() + " en " + url
                );
            }

        } catch (ResourceAccessException e) {
            resultado = new ResultadoPaso(
                    "KO",
                    "No se pudo conectar a " + url + ". Comprueba IP, puerto o que el puerto esté abierto."
            );

        } catch (Exception e) {
            resultado = new ResultadoPaso(
                    "KO",
                    "Error llamando a " + url + ": " + e.getMessage()
            );
        }

        persistir(aplicacionId, PasoDespliegue.DESPLIEGUE_EC2, resultado);
        return resultado;
    }

    /**
     * Persisto el resultado de un paso convirtiendo el estado textual (OK/KO) al enum {@link EstadoControl}.
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
