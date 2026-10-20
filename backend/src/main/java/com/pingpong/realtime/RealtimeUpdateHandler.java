package com.pingpong.realtime;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class RealtimeUpdateHandler extends TextWebSocketHandler {

    private final Set<WebSocketSession> sessions = ConcurrentHashMap.newKeySet();

    @Autowired
    private ObjectMapper objectMapper;

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        sessions.add(session);
        sendMessage(session, RealtimeEventType.CONNECTED, Collections.singletonMap("message", "connected"));
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        sessions.remove(session);
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {
        sessions.remove(session);
        if (session.isOpen()) {
            session.close(CloseStatus.SERVER_ERROR);
        }
    }

    public void broadcast(String type, Map<String, Object> payload) {
        for (WebSocketSession session : sessions) {
            if (!session.isOpen()) {
                sessions.remove(session);
                continue;
            }

            try {
                sendMessage(session, type, payload);
            } catch (IOException ignored) {
                sessions.remove(session);
            }
        }
    }

    private void sendMessage(WebSocketSession session, String type, Map<String, Object> payload) throws IOException {
        RealtimeEventMessage message = new RealtimeEventMessage();
        message.setType(type);
        message.setTimestamp(LocalDateTime.now());
        message.setPayload(payload == null ? Collections.emptyMap() : payload);
        session.sendMessage(new TextMessage(objectMapper.writeValueAsString(message)));
    }
}
