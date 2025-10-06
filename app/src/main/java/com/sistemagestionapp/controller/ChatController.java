package com.sistemagestionapp.controller;

import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Controlador para gestionar la funcionalidad del chat en tiempo real.
 * En esta clase definimos tanto la vista como el canal de comunicación WebSocket.
 *
 * @author David Tome Arnaiz
 */
@Controller
public class ChatController {

    /**
     * Este método se encarga de recibir mensajes del frontend enviados al canal "/app/mensaje".
     * Una vez recibido el mensaje, lo reenvía a todos los suscritos en "/topic/chat".
     *
     * @param mensaje El mensaje recibido del usuario.
     * @return El mismo mensaje que será enviado a los suscriptores.
     */
    @MessageMapping("/mensaje")
    @SendTo("/topic/chat")
    public String enviarMensaje(String mensaje) {
        System.out.println(" Mensaje recibido: " + mensaje); // Mostramos el mensaje por consola para depuración
        return mensaje;
    }

    /**
     * Este método muestra la plantilla del chat en tiempo real.
     * Cuando se accede a la ruta "/chat", se renderiza la vista "chat.html".
     *
     * @return El nombre de la plantilla HTML del chat.
     */
    @GetMapping("/chat")
    public String verChat() {
        return "chat";
    }
}