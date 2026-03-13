package com.erdsketch.collaboration;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.*;
import org.springframework.web.socket.handler.BinaryWebSocketHandler;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Component
@Slf4j
@RequiredArgsConstructor
public class YjsWebSocketHandler extends BinaryWebSocketHandler {

    // documentId -> set of sessions
    private final Map<String, Set<WebSocketSession>> rooms = new ConcurrentHashMap<>();

    // documentId -> accumulated Yjs binary updates (for snapshot saving)
    private final Map<String, byte[]> pendingStates = new ConcurrentHashMap<>();

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        String documentId = extractDocumentId(session);
        if (documentId == null) return;
        rooms.computeIfAbsent(documentId, k -> ConcurrentHashMap.newKeySet()).add(session);
        log.debug("WebSocket connected: session={}, document={}, room size={}",
                  session.getId(), documentId, rooms.get(documentId).size());
    }

    @Override
    protected void handleBinaryMessage(WebSocketSession session, BinaryMessage message) {
        String documentId = extractDocumentId(session);
        if (documentId == null) return;
        Set<WebSocketSession> room = rooms.get(documentId);
        if (room == null) return;

        // Accumulate binary state for periodic snapshot
        byte[] payload = message.getPayload().array();
        pendingStates.merge(documentId, payload, (existing, incoming) -> {
            byte[] merged = new byte[existing.length + incoming.length];
            System.arraycopy(existing, 0, merged, 0, existing.length);
            System.arraycopy(incoming, 0, merged, existing.length, incoming.length);
            return merged;
        });

        // Broadcast to all other sessions in the same room
        for (WebSocketSession other : room) {
            if (other.isOpen() && !other.getId().equals(session.getId())) {
                try {
                    other.sendMessage(message);
                } catch (IOException e) {
                    log.warn("Failed to relay message to session {}: {}", other.getId(), e.getMessage());
                }
            }
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        String documentId = extractDocumentId(session);
        if (documentId == null) return;
        Set<WebSocketSession> room = rooms.get(documentId);
        if (room != null) {
            room.remove(session);
            if (room.isEmpty()) rooms.remove(documentId);
        }
        log.debug("WebSocket disconnected: session={}, document={}", session.getId(), documentId);
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) {
        log.warn("WebSocket transport error: session={}, error={}", session.getId(), exception.getMessage());
    }

    @Override
    public boolean supportsPartialMessages() {
        return false;
    }

    private String extractDocumentId(WebSocketSession session) {
        String path = session.getUri() != null ? session.getUri().getPath() : null;
        if (path == null) return null;
        // Path: /ws/documents/{documentId}
        String[] parts = path.split("/");
        return parts.length >= 4 ? parts[3] : null;
    }

    public int getRoomSize(String documentId) {
        Set<WebSocketSession> room = rooms.get(documentId);
        return room != null ? room.size() : 0;
    }

    /**
     * 현재 활성 연결이 있는 문서들의 누적 Yjs 상태를 반환하고 초기화한다.
     * SnapshotScheduler가 주기적으로 호출한다.
     */
    public Map<String, byte[]> drainPendingStates() {
        Map<String, byte[]> result = new java.util.HashMap<>();
        for (String documentId : rooms.keySet()) {
            byte[] state = pendingStates.remove(documentId);
            if (state != null) {
                result.put(documentId, state);
            }
        }
        return result;
    }

    public Map<String, Set<WebSocketSession>> getRooms() {
        return rooms;
    }
}
