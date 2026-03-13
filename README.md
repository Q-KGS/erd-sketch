# ErdSketch

웹 기반 ERD 설계 및 실시간 협업 도구

팀(5~20명)이 DB 스키마를 시각적으로 설계하고, CRDT 기반 충돌 없는 동시 편집으로 함께 작업할 수 있는 ERD 에디터입니다.

---

## 주요 기능

- **ERD 캔버스** — React Flow 기반 무한 캔버스, 테이블/컬럼/관계 CRUD, 자동 레이아웃, Undo/Redo
- **실시간 협업** — Yjs CRDT로 여러 사용자가 동시에 편집, 사용자 프레즌스(아바타) 표시
- **다중 DB DDL** — MySQL, PostgreSQL, Oracle, MSSQL 방언별 DDL 생성 및 파싱
- **자동 저장** — 30초 간격으로 Yjs 상태를 DB에 스냅샷 저장
- **프로젝트 관리** — 워크스페이스/프로젝트/문서 계층, JWT 인증, RBAC(Owner/Admin/Member/Viewer)

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

### Backend

| 기술 | 용도 |
|------|------|
| Spring Boot 3.3 (Java 21) | API 서버 |
| Spring WebSocket | Yjs 바이너리 릴레이 |
| Spring Security + JWT | 인증/인가 |
| Spring Data JPA + PostgreSQL 15 | 데이터 영속성 |
| JSqlParser 4.9 | DDL 파싱 |
| Flyway | DB 마이그레이션 |

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
│       └── config/           # Security, WebSocket 설정
├── frontend/                 # React + TypeScript
│   └── src/
│       ├── components/       # canvas, editor, toolbar, collaboration, ddl, auth
│       ├── hooks/            # useYjs, useSchema
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

### 백엔드 (64개 테스트)

```bash
cd backend
JAVA_HOME="/path/to/java21" ./mvnw test -Dspring.profiles.active=test
```

H2 인메모리 DB(PostgreSQL 호환 모드)로 실행됩니다. Testcontainers 불필요.

### 프론트엔드 (39개 테스트)

```bash
cd frontend
npm test -- --run
```

Vitest + jsdom + React Testing Library 사용.

---

## API 개요

### 인증

| Method | Path | 설명 |
|--------|------|------|
| POST | `/api/v1/auth/register` | 회원가입 |
| POST | `/api/v1/auth/login` | 로그인 (JWT 발급) |
| POST | `/api/v1/auth/refresh` | 토큰 갱신 |
| GET | `/api/v1/auth/me` | 현재 사용자 정보 |

### 리소스

| Method | Path | 설명 |
|--------|------|------|
| POST/GET | `/api/v1/workspaces` | 워크스페이스 생성/목록 |
| POST/GET | `/api/v1/workspaces/{id}/projects` | 프로젝트 생성/목록 |
| POST/GET | `/api/v1/projects/{id}/documents` | 문서 생성/목록 |
| POST | `/api/v1/documents/{id}/ddl/generate` | DDL 생성 |
| POST | `/api/v1/documents/{id}/ddl/parse` | DDL → ERD 파싱 |

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
| Phase 3 | 8~10주 | 다중 DB 방언, JSON/PNG/SVG 내보내기, DDL 프리뷰 패널 | 예정 |
| Phase 4 | 11~13주 | 버전 히스토리, 댓글, 스키마 Diff, 오프라인 지원 | 예정 |
| Phase 5 | 14주+ | OAuth, 다크 모드, DBML 호환, 팔로우 모드 | 예정 |

---

## 비기능 요구사항

| 항목 | 목표 |
|------|------|
| 동시 사용자 | 워크스페이스당 최대 20명 |
| REST 응답 시간 | < 500ms |
| WebSocket 동기화 지연 | < 100ms (로컬 네트워크 기준 < 200ms) |
| 브라우저 지원 | Chrome / Edge / Firefox 최신 2버전 |
