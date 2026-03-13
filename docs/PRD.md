# ErdSketch - 웹 기반 ERD 설계 도구 PRD

**버전:** 1.0
**작성일:** 2026-03-13
**대상 사용자:** 팀(5~20명) 내부용

---

## 1. 개요

### 1.1 제품 비전

팀 내에서 DB 스키마를 시각적으로 설계하고 실시간으로 협업할 수 있는 웹 기반 ERD 도구.

### 1.2 핵심 목표

- **DB 스키마 설계**: 직관적인 캔버스 기반 ERD 편집
- **실시간 협업**: CRDT 기반 충돌 없는 동시 편집
- **다중 DB DDL 지원**: MySQL, PostgreSQL, Oracle, MSSQL 방언 지원

### 1.3 사용자 페르소나

- 백엔드 개발자: DB 스키마 설계 및 DDL 생성
- DB 아키텍트: 복잡한 ERD 설계 및 문서화
- 팀 리드: 팀원과 실시간 스키마 리뷰

---

## 2. 기술 스택

### Frontend

| 기술 | 버전 | 용도 |
|------|------|------|
| React | 18+ | UI 프레임워크 |
| TypeScript | 5+ | 타입 안전성 |
| React Flow | 11+ | ERD 캔버스 |
| Yjs | ^13 | CRDT 실시간 협업 |
| y-websocket | ^1 | Yjs WebSocket 전송 |
| y-indexeddb | ^9 | 오프라인 캐시 |
| Zustand | ^4 | 로컬 UI 상태 |
| TanStack Query | ^5 | REST API 데이터 페칭 |
| CodeMirror | 6 | DDL 구문 강조 |
| Tailwind CSS | ^3 | 스타일링 |
| React Router | ^6 | 클라이언트 라우팅 |
| Vite | ^5 | 빌드 도구 |

### Backend

| 기술 | 버전 | 용도 |
|------|------|------|
| Spring Boot | 3.x | API 서버 |
| Spring WebSocket | - | Yjs 릴레이 |
| Spring Security | - | 인증/인가 |
| Spring Data JPA | - | DB 접근 |
| PostgreSQL | 15+ | 앱 데이터베이스 |
| JSqlParser | ^4 | DDL 파싱 |
| Flyway | - | DB 마이그레이션 |
| Java | 21 | 런타임 |

---

## 3. 데이터 모델

### 3.1 앱 데이터베이스 엔티티

```sql
-- 사용자
User(id UUID PK, email, display_name, avatar_url, password_hash, created_at, updated_at)

-- 워크스페이스
Workspace(id UUID PK, name, slug UNIQUE, owner_id→User, created_at, updated_at)

-- 워크스페이스 멤버
WorkspaceMember(workspace_id→Workspace, user_id→User, role[OWNER|ADMIN|MEMBER|VIEWER], joined_at)

-- 프로젝트
Project(id UUID PK, workspace_id→Workspace, name, description,
        target_db_type[MYSQL|POSTGRESQL|ORACLE|MSSQL], created_by→User, created_at, updated_at)

-- ERD 문서
ErdDocument(id UUID PK, project_id→Project, name,
            yjs_state BYTEA, schema_snapshot JSONB, canvas_settings JSONB, created_at, updated_at)

-- 문서 버전
DocumentVersion(id UUID PK, document_id→ErdDocument, version_number INT,
                yjs_state BYTEA, schema_snapshot JSONB, label, created_by→User, created_at)

-- 댓글
Comment(id UUID PK, document_id→ErdDocument, author_id→User,
        target_type[TABLE|COLUMN|RELATIONSHIP|CANVAS], target_id, content TEXT,
        resolved BOOL, parent_id→Comment, created_at, updated_at)
```

### 3.2 ERD 스키마 모델 (Yjs 문서 / JSONB 내부)

