- 신규 세션을 시작하면 /docs/PRD.md 파일을 확인해서 현재 프로젝트 개요 확인.
- 수정 완료 된 요청사항이 /docs/PRD.md 파일 및 docs/phase/phase*.md 파일에 갱신 할 필요 있는지 검토 후 필요하면 수정.
- 화면 수정 시 사용자의 UI/UX를 충분히 고려하고 수정. 

----

## 프로젝트 개요

**ErdSketch**는 팀(5~50명)이 DB 스키마를 실시간으로 협업 설계할 수 있는 웹 기반 ERD 도구다.
React Flow 기반 캔버스 편집 + Yjs CRDT 실시간 동기화 + 다중 DB 방언 DDL 생성/파싱이 핵심이다.

---

## 현재 구현 상태 

| Phase | 상태 | 내용 |
|-------|------|------|
| Phase 1 | ✅ 완료 | JWT 인증, 워크스페이스/프로젝트 CRUD, ERD 캔버스, 테이블/컬럼/관계 편집, PostgreSQL DDL 생성/파싱 |
| Phase 2 | 🚧 진행 예정 | Yjs 실시간 협업, WebSocket 릴레이, 프레즌스/커서 |
| Phase 3 | 🚧 진행 예정 | MySQL/Oracle/MSSQL DDL, JSON/PNG/SVG 내보내기, DDL 프리뷰 |
| Phase 4 | 🚧 진행 예정 | 버전 히스토리, 댓글, 스키마 Diff, 오프라인, PDF |
| Phase 5 | 🚧 진행 예정 | OAuth, 다크 모드, 팔로우 모드, DBML, JDBC |

각 Phase의 상세 테스트 계획은 `docs/phase/phase{N}.md` 참조.

---

## 소스 구조

```
frontend/src/
  api/            # REST API 클라이언트 (TanStack Query)
  components/     # React 컴포넌트 (canvas, editor, toolbar, collaboration, ddl, project, auth, common)
  crdt/           # Yjs 문서 설정 및 Provider
  hooks/          # 커스텀 React 훅
  models/         # TypeScript 인터페이스
  store/          # Zustand 스토어
  utils/          # 유틸리티 (타입 매핑 등)

backend/src/main/java/com/erdsketch/
  auth/           # JWT 인증/인가
  workspace/      # 워크스페이스 CRUD
  project/        # 프로젝트 CRUD
  document/       # ERD 문서 CRUD
  collaboration/  # WebSocket Yjs 릴레이
  ddl/            # generator/, parser/, diff/ — 방언별 DDL 처리
  comment/        # 댓글 시스템
  version/        # 문서 버전 관리
  diff/           # 스키마 Diff
  config/         # Spring 설정 (Security, WebSocket 등)
  user/           # 사용자 프로필
  export/         # 내보내기 서비스 (PDF, PNG, JSON 등)
  oauth/          # OAuth2 연동
  jdbc/           # 라이브 DB 역공학 (Phase 5)
  dbml/           # DBML 호환 (Phase 5)
  template/       # 프로젝트 템플릿 (Phase 5)
  pdf/            # PDF 내보내기 (Phase 4)
```

---

## 핵심 아키텍처 결정사항

### 이중 저장 전략 (중요)
- `yjs_state BYTEA`: 실시간 협업의 진실 소스(Yjs 바이너리 상태)
- `schema_snapshot JSONB`: 읽기 캐시 — DDL 생성/검색/diff 용도. Java에서 Yjs 역직렬화 불필요

### CRDT 문서 구조
```
Y.Map (root) → tables: Y.Map<id, Y.Map> / relationships: Y.Map / notes: Y.Map
```
- 모든 협업 상태는 Yjs 문서를 통해 동기화
- Awareness로 커서/선택 상태 전파 (저장 불필요)

### DB 방언
지원 대상: `MYSQL` | `POSTGRESQL` | `ORACLE` | `MSSQL`
프로젝트 생성 시 `target_db_type` 고정. DDL 생성/파싱은 방언별 전략 패턴 적용.

### 권한 모델
`OWNER > ADMIN > MEMBER > VIEWER` — 워크스페이스 단위 RBAC. Spring Security + JWT.

---

## 개발 규칙

### 백엔드
- **Java 21**, **Spring Boot 3.x**, **PostgreSQL 15+**
- DDL 파싱은 **JSqlParser** 사용 (직접 파서 작성 금지)
- DB 마이그레이션은 **Flyway** (`backend/src/main/resources/db/migration/`)
- 테스트: JUnit 5 + Spring Boot Test + **H2 인메모리** (로컬 단위/통합 테스트), Testcontainers (PostgreSQL, 무거운 통합 테스트)
  - H2 테스트 환경은 `/test/resources/application-test.yml`에서 관리
  - H2 호환 이슈 주의: `columnDefinition=BYTEA` 같은 DB 특화 타입은 별도 처리 필요

### 프론트엔드
- **React 18**, **TypeScript 5+**, **Vite**
- 스타일: **Tailwind CSS** (커스텀 CSS 최소화)
- 캔버스: **React Flow** 커스텀 노드/엣지 패턴 사용
- 상태: 로컬 UI → **Zustand**, 서버 데이터 → **TanStack Query**, 협업 → **Yjs**
- 테스트: Vitest + React Testing Library (단위), Playwright (E2E)

### 공통
- API 경로 prefix: `/api/v1/`
- WebSocket: `/ws/documents/{documentId}` (Yjs sync + Awareness 바이너리)
- 새 기능 구현 전 해당 Phase의 `docs/phase/phase{N}.md` 테스트 케이스 먼저 확인

---

## 주요 참조 파일

| 파일 | 용도 |
|------|------|
| `docs/PRD.md` | 전체 기능 명세, API 명세, 데이터 모델 |
| `docs/phase/phase1.md` | Phase 1 테스트 케이스 (완료) |
| `docs/phase/phase2.md` | Phase 2 테스트 케이스 |
| `docs/phase/phase3.md` | Phase 3 테스트 케이스 |
| `docs/phase/phase4.md` | Phase 4 테스트 케이스 |
| `docs/phase/phase5.md` | Phase 5 테스트 케이스 |
| `docker-compose.yml` | 로컬 개발용 PostgreSQL 등 인프라 |
