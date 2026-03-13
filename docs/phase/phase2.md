# Phase 2 테스트 계획 — 실시간 협업 (5–7주)

## 전제 조건

Phase 1 완료 기준 충족 (인증, CRUD, 기본 DDL 동작)

## 범위

| 구현 항목 | 설명 |
|-----------|------|
| Yjs 통합 | Yjs 문서 구조, y-websocket, y-indexeddb |
| WebSocket 릴레이 | Spring WebSocket Yjs 바이너리 브로드캐스트 |
| 사용자 프레즌스 | Awareness 기반 아바타, 커서 위치 표시 |
| 선택 인식 | 다른 사용자가 편집 중인 테이블 하이라이트 |
| 자동 저장 | 30초 간격 schema_snapshot PostgreSQL 저장 |

---

## 1. 백엔드 단위/통합 테스트

### 1.1 WebSocket 릴레이 (`YjsWebSocketHandler`)

| # | 테스트 케이스 | 기대 결과 |
|---|--------------|-----------|
| B-WS-01 | 세션 연결 | `rooms[documentId]` 에 세션 추가 |
| B-WS-02 | 바이너리 메시지 수신 | 같은 방(documentId)의 다른 세션에 브로드캐스트 |
| B-WS-03 | 발신자에게는 반송 안 함 | 메시지 발신 세션에는 전송하지 않음 |
| B-WS-04 | 세션 연결 해제 | `rooms[documentId]` 에서 제거 |
| B-WS-05 | 마지막 세션 해제 | `rooms` 에서 `documentId` 키 제거 |
| B-WS-06 | 닫힌 세션에 브로드캐스트 시도 | 예외 발생 없이 스킵 |
| B-WS-07 | 다른 documentId 세션 격리 | 다른 방의 세션에는 메시지 전달 안 함 |
| B-WS-08 | 동시 연결 20개 | 모든 세션이 메시지 수신 |

```java
// 예시: YjsWebSocketHandlerTest
@Test
void handleBinaryMessage_broadcastsToOtherSessionsOnly() throws Exception {
    WebSocketSession sender = mockSession("s1");
    WebSocketSession receiver = mockSession("s2");
    handler.afterConnectionEstablished(sender);   // path: /ws/documents/doc-1
    handler.afterConnectionEstablished(receiver);

    BinaryMessage msg = new BinaryMessage(new byte[]{1, 2, 3});
    handler.handleBinaryMessage(sender, msg);

    verify(receiver).sendMessage(msg);
    verify(sender, never()).sendMessage(any());
}
```

### 1.2 자동 저장 스케줄러

| # | 테스트 케이스 | 기대 결과 |
|---|--------------|-----------|
| B-SNAP-01 | 연결된 세션이 있는 문서 스냅샷 | `schema_snapshot` 업데이트 됨 |
| B-SNAP-02 | 연결된 세션이 없는 문서 | 스냅샷 업데이트 스킵 |
| B-SNAP-03 | `yjs_state` null → 스냅샷 | 처리 오류 없이 null 유지 |

---

## 2. 프론트엔드 단위 테스트

### 2.1 Yjs 프로바이더 (`yjsProvider.ts`)

| # | 테스트 케이스 | 기대 결과 |
|---|--------------|-----------|
| F-YJS-01 | `createYjsProvider` 호출 | `ydoc`, `wsProvider`, `awareness` 반환 |
| F-YJS-02 | `destroy()` 호출 | wsProvider, idbProvider, ydoc 모두 destroy |
| F-YJS-03 | awareness에 userPresence 설정 | `awareness.getLocalState().user` 일치 |

### 2.2 `useYjs` 훅

| # | 테스트 케이스 | 기대 결과 |
|---|--------------|-----------|
| F-HOOK-01 | `documentId` null | `ydoc: null`, `isConnected: false` |
| F-HOOK-02 | `documentId` 변경 | 이전 컨텍스트 destroy, 새 컨텍스트 생성 |
| F-HOOK-03 | 컴포넌트 언마운트 | destroy 호출 |

### 2.3 `useSchema` 훅

| # | 테스트 케이스 | 기대 결과 |
|---|--------------|-----------|
| F-SCHEMA-01 | ydoc 업데이트 시 schema 재계산 | `update` 이벤트 후 새 객체 반환 |
| F-SCHEMA-02 | `addTable` 후 schema | 새 테이블 포함 |
| F-SCHEMA-03 | `updateTable` 후 schema | 변경된 name 반영 |
| F-SCHEMA-04 | `deleteTable` 후 schema | 해당 테이블 없음 |
| F-SCHEMA-05 | `addRelationship` 후 schema | 새 관계 포함 |
| F-SCHEMA-06 | `deleteRelationship` 후 schema | 해당 관계 없음 |

