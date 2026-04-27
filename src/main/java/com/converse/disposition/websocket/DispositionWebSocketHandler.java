package com.converse.disposition.websocket;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.converse.disposition.model.DispositionCard;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
public class DispositionWebSocketHandler extends TextWebSocketHandler {

    private final ConcurrentHashMap<String, WebSocketSession> agentSessions = new ConcurrentHashMap<>();
    private final ObjectMapper objectMapper;

    public DispositionWebSocketHandler(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        String agentId = session.getHandshakeHeaders().getFirst("X-Agent-Id");
        if (agentId == null || agentId.isBlank()) {
            log.warn("WebSocket connection rejected: missing X-Agent-Id header");
            session.close(CloseStatus.POLICY_VIOLATION);
            return;
        }
        agentSessions.put(agentId, session);
        log.info("WebSocket connected: agentId={}", agentId);
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        String agentId = session.getHandshakeHeaders().getFirst("X-Agent-Id");
        if (agentId != null) {
            agentSessions.remove(agentId);
            log.info("WebSocket disconnected: agentId={}", agentId);
        }
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) {
        String agentId = session.getHandshakeHeaders().getFirst("X-Agent-Id");
        log.warn("WebSocket transport error: agentId={}", agentId, exception);
        if (agentId != null) {
            agentSessions.remove(agentId);
        }
    }

    public boolean pushToAgent(String agentId, DispositionCard card) {
        WebSocketSession session = agentSessions.get(agentId);
        if (session == null) {
            log.info("Agent {} is offline — card persisted for later retrieval", agentId);
            return false;
        }
        if (!session.isOpen()) {
            agentSessions.remove(agentId);
            log.info("Stale WebSocket session removed for agentId={}", agentId);
            return false;
        }
        try {
            Map<String, Object> envelope = Map.of(
                    "type", "DISPOSITION_READY",
                    "sessionId", card.sessionId(),
                    "payload", card
            );
            String json = objectMapper.writeValueAsString(envelope);
            session.sendMessage(new TextMessage(json));
            log.info("Disposition pushed via WebSocket to agentId={}, sessionId={}", agentId, card.sessionId());
            return true;
        } catch (Exception e) {
            log.warn("WebSocket push failed for agentId={}, sessionId={}", agentId, card.sessionId(), e);
            return false;
        }
    }
}