```typescript
interface TableDef {
  id: string;
  name: string;
  schema?: string;
  comment?: string;
  color?: string;
  position: { x: number; y: number };
  columns: ColumnDef[];
  indexes: IndexDef[];
}

interface ColumnDef {
  id: string;
  name: string;
  data_type: string;
  nullable: boolean;
  default_value?: string;
  is_primary_key: boolean;
  is_unique: boolean;
  is_auto_increment: boolean;
  comment?: string;
  order: number;
}

interface IndexDef {
  id: string;
  name: string;
  columns: string[];
  is_unique: boolean;
  type: 'BTREE' | 'HASH' | 'GIN' | 'GIST';
}

interface RelationshipDef {
  id: string;
  name?: string;
  source_table_id: string;
  source_column_ids: string[];
  target_table_id: string;
  target_column_ids: string[];
  cardinality: '1:1' | '1:N' | 'M:N';
  on_delete: 'CASCADE' | 'SET_NULL' | 'RESTRICT' | 'NO_ACTION';
  on_update: 'CASCADE' | 'SET_NULL' | 'RESTRICT' | 'NO_ACTION';
}

interface NoteDef {
  id: string;
  content: string;
  position: { x: number; y: number };
  color?: string;
}
```

---

## 4. 기능 명세

### 4.1 핵심 기능 - ERD 캔버스 & 스키마 편집

#### P0 (필수)

| 기능 | 설명 | 구현 방법 |
|------|------|-----------|
| 캔버스 팬/줌 | 무한 캔버스, 드래그 팬, 스크롤 줌, 미니맵 | React Flow 기본 기능 |
| 테이블 생성 | 툴바 버튼 | React Flow 커스텀 노드 |
| 테이블 편집 | 이름 변경, 컬럼 추가/삭제/순서변경 | 인라인 편집 패널 |
| 컬럼 편집 | 이름, 타입, nullable, PK, default, unique, auto-increment | 드롭다운 + 체크박스 |
| 테이블/관계 삭제 | Delete 키 또는 컨텍스트 메뉴 | React Flow onDelete |
| 관계 생성 | 소스 컬럼 → 타겟 컬럼 드래그 | React Flow 커스텀 엣지 |
| 관계 편집 | 카디널리티, ON DELETE/UPDATE | 엣지 클릭 패널 |
| 다중 선택 | Shift+클릭, 드래그 선택 | React Flow 멀티셀렉트 |
| Undo/Redo | Ctrl+Z / Ctrl+Shift+Z | Yjs UndoManager |
| 자동 레이아웃 | dagre 기반 자동 배치 | @dagrejs/dagre |
| 대상 DB 선택 | 프로젝트별 타겟 DB | Zustand 상태 |

#### P1 (중요)

| 기능 | 설명 |
|------|------|
| 테이블 색상/그룹 | 도메인별 색상 분류 |
| 캔버스 메모 | 스티키 노트 |
| 인덱스 관리 | 테이블별 인덱스 정의 |
| 테이블 검색 | 검색 및 캔버스 하이라이트 |
| 키보드 단축키 | 주요 작업 단축키 |

#### P2 (선택)

| 기능 | 설명 |
|------|------|
| 스키마 영역 | 도메인별 시각적 그룹 |
| 테이블 템플릿 | 감사 컬럼 등 사전 정의 |
| 다크 모드 | 다크 모드 지원 |
| 그리드 스냅 | 그리드 정렬 |

### 4.2 실시간 협업

#### P0

| 기능 | 설명 | 구현 |
|------|------|------|
| 실시간 동기화 | 모든 변경 실시간 전파 | Yjs + y-websocket |
| 사용자 프레즌스 | 현재 사용자 아바타 | Yjs Awareness |
| 커서 추적 | 다른 사용자 커서 위치 | Yjs Awareness |
| 선택 인식 | 편집 중인 테이블 하이라이트 | Yjs Awareness |
| 충돌 없는 편집 | 자동 병합 | CRDT |

#### P1

| 기능 | 설명 |
|------|------|
| 버전 히스토리 | 자동/수동 버전 저장, 조회, 복원 |
| 댓글 | 스레드형 댓글, 해결/재오픈 |
| 변경 알림 | 구조적 변경 시 토스트 |
| 오프라인 지원 | y-indexeddb 로컬 큐잉 |

#### P2

| 기능 | 설명 |
|------|------|
| 팔로우 모드 | 특정 사용자 뷰포트 따라가기 |
| 활동 피드 | 최근 변경 사이드바 |
| 버전 간 Diff | 시각적 비교 |

### 4.3 DDL 기능

#### P0

| 기능 | 설명 |
|------|------|
| DDL 생성 | ERD → CREATE TABLE / CREATE INDEX |
| 다중 방언 | MySQL, PostgreSQL, Oracle, MSSQL |
| DDL 가져오기 | DDL SQL → ERD 자동 생성 |
| DDL 클립보드 복사 | 원클릭 복사 |

