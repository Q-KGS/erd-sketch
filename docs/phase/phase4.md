# Phase 4 테스트 계획 — 고급 기능 (11–13주)

## 전제 조건

Phase 1, 2, 3 완료 기준 충족

## 범위

| 구현 항목 | 설명 |
|-----------|------|
| 버전 히스토리 | 자동/수동 버전 저장, 목록 조회, 복원 |
| 댓글 시스템 | 스레드형 댓글, 테이블/컬럼/관계 연결, 해결/재오픈 |
| 스키마 Diff | 현재 버전 vs 이전 버전 변경 사항 비교 |
| 마이그레이션 DDL | Diff 기반 ALTER TABLE 스크립트 생성 |
| 오프라인 지원 | y-indexeddb 로컬 큐잉, 재연결 동기화 |
| PDF 내보내기 | 서버측 PDF 생성 (타이틀 + 테이블 목록) |

---

## 1. 백엔드 단위/통합 테스트

### 1.1 버전 관리 (`DocumentVersionService`)

| # | 테스트 케이스 | 기대 결과 |
|---|--------------|-----------|
| B-VER-01 | 수동 버전 생성 | `version_number` 자동 증가, `schema_snapshot` 현재 상태 저장 |
| B-VER-02 | 같은 문서 두 번 버전 생성 | `version_number` 1 → 2 증가 |
| B-VER-03 | 버전 목록 조회 | `version_number` 내림차순 정렬 |
| B-VER-04 | 버전 상세 조회 | `schema_snapshot` 포함 |
| B-VER-05 | 버전 복원 | 현재 `yjs_state`, `schema_snapshot`을 해당 버전 값으로 교체 |
| B-VER-06 | 자동 버전 저장 트리거 | 30초 간격 스케줄러가 변경 감지 시 버전 생성 |
| B-VER-07 | 다른 문서 버전 조회 | 접근 권한 없으면 HTTP 403 |
| B-VER-08 | 존재하지 않는 버전 복원 | HTTP 404 |

```java
@Test
void createVersion_incrementsVersionNumber() {
    // given
    ErdDocument doc = createDocumentWithSnapshot(documentId, sampleSchema);

    // when
    DocumentVersion v1 = versionService.createVersion(documentId, new CreateVersionRequest(null), userId);
    DocumentVersion v2 = versionService.createVersion(documentId, new CreateVersionRequest("Tagged"), userId);

    // then
    assertThat(v1.getVersionNumber()).isEqualTo(1);
    assertThat(v2.getVersionNumber()).isEqualTo(2);
    assertThat(v2.getLabel()).isEqualTo("Tagged");
}
```

### 1.2 댓글 (`CommentService`)

| # | 테스트 케이스 | 기대 결과 |
|---|--------------|-----------|
| B-CMT-01 | 캔버스 댓글 생성 | `target_type: CANVAS`, `target_id: null` |
| B-CMT-02 | 테이블 댓글 생성 | `target_type: TABLE`, `target_id: {tableId}` |
| B-CMT-03 | 컬럼 댓글 생성 | `target_type: COLUMN`, `target_id: {columnId}` |
| B-CMT-04 | 대댓글 생성 | `parent_id` 설정, 목록에서 `replies[]` 포함 |
| B-CMT-05 | 댓글 해결 | `resolved: true` 업데이트 |
| B-CMT-06 | 해결된 댓글 재오픈 | `resolved: false` 업데이트 |
| B-CMT-07 | 댓글 수정 | `content` 변경, `updated_at` 갱신 |
| B-CMT-08 | 댓글 삭제 | `GET /comments` 에서 해당 댓글 미포함 |
| B-CMT-09 | 작성자가 아닌 사용자 수정 | HTTP 403 |
| B-CMT-10 | 문서 삭제 시 댓글 CASCADE 삭제 | DB에서 댓글 제거 확인 |
| B-CMT-11 | 문서 댓글 목록 | `resolved: false` 미해결 댓글만 필터 가능 |

