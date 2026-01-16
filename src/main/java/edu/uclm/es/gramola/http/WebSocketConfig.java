package edu.uclm.es.gramola.http;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {

    @Autowired
    private GramolaHandler gramolaHandler;

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        // Esta es la URL a la que se conecta tu Angular: ws://localhost:8080/ws
        registry.addHandler(gramolaHandler, "/ws")
                .setAllowedOrigins("*"); // Permite conexión desde cualquier sitio
    }
}