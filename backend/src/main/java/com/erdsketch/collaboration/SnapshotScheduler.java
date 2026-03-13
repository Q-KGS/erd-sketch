package com.erdsketch.collaboration;

import com.erdsketch.document.DocumentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.UUID;

@Component
@Slf4j
@RequiredArgsConstructor
public class SnapshotScheduler {

    private final YjsWebSocketHandler wsHandler;
    private final DocumentService documentService;

    /**
     * 30초마다 활성 WebSocket 연결이 있는 문서의 Yjs 상태를 DB에 저장한다.
     */
    @Scheduled(fixedRate = 30_000)
    public void saveSnapshots() {
        Map<String, byte[]> pending = wsHandler.drainPendingStates();

        if (pending.isEmpty()) {
            log.debug("Snapshot scheduler: no pending states to save");
            return;
        }

        log.debug("Snapshot scheduler: saving {} document(s)", pending.size());

        for (Map.Entry<String, byte[]> entry : pending.entrySet()) {
            try {
                UUID documentId = UUID.fromString(entry.getKey());
                documentService.updateYjsState(documentId, entry.getValue(), null);
                log.debug("Saved yjs_state for document {}, size={}B", entry.getKey(), entry.getValue().length);
            } catch (Exception e) {
                log.warn("Failed to save snapshot for document {}: {}", entry.getKey(), e.getMessage());
            }
        }
    }
}