#### P1

| 기능 | 설명 |
|------|------|
| DDL 프리뷰 패널 | 실시간 DDL 미리보기 (CodeMirror) |
| 스키마 Diff | 버전 비교 |
| 마이그레이션 DDL | ALTER TABLE 스크립트 생성 |
| 선택적 생성 | 선택된 테이블만 DDL |

#### P2

| 기능 | 설명 |
|------|------|
| JDBC 연결 | 라이브 DB 역공학 |
| Liquibase/Flyway 내보내기 | 마이그레이션 파일 |
| 타입 매핑 커스터마이징 | 방언 간 사용자 정의 |
| DDL 검증 | 방언별 제약 경고 |

### 4.4 프로젝트 관리

#### P0

| 기능 | 설명 |
|------|------|
| 회원가입/로그인 | 이메일+비밀번호, JWT |
| 워크스페이스 생성 | 팀 컨테이너, 이메일 초대 |
| 프로젝트 CRUD | 워크스페이스 내 프로젝트 |
| 역할 기반 접근제어 | Owner, Admin, Member, Viewer |
| 프로젝트 목록 | 대시보드 |

#### P1

| 기능 | 설명 |
|------|------|
| OAuth 로그인 | Google / GitHub |
| 프로젝트별 권한 | 역할 오버라이드 |
| 초대 링크 | 공유 가능한 초대 링크 |
| 감사 로그 | 활동 로그 |

### 4.5 내보내기 / 가져오기

#### P0

| 기능 | 설명 |
|------|------|
| SQL 파일 내보내기 | .sql 다운로드 |
| JSON 내보내기/가져오기 | ERD 스키마 백업/이동 |
| PNG 내보내기 | html-to-image (클라이언트) |

#### P1

| 기능 | 설명 |
|------|------|
| SVG 내보내기 | 벡터 출력 |
| PDF 내보내기 | 서버측 생성 |
| 데이터 사전 내보내기 | Markdown/HTML |

---

## 5. API 명세

### 5.1 REST API

#### 인증

| Method | Path | 설명 |
|--------|------|------|
| POST | `/api/v1/auth/register` | 회원가입 |
| POST | `/api/v1/auth/login` | 로그인 (JWT 발급) |
| POST | `/api/v1/auth/refresh` | 토큰 갱신 |
| POST | `/api/v1/auth/oauth/{provider}` | OAuth 로그인 |
| GET | `/api/v1/auth/me` | 현재 사용자 정보 |

#### 워크스페이스

| Method | Path | 설명 |
|--------|------|------|
| POST | `/api/v1/workspaces` | 워크스페이스 생성 |
| GET | `/api/v1/workspaces` | 내 워크스페이스 목록 |
| GET | `/api/v1/workspaces/{workspaceId}` | 워크스페이스 상세 |
| PATCH | `/api/v1/workspaces/{workspaceId}` | 워크스페이스 수정 |
| DELETE | `/api/v1/workspaces/{workspaceId}` | 워크스페이스 삭제 |

#### 워크스페이스 멤버

| Method | Path | 설명 |
|--------|------|------|
| GET | `/api/v1/workspaces/{workspaceId}/members` | 멤버 목록 |
| POST | `/api/v1/workspaces/{workspaceId}/members/invite` | 멤버 초대 |
| PATCH | `/api/v1/workspaces/{workspaceId}/members/{userId}` | 역할 변경 |
| DELETE | `/api/v1/workspaces/{workspaceId}/members/{userId}` | 멤버 제거 |

#### 프로젝트

| Method | Path | 설명 |
|--------|------|------|
| POST | `/api/v1/workspaces/{workspaceId}/projects` | 프로젝트 생성 |
| GET | `/api/v1/workspaces/{workspaceId}/projects` | 프로젝트 목록 |
| GET | `/api/v1/projects/{projectId}` | 프로젝트 상세 |
| PATCH | `/api/v1/projects/{projectId}` | 프로젝트 수정 |
| DELETE | `/api/v1/projects/{projectId}` | 프로젝트 삭제 |

#### ERD 문서