```java
@Test
void createComment_withParentId_isReply() {
    Comment parent = commentService.create(documentId, new CreateCommentRequest(
        "CANVAS", null, "첫 번째 댓글", null), userId);
    Comment reply = commentService.create(documentId, new CreateCommentRequest(
        "CANVAS", null, "답글입니다", parent.getId()), userId);

    List<CommentResponse> comments = commentService.list(documentId, userId);
    assertThat(comments.get(0).replies()).hasSize(1);
    assertThat(comments.get(0).replies().get(0).content()).isEqualTo("답글입니다");
}
```

### 1.3 스키마 Diff (`SchemaDiffService`)

| # | 테스트 케이스 | 기대 결과 |
|---|--------------|-----------|
| B-DIFF-01 | 테이블 추가됨 | `changes` 에 `{type: "ADD_TABLE", tableName: "orders"}` |
| B-DIFF-02 | 테이블 삭제됨 | `changes` 에 `{type: "DROP_TABLE", tableName: "old_table"}` |
| B-DIFF-03 | 컬럼 추가됨 | `{type: "ADD_COLUMN", tableName: "users", columnName: "phone"}` |
| B-DIFF-04 | 컬럼 삭제됨 | `{type: "DROP_COLUMN", tableName: "users", columnName: "fax"}` |
| B-DIFF-05 | 컬럼 타입 변경 | `{type: "MODIFY_COLUMN", from: "VARCHAR", to: "TEXT"}` |
| B-DIFF-06 | 컬럼 nullable 변경 | `{type: "MODIFY_COLUMN", change: "nullable: false→true"}` |
| B-DIFF-07 | 변경 없음 | `changes: []`, `migrationDdl: ""` |
| B-DIFF-08 | 복수 변경 사항 | 모든 변경 사항 포함 |

### 1.4 마이그레이션 DDL 생성 (`MigrationDdlGenerator`)

| # | 테스트 케이스 | 기대 ALTER 구문 |
|---|--------------|----------------|
| B-MIG-01 | 컬럼 추가 | `ALTER TABLE "users" ADD COLUMN "phone" VARCHAR` |
| B-MIG-02 | 컬럼 삭제 | `ALTER TABLE "users" DROP COLUMN "fax"` |
| B-MIG-03 | 컬럼 타입 변경 | `ALTER TABLE "users" ALTER COLUMN "bio" TYPE TEXT` (PostgreSQL) |
| B-MIG-04 | 테이블 추가 | `CREATE TABLE "orders" (...)` |
| B-MIG-05 | 테이블 삭제 | `DROP TABLE "old_table"` |
| B-MIG-06 | NULL → NOT NULL | `ALTER TABLE "t" ALTER COLUMN "c" SET NOT NULL` |
| B-MIG-07 | 방언별 ALTER 문법 차이 | MySQL: `MODIFY COLUMN`, MSSQL: `ALTER COLUMN` |

### 1.5 PDF 내보내기

| # | 테스트 케이스 | 기대 결과 |
|---|--------------|-----------|
| B-PDF-01 | PDF 생성 | 유효한 PDF 바이너리 반환 |
| B-PDF-02 | 타이틀 페이지 | 프로젝트명, 날짜 포함 |
| B-PDF-03 | 테이블 목록 | 각 테이블명, 컬럼 목록 포함 |
| B-PDF-04 | 테이블 없는 문서 | "스키마가 없습니다" 페이지 생성 |

---

## 2. 프론트엔드 단위 테스트

### 2.1 버전 히스토리 패널 (`VersionHistoryPanel`)

| # | 테스트 케이스 | 기대 결과 |
|---|--------------|-----------|
| F-VER-01 | 버전 목록 렌더링 | `v1`, `v2` 항목 표시 |
| F-VER-02 | 버전 클릭 | 상세 정보(날짜, 레이블) 표시 |
| F-VER-03 | "복원" 버튼 클릭 | 확인 다이얼로그 후 `restoreVersion` API 호출 |
| F-VER-04 | "새 버전 저장" 클릭 | 레이블 입력 후 `createVersion` API 호출 |
| F-VER-05 | 버전 없을 때 | "저장된 버전이 없습니다" 메시지 |

