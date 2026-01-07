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
 * En este servicio compruebo si una aplicación desplegada en una EC2 responde por HTTP.
 * Lo uso en el paso de despliegue para validar que la instancia está accesible desde fuera.
 */
@Service
public class Ec2Service {

    // Uso RestTemplate para hacer la llamada HTTP de comprobación.
    // Le configuro timeouts para que el asistente no se quede esperando indefinidamente.
    private final RestTemplate restTemplate;

    // Este servicio lo uso para guardar el estado del paso (OK/KO) y su mensaje en base de datos.
    private final DeployWizardService deployWizardService;

    public Ec2Service(DeployWizardService deployWizardService) {
        this.deployWizardService = deployWizardService;

        // Configuro timeouts de conexión y lectura para que la comprobación sea rápida.
        // Si no responde en pocos segundos, lo considero KO y continúo.
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(3000);
        factory.setReadTimeout(3000);
        this.restTemplate = new RestTemplate(factory);
    }

    /**
     * En este método compruebo si el host:puerto responde con un 2xx.
     * Normalizo host y path para construir una URL válida y guardo el resultado del paso.
     */
    public ResultadoPaso comprobarEc2(Long aplicacionId, String host, int port, String path) {

        ResultadoPaso resultado;

        // Primero valido que el usuario me haya dado un host/IP con contenido.
        if (host == null || host.isBlank()) {
            resultado = new ResultadoPaso("KO", "El host/IP está vacío.");
            persistir(aplicacionId, PasoDespliegue.DESPLIEGUE_EC2, resultado);
            return resultado;
        }

        // Limpio espacios y elimino el protocolo si el usuario lo ha escrito (http:// o https://),
        // porque yo lo vuelvo a componer de forma controlada al construir la URL.
        host = host.trim();
        if (host.startsWith("http://")) host = host.substring("http://".length());
        else if (host.startsWith("https://")) host = host.substring("https://".length());

        // Si no me pasan path, uso "/" por defecto y fuerzo que empiece por "/".
        path = (path == null || path.isBlank()) ? "/" : path.trim();
        if (!path.startsWith("/")) path = "/" + path;

        // También valido que el puerto sea correcto (evito 0 o negativos).
        if (port <= 0) {
            resultado = new ResultadoPaso("KO", "El puerto debe ser mayor que 0.");
            persistir(aplicacionId, PasoDespliegue.DESPLIEGUE_EC2, resultado);
            return resultado;
        }

        // Construyo la URL final que voy a comprobar.
        String url = "http://" + host + ":" + port + path;

        try {
            // Lanzo una petición GET sencilla para validar accesibilidad.
            ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);

            // Si devuelve un 2xx, considero que el despliegue está accesible por HTTP.
            if (response.getStatusCode().is2xxSuccessful()) {
                resultado = new ResultadoPaso(
                        "OK",
                        "La instancia EC2 responde correctamente ("
                                + response.getStatusCode().value() + ") en " + url
                );
            } else {
                // Si responde pero con un código no 2xx, lo considero KO (por ejemplo 404, 500...).
                resultado = new ResultadoPaso(
                        "KO",
                        "La instancia EC2 respondió con código "
                                + response.getStatusCode().value() + " en " + url
                );
            }

        } catch (ResourceAccessException e) {
            // Aquí suelo caer cuando hay timeout, DNS, conexión rechazada o puerto cerrado.
            // Es el caso típico cuando el Security Group no deja entrar al puerto o el contenedor no está levantado.
            resultado = new ResultadoPaso(
                    "KO",
                    "No se pudo conectar a " + url
                            + ". Comprueba IP, puerto o que el puerto esté abierto."
            );

        } catch (Exception e) {
            // Capturo cualquier otro error inesperado y lo devuelvo como KO con el mensaje.
            resultado = new ResultadoPaso(
                    "KO",
                    "Error llamando a " + url + ": " + e.getMessage()
            );
        }

        // Guardo el resultado (OK o KO) para que el asistente recuerde el estado del paso.
        persistir(aplicacionId, PasoDespliegue.DESPLIEGUE_EC2, resultado);
        return resultado;
    }

    /**
     * En este método traduzco el resultado del paso (OK/KO) al enum EstadoControl y lo persisto.
     * Así consigo que el estado del asistente quede guardado y pueda bloquear/desbloquear pasos.
     */
    private void persistir(Long aplicacionId, PasoDespliegue paso, ResultadoPaso r) {
        if (aplicacionId == null) return;

        EstadoControl estado = "OK".equalsIgnoreCase(r.getEstado())
                ? EstadoControl.OK
                : EstadoControl.KO;

        deployWizardService.marcarPaso(aplicacionId, paso, estado, r.getMensaje());
    }
}
