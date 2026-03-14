# ErdSketch

웹 기반 ERD 설계 및 실시간 협업 도구

팀(5~50명)이 DB 스키마를 시각적으로 설계하고, CRDT 기반 충돌 없는 동시 편집으로 함께 작업할 수 있는 ERD 에디터입니다.

---

## 주요 기능

- **ERD 캔버스** — React Flow 기반 무한 캔버스, 테이블/컬럼/관계 CRUD, 자동 레이아웃, Undo/Redo
- **실시간 협업** — Yjs CRDT로 여러 사용자가 동시에 편집, 사용자 프레즌스(아바타) 표시
- **다중 DB DDL** — MySQL, PostgreSQL, Oracle, MSSQL 방언별 DDL 생성 및 파싱
- **자동 저장** — 30초 간격으로 Yjs 상태를 DB에 스냅샷 저장
- **프로젝트 관리** — 워크스페이스/프로젝트/문서 계층, JWT 인증, RBAC(Owner/Admin/Member/Viewer)
- **버전 히스토리 & 스키마 Diff** — 문서 버전 저장/복원, 버전 간 스키마 변경 비교
- **댓글 시스템** — ERD 요소별 스레드 댓글
- **오프라인 지원** — IndexedDB 캐시로 오프라인 편집 후 재연결 시 자동 병합
- **내보내기** — PDF, PNG, SVG, JSON 형식으로 ERD 내보내기
- **OAuth 로그인** — Google / GitHub 소셜 로그인
- **다크 모드** — 시스템 설정 연동 및 수동 전환
- **팔로우 모드** — 다른 사용자의 뷰포트를 실시간으로 따라가기
- **DBML 호환** — DBML 형식 가져오기/내보내기
- **JDBC 역공학** — 실제 DB에 연결해 스키마를 자동으로 ERD로 변환
- **프로젝트 템플릿** — 사전 정의된 스키마 템플릿으로 빠른 시작

---

## 기술 스택

### Frontend

| 기술 | 용도 |
|------|------|
| React 18 + TypeScript | UI 프레임워크 |
| React Flow | ERD 캔버스 |
| Yjs + y-websocket | CRDT 실시간 협업 |
| y-indexeddb | 오프라인 캐시 |
| Zustand | 로컬 UI 상태 |
| TanStack Query | REST API 데이터 페칭 |
| Tailwind CSS | 스타일링 |
| Vite | 빌드 도구 |
| html-to-image | PNG/SVG 내보내기 |
| CodeMirror | DDL 프리뷰 편집기 |
| Playwright | E2E 테스트 |

### Backend

| 기술 | 용도 |
|------|------|
| Spring Boot 3.3 (Java 21) | API 서버 |
| Spring WebSocket | Yjs 바이너리 릴레이 |
| Spring Security + JWT | 인증/인가 |
| Spring Data JPA + PostgreSQL 15 | 데이터 영속성 |
| JSqlParser 4.9 | DDL 파싱 |
| Flyway | DB 마이그레이션 |
| Apache PDFBox | PDF 내보내기 |
| OAuth2 Client | Google/GitHub 로그인 |

---

## 아키텍처

```
Browser Clients
  ├── REST (HTTPS) ──→ Spring Boot API Controllers
  │                          → Service Layer
  │                          → Spring Data JPA
  │                          → PostgreSQL
  │
  └── WebSocket ────→ YjsWebSocketHandler (바이너리 브로드캐스트)
                            → 30초 간격 SnapshotScheduler → PostgreSQL
```

### 이중 저장 전략

| 컬럼 | 타입 | 역할 |
|------|------|------|
| `yjs_state` | BYTEA | 실시간 협업 복원용 진실 소스 |
| `schema_snapshot` | JSONB | DDL 생성·검색·diff용 읽기 캐시 |

---

## 프로젝트 구조

```
erdsketch/
├── backend/                  # Spring Boot
│   └── src/main/java/com/erdsketch/
│       ├── auth/             # JWT 인증
│       ├── workspace/        # 워크스페이스 CRUD
│       ├── project/          # 프로젝트 CRUD
│       ├── document/         # ERD 문서 CRUD
│       ├── collaboration/    # WebSocket 릴레이, 스냅샷 스케줄러
│       ├── ddl/              # DDL 생성기 / 파서
│       ├── comment/          # 댓글 시스템
│       ├── version/          # 문서 버전 관리
│       ├── diff/             # 스키마 Diff
│       ├── user/             # 사용자 프로필
│       ├── export/           # 내보내기 서비스 (PDF, PNG, JSON 등)
│       ├── oauth/            # OAuth2 연동
│       ├── jdbc/             # 라이브 DB 역공학
│       ├── dbml/             # DBML 호환
│       ├── template/         # 프로젝트 템플릿
│       ├── pdf/              # PDF 내보내기
│       └── config/           # Security, WebSocket 설정
├── frontend/                 # React + TypeScript
│   └── src/
│       ├── components/       # canvas, editor, toolbar, collaboration, ddl, auth, project, comment, version, export
│       ├── hooks/            # useYjs, useSchema, useDarkMode, useFollowMode
│       ├── crdt/             # yjsProvider, schemaConverter
│       ├── store/            # Zustand 스토어
│       ├── api/              # REST 클라이언트
│       └── utils/            # 타입 매핑, 자동 레이아웃
├── docs/
│   ├── PRD.md
│   └── phase/                # phase1.md ~ phase5.md
└── docker-compose.yml
```

