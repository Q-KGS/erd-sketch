package com.erdsketch.collaboration;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.socket.BinaryMessage;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.WebSocketSession;

import java.io.IOException;
import java.net.URI;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class YjsWebSocketHandlerTest {

    YjsWebSocketHandler handler;

    @BeforeEach
    void setUp() {
        handler = new YjsWebSocketHandler();
    }

    WebSocketSession mockSession(String sessionId, String documentId) throws Exception {
        WebSocketSession session = mock(WebSocketSession.class);
        when(session.getId()).thenReturn(sessionId);
        when(session.isOpen()).thenReturn(true);
        when(session.getUri()).thenReturn(new URI("/ws/documents/" + documentId));
        return session;
    }

    WebSocketSession mockClosedSession(String sessionId, String documentId) throws Exception {
        WebSocketSession session = mockSession(sessionId, documentId);
        when(session.isOpen()).thenReturn(false);
        return session;
    }

    // ───── B-WS-01: 세션 연결 시 rooms에 추가 ─────
    @Test
    void B_WS_01_세션_연결_시_rooms에_추가() throws Exception {
        WebSocketSession s1 = mockSession("s1", "doc-1");
        handler.afterConnectionEstablished(s1);

        assertThat(handler.getRoomSize("doc-1")).isEqualTo(1);
    }

    // ───── B-WS-02: 바이너리 메시지 수신 시 같은 방 다른 세션에 브로드캐스트 ─────
    @Test
    void B_WS_02_메시지_수신_시_같은_방_다른_세션에_브로드캐스트() throws Exception {
        WebSocketSession sender = mockSession("s1", "doc-1");
        WebSocketSession receiver = mockSession("s2", "doc-1");
        handler.afterConnectionEstablished(sender);
        handler.afterConnectionEstablished(receiver);

        BinaryMessage msg = new BinaryMessage(new byte[]{1, 2, 3});
        handler.handleBinaryMessage(sender, msg);

        verify(receiver).sendMessage(msg);
    }

    // ───── B-WS-03: 발신자에게는 반송 안 함 ─────
    @Test
    void B_WS_03_발신자에게는_반송_안_함() throws Exception {
        WebSocketSession sender = mockSession("s1", "doc-1");
        WebSocketSession receiver = mockSession("s2", "doc-1");
        handler.afterConnectionEstablished(sender);
        handler.afterConnectionEstablished(receiver);

        BinaryMessage msg = new BinaryMessage(new byte[]{1, 2, 3});
        handler.handleBinaryMessage(sender, msg);

        verify(sender, never()).sendMessage(any());
    }

    // ───── B-WS-04: 세션 연결 해제 시 rooms에서 제거 ─────
    @Test
    void B_WS_04_세션_연결_해제_시_rooms에서_제거() throws Exception {
        WebSocketSession s1 = mockSession("s1", "doc-1");
        WebSocketSession s2 = mockSession("s2", "doc-1");
        handler.afterConnectionEstablished(s1);
        handler.afterConnectionEstablished(s2);

        handler.afterConnectionClosed(s1, CloseStatus.NORMAL);

        assertThat(handler.getRoomSize("doc-1")).isEqualTo(1);
    }

    // ───── B-WS-05: 마지막 세션 해제 시 rooms에서 documentId 키 제거 ─────
    @Test
    void B_WS_05_마지막_세션_해제_시_room_키_제거() throws Exception {
        WebSocketSession s1 = mockSession("s1", "doc-1");
        handler.afterConnectionEstablished(s1);
        handler.afterConnectionClosed(s1, CloseStatus.NORMAL);

        assertThat(handler.getRoomSize("doc-1")).isEqualTo(0);
        assertThat(handler.getRooms()).doesNotContainKey("doc-1");
    }

    // ───── B-WS-06: 닫힌 세션에 브로드캐스트 시도 → 예외 없이 스킵 ─────
    @Test
    void B_WS_06_닫힌_세션에_브로드캐스트_예외_없음() throws Exception {
        WebSocketSession sender = mockSession("s1", "doc-1");
        WebSocketSession closed = mockClosedSession("s2", "doc-1");
        handler.afterConnectionEstablished(sender);
        handler.afterConnectionEstablished(closed);

        BinaryMessage msg = new BinaryMessage(new byte[]{1, 2, 3});
        assertThatCode(() -> handler.handleBinaryMessage(sender, msg))
                .doesNotThrowAnyException();

        // closed session은 isOpen()==false이므로 sendMessage 호출 안 함
        verify(closed, never()).sendMessage(any());
    }

    // ───── B-WS-07: 다른 documentId 세션 격리 ─────
    @Test
    void B_WS_07_다른_room_세션_격리() throws Exception {
        WebSocketSession s1 = mockSession("s1", "doc-1");
        WebSocketSession s2 = mockSession("s2", "doc-2");
        handler.afterConnectionEstablished(s1);
        handler.afterConnectionEstablished(s2);

        BinaryMessage msg = new BinaryMessage(new byte[]{1, 2, 3});
        handler.handleBinaryMessage(s1, msg);

        verify(s2, never()).sendMessage(any());
    }

    // ───── B-WS-08: 동시 연결 20개 → 모든 세션이 메시지 수신 ─────
    @Test
    void B_WS_08_동시_연결_20개_모든_세션_수신() throws Exception {
        WebSocketSession sender = mockSession("sender", "doc-load");
        handler.afterConnectionEstablished(sender);

        List<WebSocketSession> receivers = new ArrayList<>();
        for (int i = 0; i < 19; i++) {
            WebSocketSession s = mockSession("r" + i, "doc-load");
            handler.afterConnectionEstablished(s);
            receivers.add(s);
        }

        BinaryMessage msg = new BinaryMessage(new byte[]{9, 9, 9});
        handler.handleBinaryMessage(sender, msg);

        for (WebSocketSession r : receivers) {
            verify(r).sendMessage(msg);
        }
    }

    // ───── 추가: 상태 축적 및 drainPendingStates ─────
    @Test
    void 메시지_수신_시_pendingState_축적() throws Exception {
        WebSocketSession s1 = mockSession("s1", "doc-snap");
        WebSocketSession s2 = mockSession("s2", "doc-snap");
        handler.afterConnectionEstablished(s1);
        handler.afterConnectionEstablished(s2);

        handler.handleBinaryMessage(s1, new BinaryMessage(new byte[]{1, 2}));
        handler.handleBinaryMessage(s2, new BinaryMessage(new byte[]{3, 4}));

        Map<String, byte[]> states = handler.drainPendingStates();
        assertThat(states).containsKey("doc-snap");
        assertThat(states.get("doc-snap")).hasSize(4);
    }

    @Test
    void drainPendingStates_호출_후_초기화됨() throws Exception {
        WebSocketSession s1 = mockSession("s1", "doc-drain");
        handler.afterConnectionEstablished(s1);
        handler.handleBinaryMessage(s1, new BinaryMessage(new byte[]{1}));

        handler.drainPendingStates();  // first drain
        Map<String, byte[]> second = handler.drainPendingStates();

        assertThat(second).doesNotContainKey("doc-drain");
    }

    @Test
    void 연결_없는_문서는_drainPendingStates_미포함() throws Exception {
        // doc-no-session에는 세션이 없으므로 drain 결과에 포함 안 됨
        WebSocketSession s1 = mockSession("s1", "doc-active");
        handler.afterConnectionEstablished(s1);
        handler.handleBinaryMessage(s1, new BinaryMessage(new byte[]{1}));

        // doc-active에서만 상태 있음
        handler.afterConnectionClosed(s1, CloseStatus.NORMAL); // 세션 해제
        Map<String, byte[]> states = handler.drainPendingStates();

        // 세션이 닫혀서 rooms에서 doc-active 제거됨 → drain 대상 없음
        assertThat(states).doesNotContainKey("doc-active");
    }
}
