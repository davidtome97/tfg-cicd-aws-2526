package com.sistemagestionapp.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.*;

/**
 * Esta clase configura el soporte de WebSocket para el chat en tiempo real dentro de mi proyecto.
 * Gracias a esta configuración, los clientes pueden enviar y recibir mensajes usando STOMP sobre WebSocket.
 *
 * Utilizo la anotación {@code @EnableWebSocketMessageBroker} para activar la mensajería basada en broker,
 * que es ideal para chats y otras funciones de comunicación en tiempo real.
 *
 * En este archivo defino dos cosas importantes:
 * 1. El prefijo "/app" que utilizará el frontend para enviar mensajes al backend.
 * 2. El canal "/topic" que usaremos para emitir los mensajes a todos los clientes suscritos.
 *
 * Además, defino el endpoint "/chat-websocket" con soporte para SockJS, que mejora la compatibilidad
 * con navegadores que no soportan WebSocket directamente.
 *
 * Esta configuración permite que el chat en vivo funcione correctamente.
 *
 * @author David Tomé Arnaiz
 */
@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    /**
     * Aquí defino cómo va a funcionar el broker de mensajes.
     * El broker se encarga de gestionar el envío de mensajes a todos los usuarios conectados.
     *
     * @param config objeto que me permite establecer configuraciones del broker.
     */
    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        // Este prefijo indica que todos los mensajes enviados desde el backend a los clientes van a "/topic"
        config.enableSimpleBroker("/topic");

        // Este prefijo indica que todos los mensajes enviados desde el frontend al backend empiezan por "/app"
        config.setApplicationDestinationPrefixes("/app");
    }

    /**
     * En este método registro el endpoint que se usará para establecer la conexión WebSocket.
     * Uso SockJS para asegurar compatibilidad con navegadores que no soportan WebSocket.
     *
     * @param registry objeto donde registro los endpoints disponibles para WebSocket.
     */
    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/chat-websocket")
                .setAllowedOriginPatterns("*") // Permitimos todas las URLs
                .withSockJS(); // Activamos SockJS por compatibilidad
    }
}