| Method | Path | 설명 |
|--------|------|------|
| POST | `/api/v1/projects/{projectId}/documents` | 문서 생성 |
| GET | `/api/v1/projects/{projectId}/documents` | 문서 목록 |
| GET | `/api/v1/documents/{documentId}` | 문서 상세 |
| DELETE | `/api/v1/documents/{documentId}` | 문서 삭제 |
| PATCH | `/api/v1/documents/{documentId}/settings` | 캔버스 설정 변경 |
| GET | `/api/v1/documents/{documentId}/schema` | 스키마 스냅샷 조회 |

#### 버전 관리

| Method | Path | 설명 |
|--------|------|------|
| GET | `/api/v1/documents/{documentId}/versions` | 버전 목록 |
| POST | `/api/v1/documents/{documentId}/versions` | 버전 생성 |
| GET | `/api/v1/documents/{documentId}/versions/{versionId}` | 버전 상세 |
| POST | `/api/v1/documents/{documentId}/versions/{versionId}/restore` | 버전 복원 |

#### 댓글

| Method | Path | 설명 |
|--------|------|------|
| POST | `/api/v1/documents/{documentId}/comments` | 댓글 생성 |
| GET | `/api/v1/documents/{documentId}/comments` | 댓글 목록 |
| PATCH | `/api/v1/comments/{commentId}` | 댓글 수정 |
| DELETE | `/api/v1/comments/{commentId}` | 댓글 삭제 |
| PATCH | `/api/v1/comments/{commentId}/resolve` | 댓글 해결 |

#### DDL

```
POST /api/v1/documents/{documentId}/ddl/generate
  Body:     { dialect: "MYSQL"|"POSTGRESQL"|"ORACLE"|"MSSQL", tableIds?: string[], includeDrops: boolean }
  Response: { ddl: string, warnings: string[] }

POST /api/v1/documents/{documentId}/ddl/parse
  Body:     { ddl: string, dialect: string }
  Response: { tables: TableDef[], relationships: RelationshipDef[], warnings: string[] }

POST /api/v1/documents/{documentId}/ddl/diff
  Body:     { baseVersionId?: string, targetDialect: string }
  Response: { changes: Change[], migrationDdl: string }
```

#### 내보내기/가져오기

```
POST /api/v1/documents/{documentId}/export
  Body:     { format: "PNG"|"SVG"|"PDF"|"JSON"|"SQL"|"MARKDOWN", options: {} }
  Response: binary 또는 JSON

POST /api/v1/documents/{documentId}/import
  Body:     multipart (file + format)
  Response: { tables: TableDef[], relationships: RelationshipDef[] }
```

### 5.2 WebSocket

```
WS /ws/documents/{documentId}

프로토콜: Yjs sync + Awareness (바이너리 메시지)

Awareness 메시지 포맷:
{
  clientId: number,
  user: {
    name: string,
    color: string,
    cursor: { x: number, y: number },
    selectedTableId: string | null,
    selectedColumnId: string | null
  }
}
```

---

## 6. 아키텍처

### 6.1 전체 구조

```
Browser Clients
  ├── REST (HTTPS) ──→ Spring Boot API Controllers
  │                          → Service Layer
  │                          → Spring Data JPA
  │                          → PostgreSQL
  │
  └── WebSocket ────→ Yjs Relay Handler
                            → 같은 문서 룸 클라이언트에 브로드캐스트
                            → 30초 간격 PostgreSQL 스냅샷 저장
```

### 6.2 Yjs 문서 구조

```
Y.Map (document root)
  ├── tables: Y.Map<TableId, Y.Map>
  │     └── [tableId]: Y.Map
  │           ├── id, name, schema, comment, color: Y.Text/primitive
  │           ├── position: Y.Map { x, y }
  │           ├── columns: Y.Array<Y.Map>
  │           └── indexes: Y.Array<Y.Map>
  ├── relationships: Y.Map<RelId, Y.Map>
  └── notes: Y.Map<NoteId, Y.Map>
```

### 6.3 이중 저장 전략

| 컬럼 | 타입 | 역할 | 용도 |
|------|------|------|------|
| `yjs_state` | BYTEA | 진실 소스 | 실시간 협업 복원 |
| `schema_snapshot` | JSONB | 읽기 캐시 | DDL 생성, 검색, diff (Java에서 Yjs 역직렬화 불필요) |

### 6.4 폴더 구조