---

## 빠른 시작

### 사전 요구사항

- Java 21
- Node.js 20+
- Docker & Docker Compose

### 1. 인프라 실행

```bash
docker-compose up -d   # PostgreSQL 시작
```

### 2. 백엔드 실행

```bash
cd backend
./mvnw spring-boot:run
# API 서버: http://localhost:8080
```

### 3. 프론트엔드 실행

```bash
cd frontend
npm install
npm run dev
# 개발 서버: http://localhost:5173
```

---

## 테스트

### 백엔드 (181개 테스트)

```bash
cd backend
JAVA_HOME="/path/to/java21" ./mvnw test -Dspring.profiles.active=test
```

H2 인메모리 DB(PostgreSQL 호환 모드)로 실행됩니다. Testcontainers 불필요.

### 프론트엔드 (69개 테스트)

```bash
cd frontend
npm test -- --run
```

Vitest + jsdom + React Testing Library 사용.

### E2E 테스트 (Playwright)

```bash
cd frontend
npx playwright test
```

실제 백엔드 서버가 실행 중이어야 합니다. `frontend/e2e/` 디렉토리에 Phase별 시나리오 테스트가 있습니다.

---

## API 개요

### 인증

| Method | Path | 설명 |
|--------|------|------|
| POST | `/api/v1/auth/register` | 회원가입 |
| POST | `/api/v1/auth/login` | 로그인 (JWT 발급) |
| POST | `/api/v1/auth/refresh` | 토큰 갱신 |
| GET | `/api/v1/auth/me` | 현재 사용자 정보 |
| GET | `/api/v1/oauth2/authorization/{provider}` | OAuth 로그인 (google/github) |

### 리소스

| Method | Path | 설명 |
|--------|------|------|
| POST/GET | `/api/v1/workspaces` | 워크스페이스 생성/목록 |
| POST/GET | `/api/v1/workspaces/{id}/projects` | 프로젝트 생성/목록 |
| POST/GET | `/api/v1/projects/{id}/documents` | 문서 생성/목록 |
| POST | `/api/v1/documents/{id}/ddl/generate` | DDL 생성 |
| POST | `/api/v1/documents/{id}/ddl/parse` | DDL → ERD 파싱 |
| GET/POST | `/api/v1/documents/{id}/export/{format}` | 내보내기 (pdf/png/svg/json) |
| GET/POST | `/api/v1/documents/{id}/versions` | 버전 히스토리 조회/저장 |
| GET/POST | `/api/v1/documents/{id}/comments` | 댓글 조회/작성 |
| GET | `/api/v1/documents/{id}/diff` | 스키마 Diff |
| GET/POST | `/api/v1/documents/{id}/dbml` | DBML 내보내기/가져오기 |
| GET/POST | `/api/v1/jdbc/import` | JDBC 역공학 |
| GET/POST | `/api/v1/templates` | 프로젝트 템플릿 목록/생성 |

### WebSocket

```
WS /ws/documents/{documentId}
# Yjs sync + Awareness 바이너리 프로토콜
```

---

## 배포

프로덕션 환경은 Fly.io에 운영 중입니다: **https://erdsketch.fly.dev**

배포 전략, 헬스체크, 자동 롤백, 운영 명령어 등 상세 내용은 [docs/deployment.md](docs/deployment.md)를 참조하세요.

---

## 구현 로드맵

| Phase | 기간 | 내용 | 상태 |
|-------|------|------|------|
| Phase 1 | 1~4주 | 인증, 워크스페이스/프로젝트 CRUD, ERD 캔버스, 기본 DDL | ✅ 완료 |
| Phase 2 | 5~7주 | Yjs 실시간 협업, WebSocket 릴레이, 자동 저장, 프레즌스 | ✅ 완료 |
| Phase 3 | 8~10주 | 다중 DB 방언, JSON/PNG/SVG 내보내기, DDL 프리뷰 패널 | ✅ 완료 |
| Phase 4 | 11~13주 | 버전 히스토리, 댓글, 스키마 Diff, 오프라인 지원, PDF | ✅ 완료 |
| Phase 5 | 14주+ | OAuth, 다크 모드, DBML 호환, 팔로우 모드, JDBC, 템플릿, E2E | ✅ 완료 |

---

## 비기능 요구사항

| 항목 | 목표 |
|------|------|
| 동시 사용자 | 워크스페이스당 최대 50명 |
| REST 응답 시간 | < 500ms |
| WebSocket 동기화 지연 | < 100ms (로컬 네트워크 기준 < 200ms) |
| 브라우저 지원 | Chrome / Edge / Firefox 최신 2버전 |