### 2.4 `CollaborationPresence` 컴포넌트

| # | 테스트 케이스 | 기대 결과 |
|---|--------------|-----------|
| F-PRES-01 | 자신 포함 0명 | 아바타 미렌더링 |
| F-PRES-02 | 다른 사용자 2명 | 아바타 2개 렌더링, 이름 첫 글자 표시 |
| F-PRES-03 | 6명 이상 | 최대 5개 + "+N" 뱃지 표시 |
| F-PRES-04 | awareness change 이벤트 | 리렌더링 발생 |

---

## 3. 실시간 협업 E2E 시나리오

> **준비**: 같은 ERD 문서 URL을 두 개의 브라우저 탭(탭A, 탭B)에서 각각 다른 사용자로 열기

### 시나리오 E2E-COLLAB-01: 테이블 동기화

```
1. 탭A: 캔버스 더블클릭 → "orders" 테이블 생성
2. 탭B: 1초 이내에 "orders" 테이블 노드가 나타남 확인
3. 탭A: 테이블 이름을 "order_items"로 변경
4. 탭B: 테이블명이 "order_items"로 자동 변경 확인
```

**검증 포인트:**
- [ ] 탭B에서 3초 이내 동기화 완료
- [ ] 탭B의 캔버스 노드 위치가 탭A와 동일
- [ ] 두 탭의 테이블 수 일치

### 시나리오 E2E-COLLAB-02: 동시 편집 충돌 없음

```
1. 탭A: "users" 테이블 생성
2. 탭B: "posts" 테이블 생성 (동시에)
3. 양쪽 모두 두 테이블이 존재하는지 확인
4. 탭A: "users" 테이블에 "email" 컬럼 추가
5. 탭B: 동시에 "posts" 테이블에 "title" 컬럼 추가
6. 양쪽 모두 각자의 변경이 반영된 상태 확인
```

**검증 포인트:**
- [ ] 두 탭 모두 테이블 2개 표시
- [ ] 탭A: `users.email` 존재, `posts.title` 존재
- [ ] 탭B: `users.email` 존재, `posts.title` 존재
- [ ] 데이터 손실 없음

### 시나리오 E2E-COLLAB-03: 사용자 프레즌스 아바타

```
1. 탭A, 탭B 모두 같은 문서 접속
2. 탭A에서 캔버스 우측 상단에 탭B 사용자의 아바타 표시 확인
3. 탭B에서 캔버스 우측 상단에 탭A 사용자의 아바타 표시 확인
4. 탭B 닫기
5. 탭A에서 탭B 아바타가 사라짐 확인
```

**검증 포인트:**
- [ ] 아바타에 사용자 이름 첫 글자 표시
- [ ] 각 사용자마다 고유한 색상
- [ ] 탭 닫힌 후 3초 내 아바타 제거

### 시나리오 E2E-COLLAB-04: 선택 인식

```
1. 탭A: "users" 테이블 클릭 (선택)
2. 탭B: "users" 테이블이 하이라이트/표시 변경 확인
3. 탭A: 다른 테이블 선택
4. 탭B: 하이라이트 이동 확인
```

**검증 포인트:**
- [ ] 선택 중인 테이블에 다른 사용자 색상 테두리 표시
- [ ] 선택 해제 시 즉시 제거

### 시나리오 E2E-COLLAB-05: 연결 해제 후 재연결 동기화

```
1. 탭A, 탭B 접속
2. 탭B: 네트워크 오프라인 (개발자 도구 Network → Offline)
3. 탭A: 테이블 2개 추가
4. 탭B: 네트워크 복구 (Online)
5. 탭B: 재연결 후 탭A에서 추가된 테이블 동기화 확인
```

**검증 포인트:**
- [ ] 툴바 연결 상태 표시 "오프라인" → "연결됨" 변경
- [ ] 재연결 후 탭B에 탭A의 변경 사항 반영

### 시나리오 E2E-COLLAB-06: Undo/Redo 협업

```
1. 탭A: "category" 테이블 생성
2. 탭B: 해당 테이블 확인
3. 탭A: Ctrl+Z (undo) → 테이블 사라짐
4. 탭B: 테이블 사라짐 확인
5. 탭A: Ctrl+Shift+Z (redo) → 테이블 복원
6. 탭B: 테이블 복원 확인
```

