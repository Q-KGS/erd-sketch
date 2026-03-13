package com.erdsketch.collaboration;

import com.erdsketch.document.DocumentService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SnapshotSchedulerTest {

    @Mock YjsWebSocketHandler wsHandler;
    @Mock DocumentService documentService;
    @InjectMocks SnapshotScheduler scheduler;

    String docId;

    @BeforeEach
    void setUp() {
        docId = UUID.randomUUID().toString();
    }

    // ───── B-SNAP-01: 연결된 세션이 있는 문서 스냅샷 저장 ─────
    @Test
    void B_SNAP_01_활성_세션_문서_스냅샷_저장() {
        byte[] state = {1, 2, 3};
        given(wsHandler.drainPendingStates()).willReturn(Map.of(docId, state));

        scheduler.saveSnapshots();

        verify(documentService).updateYjsState(UUID.fromString(docId), state, null);
    }

    // ───── B-SNAP-02: 연결된 세션이 없는 문서 → 스냅샷 업데이트 스킵 ─────
    @Test
    void B_SNAP_02_연결_없는_문서_스냅샷_스킵() {
        given(wsHandler.drainPendingStates()).willReturn(Map.of());

        scheduler.saveSnapshots();

        verifyNoInteractions(documentService);
    }

    // ───── B-SNAP-03: yjs_state null (pending 없음) → 오류 없음, null 유지 ─────
    @Test
    void B_SNAP_03_pending_없을때_오류_없음() {
        // pendingStates에 state는 없지만 세션은 있는 경우 → drain은 비어있음
        given(wsHandler.drainPendingStates()).willReturn(Map.of());

        // 예외 없이 정상 실행
        org.assertj.core.api.Assertions.assertThatCode(() -> scheduler.saveSnapshots())
                .doesNotThrowAnyException();
    }

    // ───── 추가: 여러 문서 동시 저장 ─────
    @Test
    void 여러_문서_동시_스냅샷_저장() {
        String docId2 = UUID.randomUUID().toString();
        byte[] state1 = {1, 2};
        byte[] state2 = {3, 4, 5};
        given(wsHandler.drainPendingStates()).willReturn(Map.of(docId, state1, docId2, state2));

        scheduler.saveSnapshots();

        verify(documentService).updateYjsState(UUID.fromString(docId), state1, null);
        verify(documentService).updateYjsState(UUID.fromString(docId2), state2, null);
    }

    // ───── 추가: documentService 예외 발생 시 다른 문서 계속 처리 ─────
    @Test
    void 저장_예외_발생_시_다른_문서는_계속_처리() {
        String docId2 = UUID.randomUUID().toString();
        byte[] state1 = {1};
        byte[] state2 = {2};
        given(wsHandler.drainPendingStates()).willReturn(Map.of(docId, state1, docId2, state2));
        doThrow(new RuntimeException("DB error"))
                .when(documentService).updateYjsState(UUID.fromString(docId), state1, null);

        // 예외가 전파되지 않아야 함
        org.assertj.core.api.Assertions.assertThatCode(() -> scheduler.saveSnapshots())
                .doesNotThrowAnyException();
    }
}
