package edu.uclm.es.gramola.http;

import java.util.concurrent.CopyOnWriteArrayList;
import java.io.IOException;
import java.util.List;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;
import org.springframework.stereotype.Component;

@Component
public class GramolaHandler extends TextWebSocketHandler {
    
    // Lista para guardar a todos los que están mirando la web ahora mismo
    private static List<WebSocketSession> sessions = new CopyOnWriteArrayList<>();

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        sessions.add(session);
        System.out.println("Nueva conexión WebSocket: " + session.getId());
    }

    // Método para enviar mensaje a TODOS
    public void broadcast(String message) {
        for (WebSocketSession session : sessions) {
            try {
                if (session.isOpen()) {
                    session.sendMessage(new TextMessage(message));
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}