**검증 포인트:**
- [ ] Undo는 해당 사용자(탭A)의 변경만 되돌림
- [ ] 탭B의 독립적 변경은 탭A의 Undo에 영향받지 않음

---

## 4. 성능 검증

### 동기화 지연 측정

| 시나리오 | 목표 |
|---------|------|
| 테이블명 변경 → 상대방 반영 | < 200ms (로컬 네트워크) |
| 컬럼 추가 → 상대방 반영 | < 200ms |
| 20명 동시 접속 상태에서 변경 전파 | < 500ms |

**측정 방법:**
```javascript
// 브라우저 콘솔에서 타임스탬프 기록
const t0 = performance.now()
// 변경 발생 후 상대 탭에서 DOM 변경 감지
const t1 = performance.now()
console.log(`동기화 지연: ${t1 - t0}ms`)
```

### WebSocket 연결 유지

- [ ] 30분 유휴 상태 후에도 WebSocket 연결 유지 확인
- [ ] 핑/퐁 메시지로 연결 유지 (Spring WebSocket 기본 설정)

---

## 5. 자동 저장 검증

```
1. 에디터에서 테이블 3개 생성
2. 30초 대기
3. DB 직접 조회: SELECT schema_snapshot FROM erd_documents WHERE id = '{docId}'
4. JSONB에 테이블 3개 포함 확인
5. 서버 재시작
6. 에디터 재접속 후 y-indexeddb에서 오프라인 상태 복원 확인
```

**검증 포인트:**
- [ ] `schema_snapshot`이 null이 아님
- [ ] `schema_snapshot.tables` 키 존재
- [ ] `yjs_state` BYTEA 데이터 저장 확인

---

## 6. 수동 검증 체크리스트

### WebSocket

- [ ] 브라우저 개발자 도구 → Network → WS 탭에서 `/ws/documents/{id}` 연결 확인
- [ ] 메시지 프레임이 Binary 타입으로 전송됨 확인
- [ ] 서버 재시작 후 클라이언트 자동 재연결 (y-websocket 내장 재시도)

### 오프라인 지원

- [ ] 오프라인에서 에디터 접속 시 IndexedDB 캐시로 이전 상태 표시
- [ ] 오프라인 편집 후 온라인 복귀 시 변경 사항 서버 동기화

---

## 7. 완료 기준 (Definition of Done)

- [x] `YjsWebSocketHandler` 단위 테스트 전체 통과 (B-WS-01~08 + 3 추가, 총 11개)
- [x] `SnapshotScheduler` 단위 테스트 전체 통과 (B-SNAP-01~03 + 2 추가, 총 5개)
- [x] 프론트엔드 Yjs 프로바이더 테스트 통과 (F-YJS-01~03 + 1 추가, 총 4개)
- [x] `useYjs` 훅 테스트 통과 (F-HOOK-01~03, 총 3개)
- [x] `useSchema` 훅 테스트 통과 (F-SCHEMA-01~06 + 2 추가, 총 8개)
- [x] `CollaborationPresence` 컴포넌트 테스트 통과 (F-PRES-01~04, 총 4개)
- [x] 백엔드 전체 테스트 64개 통과 (`mvn test`) — Phase 2 완료 시점 기준
- [x] 프론트엔드 전체 테스트 39개 통과 (`npm test`) — Phase 2 완료 시점 기준
- [ ] E2E-COLLAB-01, 02 (기본 동기화, 충돌 없음) 검증 완료
- [ ] E2E-COLLAB-03 (프레즌스 아바타) 수동 검증 완료
- [ ] 30초 자동 저장 후 DB에 `schema_snapshot` 존재 확인
- [ ] 동기화 지연 < 200ms (로컬 네트워크 기준)
- [ ] 20명 동시 접속 부하 테스트 통과 (모든 세션 메시지 수신)

---

## 8. 구현 완료 내역 (2026-03-13)

### 테스트 파일
- `YjsWebSocketHandlerTest.java` — B-WS-01~08 + 3 추가 (11개 테스트)
- `SnapshotSchedulerTest.java` (DocumentServiceTest 내) — B-SNAP-01~03 + 2 추가 (5개 테스트)
- `yjsProvider.test.ts` — F-YJS-01~03 + 1 추가 (4개 테스트)
- `useYjs.test.tsx` — F-HOOK-01~03 (3개 테스트)
- `useSchema.test.ts` — F-SCHEMA-01~06 + 2 추가 (8개 테스트)
- `CollaborationPresence.test.tsx` — F-PRES-01~04 (4개 테스트)
