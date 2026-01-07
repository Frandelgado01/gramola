package edu.uclm.es.gramola.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;
import edu.uclm.es.gramola.http.GramolaHandler;
import org.springframework.context.annotation.Bean;

@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        // Definimos la URL donde se conectarán los clientes: ws://localhost:8080/ws-gramola
        registry.addHandler(gramolaHandler(), "/ws-gramola")
                .setAllowedOrigins("*"); // Permitir conexión desde Angular
    }

    @Bean
    public GramolaHandler gramolaHandler() {
        return new GramolaHandler();
    }
}