### 2.2 댓글 패널 (`CommentPanel`)

| # | 테스트 케이스 | 기대 결과 |
|---|--------------|-----------|
| F-CMT-01 | 댓글 목록 렌더링 | 작성자 이름, 내용, 날짜 표시 |
| F-CMT-02 | 댓글 작성 | 입력 후 전송, 목록 갱신 |
| F-CMT-03 | 대댓글 작성 | "답글" 클릭 → 들여쓰기된 입력 표시 |
| F-CMT-04 | 해결 버튼 | `resolved: true`, 댓글 스타일 변경 |
| F-CMT-05 | 미해결 필터 | 해결된 댓글 숨김 |

---

## 3. E2E 시나리오

### 시나리오 E2E-VER-01: 수동 버전 저장 및 복원

```
1. ERD에서 "users", "posts" 테이블 생성
2. 버전 패널 열기 → "현재 상태 저장" 클릭
3. 레이블 "v1 - 초기 설계" 입력 후 저장
4. "products" 테이블 추가
5. 버전 패널에서 "v1 - 초기 설계" 선택 → 복원
6. 캔버스에 "products" 테이블이 사라지고 "users", "posts"만 존재 확인
```

**검증 포인트:**
- [ ] 버전 목록에 레이블 표시
- [ ] 복원 확인 다이얼로그 표시
- [ ] 복원 후 캔버스 상태 변경

### 시나리오 E2E-VER-02: 자동 버전 히스토리

```
1. ERD 편집 30초 이상 진행
2. 버전 패널 열기
3. "자동 저장" 레이블의 버전 항목 존재 확인
```

**검증 포인트:**
- [ ] 자동 저장 버전이 목록에 나타남
- [ ] 자동 버전도 복원 가능

### 시나리오 E2E-CMT-01: 테이블에 댓글 달기

```
1. "users" 테이블 우클릭 → "댓글 추가" 메뉴
2. 댓글 내용 "email 컬럼 인덱스 추가 필요" 입력
3. 댓글 패널에서 해당 댓글 표시 확인
4. 다른 사용자(탭B)에서 동일 댓글 확인 (실시간)
5. "답글" 클릭 → "확인했습니다" 대댓글 작성
6. 댓글 "해결" 버튼 클릭
7. 해결된 댓글 스타일 변경 확인
```

**검증 포인트:**
- [ ] 댓글 패널에 테이블명 연결 표시
- [ ] 대댓글 들여쓰기 렌더링
- [ ] 해결 후 `resolved` 배지 표시
- [ ] 탭B에 실시간 댓글 반영 (Polling 또는 WebSocket)

### 시나리오 E2E-DIFF-01: 스키마 Diff 확인

```
1. "users" 테이블 (id, email, name)으로 v1 저장
2. "phone" 컬럼 추가, "name" → "full_name" 으로 변경
3. "orders" 테이블 신규 추가
4. Diff 패널 열기 → "v1과 비교"
5. 변경 사항 목록 확인:
   - ADDED: orders 테이블
   - ADDED COLUMN: users.phone
   - MODIFIED COLUMN: users.name → full_name
```

**검증 포인트:**
- [ ] 각 변경 유형 색상 구분 (추가: 초록, 삭제: 빨강, 변경: 노랑)
- [ ] "마이그레이션 DDL 생성" 버튼으로 ALTER 스크립트 확인

### 시나리오 E2E-DIFF-02: 마이그레이션 DDL 생성

```
1. E2E-DIFF-01 시나리오 완료 상태
2. "마이그레이션 DDL 생성" 클릭
3. 생성된 DDL 확인:
   ALTER TABLE "users" ADD COLUMN "phone" VARCHAR;
   ALTER TABLE "users" RENAME COLUMN "name" TO "full_name";
   CREATE TABLE "orders" (...);
4. 실제 PostgreSQL DB에서 실행 검증
```