```
erdsketch/
  frontend/                          # React + TypeScript
    src/
      components/
        canvas/                      # React Flow 캔버스
        editor/                      # 테이블/컬럼 편집 패널
        toolbar/                     # 툴바, 메뉴
        collaboration/               # 프레즌스, 커서, 댓글
        ddl/                         # DDL 프리뷰, 가져오기/내보내기
        project/                     # 프로젝트/워크스페이스 관리
        auth/                        # 로그인, 회원가입
        common/                      # 공통 UI 컴포넌트
      hooks/                         # 커스텀 React 훅
      store/                         # Zustand 스토어
      crdt/                          # Yjs 문서 설정, 프로바이더
      api/                           # REST API 클라이언트
      models/                        # TypeScript 인터페이스
      utils/                         # 유틸리티, 타입 매핑
  backend/                           # Spring Boot
    src/main/java/com/erdsketch/
      config/                        # Spring 설정
      auth/                          # 인증, JWT
      workspace/                     # 워크스페이스 CRUD
      project/                       # 프로젝트 CRUD
      document/                      # 문서 CRUD, 버전
      collaboration/                 # WebSocket, Yjs 릴레이
      ddl/
        generator/                   # 방언별 DDL 생성기
        parser/                      # 방언별 DDL 파서
        diff/                        # 스키마 비교
      export/                        # 내보내기 서비스
      comment/                       # 댓글 CRUD
    src/main/resources/
      db/migration/                  # Flyway 마이그레이션
```

---

## 7. 구현 로드맵

### Phase 1 (1-4주) - 기본 기능 ✅

- [x] 프로젝트 스캐폴딩 (Vite + Spring Boot)
- [x] 인증 (JWT 회원가입/로그인)
- [x] 워크스페이스/프로젝트 CRUD
- [x] ERD 캔버스 (React Flow 테이블 노드)
- [x] 테이블/컬럼/관계 CRUD
- [x] 기본 DDL 생성/파싱 (PostgreSQL)

### Phase 2 (5-7주) - 실시간 협업

- [ ] Yjs 통합 및 문서 구조 설계
- [ ] Spring WebSocket Yjs 릴레이 서버
- [ ] 사용자 프레즌스 및 커서
- [ ] 실시간 협업 편집
- [ ] 자동 저장 (30초 스냅샷)

### Phase 3 (8-10주) - 다중 방언 & 내보내기

- [ ] MySQL/Oracle/MSSQL DDL 생성기/파서
- [ ] JSON 내보내기/가져오기
- [ ] PNG/SVG 내보내기
- [ ] DDL 프리뷰 패널 (CodeMirror)

### Phase 4 (11-13주) - 고급 기능

- [ ] 버전 히스토리 (저장/조회/복원)
- [ ] 댓글 시스템
- [ ] 스키마 Diff 및 마이그레이션 DDL
- [ ] 오프라인 지원
- [ ] PDF 내보내기

### Phase 5 (14주+) - 선택 기능

- [ ] OAuth (Google/GitHub)
- [ ] 다크 모드
- [ ] DBML 호환
- [ ] 팔로우 모드
- [ ] 프로젝트 템플릿
- [ ] JDBC 연결

---

## 8. 검증 계획

### 프론트엔드

- **단위/컴포넌트**: Vitest + React Testing Library
- **E2E**: Playwright

### 백엔드

- **통합 테스트**: JUnit 5 + Spring Boot Test
- **DB 테스트**: Testcontainers (PostgreSQL)

### 특수 검증

| 항목 | 검증 방법 |
|------|-----------|
| 실시간 협업 | 두 브라우저 탭 동시 편집 후 상태 일치 확인 |
| DDL 유효성 | 생성된 DDL을 실제 DB에서 실행 |
| 성능 | 100개 테이블 ERD 렌더링/동기화 지연 측정 |

---

## 9. 비기능 요구사항

| 항목 | 요구사항 |
|------|----------|
| 동시 사용자 | 워크스페이스당 최대 20명 |
| 응답 시간 | REST API < 500ms, WebSocket 동기화 < 100ms |
| 가용성 | 99% 업타임 (팀 내부용) |
| 보안 | JWT + HTTPS, RBAC, SQL 인젝션 방지 |
| 브라우저 지원 | Chrome/Edge/Firefox 최신 2버전 |

---

*이 문서는 ErdSketch 개발 팀의 내부 PRD입니다.*