### 시나리오 E2E-OFFLINE-01: 오프라인 편집 → 재연결 동기화

```
1. 에디터 정상 접속 (온라인)
2. 개발자 도구에서 Network Offline 설정
3. 테이블 "offline_table" 생성
4. 편집 패널에서 컬럼 추가
5. 온라인 복구
6. 서버에 변경 사항 동기화 확인 (DB 직접 조회)
```

**검증 포인트:**
- [ ] 오프라인 상태에서 편집 계속 가능
- [ ] 오프라인 중 변경 사항이 IndexedDB에 저장
- [ ] 온라인 복구 후 WebSocket 재연결 및 서버 sync
- [ ] 다른 사용자 탭에 변경 사항 전파

### 시나리오 E2E-PDF-01: PDF 내보내기

```
1. 3개 테이블이 있는 ERD
2. "내보내기 → PDF" 클릭
3. 다운로드된 PDF 열기
4. 타이틀 페이지 (프로젝트명, 날짜) 확인
5. 각 테이블의 컬럼 목록 페이지 확인
```

**검증 포인트:**
- [ ] 유효한 PDF 파일 형식
- [ ] 테이블명, 컬럼명, 타입 포함
- [ ] 한글 문자 깨짐 없음

---

## 4. 마이그레이션 DDL 실행 검증

### 테스트 절차

```bash
# 1단계: v1 스키마 배포
psql -U erdsketch -d test_migration -f schema_v1.sql

# 2단계: 마이그레이션 스크립트 실행
psql -U erdsketch -d test_migration -f migration_v1_to_v2.sql

# 3단계: 결과 검증
psql -U erdsketch -d test_migration -c "\d+ users"
```

### 검증 항목

- [ ] `ALTER TABLE ADD COLUMN` 성공
- [ ] `ALTER TABLE DROP COLUMN` 성공
- [ ] `ALTER TABLE RENAME COLUMN` 성공
- [ ] `CREATE TABLE` (새 테이블) 성공
- [ ] 기존 데이터 손실 없음 (사전 INSERT 후 마이그레이션)

---

## 5. 성능 및 경계 조건

| 케이스 | 기대 결과 |
|--------|-----------|
| 버전 100개 이상인 문서 | 버전 목록 페이지네이션 (최신 20개 기본) |
| 댓글 500개 이상 | 페이지네이션 또는 무한 스크롤 |
| Diff: 테이블 50개 변경 | 5초 이내 완료 |
| 오프라인 6시간 후 재연결 | IndexedDB 캐시로 복원 후 동기화 |
| PDF 테이블 100개 | 생성 시간 30초 이내 |

---

## 6. 완료 기준 (Definition of Done)

- [x] 버전 저장/복원 통합 테스트 (B-VER-01 ~ B-VER-08) 통과
- [x] 댓글 CRUD 통합 테스트 (B-CMT-01 ~ B-CMT-11) 통과
- [x] 스키마 Diff 단위 테스트 (B-DIFF-01 ~ B-DIFF-08) 통과
- [x] 마이그레이션 DDL 단위 테스트 (B-MIG-01 ~ B-MIG-07) 통과
- [x] PDF 내보내기 단위 테스트 (B-PDF-01 ~ B-PDF-04) 통과
- [x] 버전 히스토리 패널 단위 테스트 (F-VER-01 ~ F-VER-05) 통과
- [x] 댓글 패널 단위 테스트 (F-CMT-01 ~ F-CMT-05) 통과
- [ ] E2E-VER-01 (버전 복원) 수동 검증 완료
- [ ] E2E-CMT-01 (댓글 + 실시간 표시) 수동 검증 완료
- [ ] E2E-DIFF-02 마이그레이션 DDL 실제 DB 실행 성공
- [ ] E2E-OFFLINE-01 (오프라인 편집 → 재연결) 수동 검증 완료
- [ ] PDF 내보내기 한글 포함 정상 렌더링 확인
