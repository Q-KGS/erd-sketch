# Phase 1 테스트 계획 — 기본 기능 (1–4주)

## 범위

| 구현 항목 | 설명 |
|-----------|------|
| 프로젝트 스캐폴딩 | Vite + Spring Boot 초기 환경 |
| 인증 | JWT 회원가입/로그인/토큰 갱신 |
| 워크스페이스/프로젝트 CRUD | 역할 기반 접근제어 포함 |
| ERD 캔버스 | React Flow 테이블 노드, 팬/줌 |
| 테이블/컬럼/관계 CRUD | 인라인 편집, 삭제, Undo/Redo |
| 기본 DDL | PostgreSQL DDL 생성 및 파싱 |

---

## 1. 백엔드 단위/통합 테스트

### 1.1 인증 (`AuthService`)

| # | 테스트 케이스 | 입력 | 기대 결과 |
|---|--------------|------|-----------|
| B-AUTH-01 | 정상 회원가입 | `{email: "a@b.com", password: "pass1234", displayName: "Alice"}` | HTTP 200, `user.id` 존재, `tokens.accessToken` JWT 반환 |
| B-AUTH-02 | 중복 이메일 가입 | 같은 이메일로 2회 가입 | HTTP 400, 에러 메시지 "Email already exists" |
| B-AUTH-03 | 비밀번호 8자 미만 | `password: "1234567"` | HTTP 400, validation 에러 |
| B-AUTH-04 | 정상 로그인 | 가입된 email/password | HTTP 200, accessToken + refreshToken 반환 |
| B-AUTH-05 | 잘못된 비밀번호 | 틀린 password | HTTP 401 |
| B-AUTH-06 | 존재하지 않는 이메일 로그인 | 없는 email | HTTP 401 |
| B-AUTH-07 | 토큰 갱신 | 유효한 refreshToken | HTTP 200, 새 accessToken 반환 |
| B-AUTH-08 | 만료된 refreshToken | 만료 토큰 | HTTP 401 |
| B-AUTH-09 | `/auth/me` 인증된 요청 | Bearer accessToken | HTTP 200, 사용자 정보 반환 |
| B-AUTH-10 | `/auth/me` 토큰 없음 | Authorization 헤더 없음 | HTTP 401 |

```java
// 예시: AuthServiceTest
@SpringBootTest
@Transactional
class AuthServiceTest {
    @Test
    void register_duplicateEmail_throwsException() {
        authService.register(new RegisterRequest("a@b.com", "pass1234", "Alice"));
        assertThrows(IllegalArgumentException.class,
            () -> authService.register(new RegisterRequest("a@b.com", "pass1234", "Bob")));
    }
}
```

### 1.2 워크스페이스 (`WorkspaceService`)

| # | 테스트 케이스 | 기대 결과 |
|---|--------------|-----------|
| B-WS-01 | 워크스페이스 생성 | 생성자가 OWNER 역할로 workspace_members에 자동 추가 |
| B-WS-02 | 중복 슬러그 생성 | HTTP 400 |
| B-WS-03 | 멤버가 아닌 사용자 조회 | HTTP 403 |
| B-WS-04 | VIEWER가 PATCH 요청 | HTTP 403 |
| B-WS-05 | OWNER가 아닌 사용자 DELETE | HTTP 403 |
| B-WS-06 | 이메일로 멤버 초대 | workspace_members에 추가, role 적용 |
| B-WS-07 | 없는 이메일로 초대 | HTTP 400 |
| B-WS-08 | MEMBER가 초대 시도 | HTTP 403 |
| B-WS-09 | 멤버 역할 변경 (ADMIN→MEMBER) | role 업데이트 |
| B-WS-10 | 멤버 제거 후 해당 사용자 접근 | HTTP 403 |

### 1.3 프로젝트 (`ProjectService`)

| # | 테스트 케이스 | 기대 결과 |
|---|--------------|-----------|
| B-PRJ-01 | 워크스페이스 멤버가 프로젝트 생성 | HTTP 200, `target_db_type` 저장 확인 |
| B-PRJ-02 | 워크스페이스 비멤버가 생성 시도 | HTTP 403 |
| B-PRJ-03 | `targetDbType` 유효하지 않은 값 | HTTP 400 |
| B-PRJ-04 | 프로젝트 목록 조회 | 해당 워크스페이스 프로젝트만 반환 |
| B-PRJ-05 | 프로젝트 수정 (`targetDbType` 변경) | 변경 값 반영 |
| B-PRJ-06 | 프로젝트 삭제 후 조회 | HTTP 404 |

### 1.4 ERD 문서 (`DocumentService`)

| # | 테스트 케이스 | 기대 결과 |
|---|--------------|-----------|
| B-DOC-01 | 문서 생성 | HTTP 200, `project_id` 연결 확인 |
| B-DOC-02 | 다른 워크스페이스 사용자가 문서 조회 | HTTP 403 |
| B-DOC-03 | `schema_snapshot` null인 문서 schema 조회 | null 또는 빈 응답 |
| B-DOC-04 | 문서 삭제 후 조회 | HTTP 404 |

### 1.5 DDL 생성 (`PostgreSqlDdlGenerator`)

| # | 테스트 케이스 | 입력 스키마 | 기대 DDL 요소 |
|---|--------------|------------|--------------|
| B-DDL-01 | 기본 테이블 생성 | `users` 테이블, `id BIGINT PK AI`, `email VARCHAR` | `CREATE TABLE "users"`, `BIGSERIAL`, `PRIMARY KEY ("id")` |
| B-DDL-02 | NOT NULL 컬럼 | `nullable: false` | `NOT NULL` 포함 |
| B-DDL-03 | UNIQUE 컬럼 | `is_unique: true, is_primary_key: false` | `UNIQUE` 포함 |
| B-DDL-04 | 기본값 설정 | `default_value: "now()"` | `DEFAULT now()` 포함 |
| B-DDL-05 | 복합 PK | 두 컬럼 모두 `is_primary_key: true` | `PRIMARY KEY ("col1", "col2")` |
| B-DDL-06 | includeDrops: true | - | `DROP TABLE IF EXISTS ... CASCADE` 선행 |
| B-DDL-07 | FK 관계 생성 | `source → target`, `on_delete: CASCADE` | `ALTER TABLE ... ADD CONSTRAINT fk_...`, `ON DELETE CASCADE` |
| B-DDL-08 | 인덱스 생성 | BTREE 인덱스 | `CREATE INDEX ... USING BTREE` |
| B-DDL-09 | UNIQUE 인덱스 | `is_unique: true` | `CREATE UNIQUE INDEX` |
| B-DDL-10 | 빈 스키마 DDL 생성 | `tables: {}` | `-- No schema defined yet` |
| B-DDL-11 | 특수 문자 테이블명 | `name: 'user-data'` | 식별자 이스케이프 `"user-data"` |
| B-DDL-12 | 테이블 설명 | `comment: "사용자 테이블"` | `COMMENT ON TABLE` 포함 |
| B-DDL-13 | 컬럼 주석 | `comment: "이메일 주소"` | `COMMENT ON COLUMN "t"."col" IS '...'` 포함 |

```java
// 예시: PostgreSqlDdlGeneratorTest
@Test
void generate_withPrimaryKey_includesBigserial() {
    Map<String, Object> schema = buildSchema("users",
        List.of(col("id", "BIGINT", true, false, false, true)));
    String ddl = generator.generate(schema, null, false, new ArrayList<>());
    assertThat(ddl).contains("BIGSERIAL").contains("PRIMARY KEY");
}
```

---

## 2. 프론트엔드 단위 테스트

### 2.1 스키마 변환 유틸 (`schemaConverter.ts`)

| # | 테스트 케이스 | 기대 결과 |
|---|--------------|-----------|
| F-CONV-01 | 빈 Yjs 문서 → 스키마 변환 | `{ tables: {}, relationships: {}, notes: {} }` |
| F-CONV-02 | 테이블 추가 후 변환 | `tables[id]` 존재, `name`, `columns` 포함 |
| F-CONV-03 | `addTableToYdoc` 호출 | 반환된 id로 `tablesMap.get(id)` 존재 |
| F-CONV-04 | 관계 추가 후 변환 | `relationships[id]` 존재 |
| F-CONV-05 | PK 컬럼 포함 테이블 변환 | `isPrimaryKey: true` 유지 |

```typescript
// 예시: schemaConverter.test.ts
test('addTableToYdoc creates table with default column', () => {
  const ydoc = new Y.Doc()
  const id = addTableToYdoc(ydoc, {
    name: 'users',
    position: { x: 0, y: 0 },
    columns: [],
    indexes: [],
  })
  const schema = ydocToSchema(ydoc)
  expect(schema.tables[id].name).toBe('users')
})
```

### 2.2 자동 레이아웃 (`autoLayout.ts`)

| # | 테스트 케이스 | 기대 결과 |
|---|--------------|-----------|
| F-LAYOUT-01 | 노드 없음 | 빈 배열 반환 |
| F-LAYOUT-02 | 노드 1개 | position 값 숫자로 반환 |
| F-LAYOUT-03 | 연결된 노드 2개 | source가 target 좌측 또는 상단에 위치 (LR/TB 방향) |
| F-LAYOUT-04 | 고컬럼 수 테이블 | height 증가 반영 |

### 2.3 타입 매핑 (`typeMapping.ts`)

| # | 테스트 케이스 | 기대 결과 |
|---|--------------|-----------|
| F-TYPE-01 | PostgreSQL 타입 목록 | `BIGINT`, `UUID`, `JSONB` 포함 |
| F-TYPE-02 | MySQL 타입 목록 | `BIGINT`, `DATETIME`, `JSON` 포함, `JSONB` 미포함 |
| F-TYPE-03 | Oracle 타입 목록 | `NUMBER`, `VARCHAR2` 포함 |
| F-TYPE-04 | MSSQL 타입 목록 | `NVARCHAR`, `UNIQUEIDENTIFIER` 포함 |

---

## 3. E2E 시나리오 (Playwright)

### 시나리오 E2E-01: 회원가입 → 로그인 → 대시보드 진입

```
1. /register 페이지 접근
2. 이름, 이메일, 비밀번호 입력 후 가입
3. 자동으로 대시보드(/) 리다이렉트 확인
4. 로그아웃
5. /login 페이지에서 같은 이메일/비밀번호로 로그인
6. 대시보드 진입 확인
```

**검증 포인트:**
- [ ] 가입 후 URL이 `/`로 변경
- [ ] 대시보드에 사용자 이름 표시
- [ ] 로그아웃 후 `/login` 리다이렉트
- [ ] 재로그인 후 토큰이 localStorage에 저장

### 시나리오 E2E-02: 워크스페이스 + 프로젝트 생성 → ERD 에디터 진입

```
1. 대시보드에서 "워크스페이스 만들기" 클릭
2. 이름과 슬러그 입력 후 생성
3. 사이드바에 새 워크스페이스 표시 확인
4. "새 프로젝트" 클릭
5. 프로젝트 이름과 대상 DB(PostgreSQL) 선택 후 생성
6. 생성된 프로젝트 카드 클릭
7. ERD 에디터 페이지 로드 확인
```

**검증 포인트:**
- [ ] 워크스페이스 카드 사이드바 표시
- [ ] 프로젝트 카드에 DB 타입 배지 표시 (`POSTGRESQL`)
- [ ] 에디터 URL 패턴 `/workspaces/{id}/projects/{id}/documents/{id}` 일치
- [ ] 툴바에 프로젝트 이름 표시

### 시나리오 E2E-03: 테이블 생성 및 컬럼 편집

```
1. ERD 에디터 진입
2. 캔버스 빈 영역 더블클릭 → 테이블 생성
3. 오른쪽 편집 패널에서 테이블 이름을 "users"로 변경
4. "+ 추가" 버튼으로 컬럼 추가
5. 컬럼명 "email", 타입 "VARCHAR" 입력
6. "NULL" 체크박스 해제 (NOT NULL)
7. 캔버스의 테이블 노드에 "users", "email" 컬럼 표시 확인
```

**검증 포인트:**
- [ ] 더블클릭 후 `new_table` 노드 생성
- [ ] 편집 패널 테이블명 변경이 캔버스 노드에 즉시 반영
- [ ] 컬럼 추가 후 노드 내 컬럼 행 증가
- [ ] NOT NULL 체크 해제 시 `NN` 표시

### 시나리오 E2E-04: 관계(FK) 생성

```
1. 테이블 "users" (id PK), "posts" (user_id) 생성
2. "posts" 테이블의 table-source 핸들에서 "users" 테이블의 table-target 핸들로 드래그
3. 두 테이블 사이에 엣지 생성 확인
4. 엣지에 카디널리티 레이블 표시 확인
```

**검증 포인트:**
- [ ] 드래그 후 엣지(RelationshipEdge) 렌더링
- [ ] 카디널리티 레이블 `1:N` 기본값 표시

### 시나리오 E2E-05: 테이블 삭제 (Delete 키)

```
1. 테이블 노드 클릭 (선택)
2. Delete 키 입력
3. 테이블 노드 사라짐 확인
```

**검증 포인트:**
- [ ] 노드 삭제 후 캔버스에서 제거
- [ ] 편집 패널 자동 닫힘

### 시나리오 E2E-06: 자동 레이아웃

```
1. 3개 이상의 테이블 생성 (랜덤 위치)
2. 툴바 "자동 정렬" 버튼 클릭
3. 노드 위치가 정렬됨 확인
```

**검증 포인트:**
- [ ] 클릭 후 노드 위치가 변경됨
- [ ] 노드끼리 겹치지 않음

### 시나리오 E2E-07: DDL 미리보기

```
1. "users" 테이블(id BIGINT PK, email VARCHAR NOT NULL) 생성
2. 툴바 "DDL 보기" 클릭
3. 오른쪽 DDL 패널 열림
4. CREATE TABLE "users" 구문 확인
5. 방언 드롭다운을 "MYSQL"로 변경
6. DDL 내용 변경 확인 (backtick 방식)
```

**검증 포인트:**
- [ ] DDL 패널 렌더링
- [ ] PostgreSQL: `CREATE TABLE "users"`, `BIGSERIAL` 포함
- [ ] "복사" 클릭 후 토스트 메시지 표시
- [ ] "다운로드" 클릭 후 `.sql` 파일 다운로드

---

## 4. 수동 검증 체크리스트

### 인프라

- [ ] `docker-compose up postgres -d` 실행 후 DB 접속 확인
- [ ] `mvn spring-boot:run` 실행 후 Flyway 마이그레이션 성공 로그 확인
- [ ] `npm run dev` 실행 후 `http://localhost:3000` 접속

### 보안

- [ ] Authorization 헤더 없이 `/api/v1/workspaces` 요청 → 401
- [ ] 만료된 토큰으로 요청 → 401, 클라이언트 자동 refresh 시도
- [ ] VIEWER 역할 사용자가 워크스페이스 수정 시도 → 403

### 캔버스 UX

- [ ] 마우스 휠로 줌 인/아웃 작동
- [ ] 캔버스 드래그(팬) 작동
- [ ] 미니맵 표시 및 클릭으로 뷰포트 이동
- [ ] Ctrl+Z 로 테이블 추가 undo
- [ ] Shift+클릭으로 다중 테이블 선택 후 그룹 이동

---

## 5. 경계 조건 테스트

| 케이스 | 입력 | 기대 동작 |
|--------|------|-----------|
| 테이블명 빈 문자열 | `name: ""` | DDL 생성 시 경고, 편집 패널 표시 유지 |
| 컬럼 0개 테이블 DDL | 컬럼 없는 테이블 | `-- 컬럼 없음` 주석 또는 빈 괄호 DDL |
| 슬러그 대문자 포함 | `slug: "MySlug"` | HTTP 400 (pattern validation) |
| 비밀번호 공백 포함 | `password: "pass 1234"` | 허용 (길이만 체크) |
| 테이블명 SQL 예약어 | `name: "order"` | 이스케이프 처리 → `"order"` |

---

## 6. 완료 기준 (Definition of Done)

- [x] 모든 백엔드 통합 테스트 통과 (H2 인메모리 DB) — 48개 통과 (2026-03-13)
- [x] 프론트엔드 단위 테스트 통과 (Vitest) — 20개 통과 (2026-03-13)
- [ ] E2E 시나리오 E2E-01 ~ E2E-07 수동 검증 완료
- [x] `POST /api/v1/auth/register` → `POST /api/v1/auth/login` → CRUD 플로우 전체 동작
- [ ] 생성된 PostgreSQL DDL을 실제 DB에서 `psql`로 실행하여 에러 없음 확인
- [ ] 코드 리뷰 완료

## 7. 구현 완료 내역 (2026-03-13)

### 추가 구현
- `DdlParser.java` — JSqlParser 4.9 기반 DDL 파싱 (CREATE TABLE, CREATE INDEX, ALTER TABLE FK)
- `PostgreSqlDdlGenerator` — 빈 스키마 시 `-- No schema defined yet` 반환 수정
- `GlobalExceptionHandler` — `HttpMessageNotReadableException` → 400 처리 추가
- `SecurityConfig` — 미인증 요청 시 401 응답 (`authenticationEntryPoint`) 추가

### 테스트 파일
- `src/test/resources/application-test.yml` — H2 인메모리 DB 설정 (JSONB 호환)
- `AuthServiceTest.java` — B-AUTH-01~10 (12개 테스트)
- `WorkspaceServiceTest.java` — B-WS-01~10 (11개 테스트)
- `ProjectServiceTest.java` — B-PRJ-01~06 (7개 테스트)
- `DocumentServiceTest.java` — B-DOC-01~04 (5개 테스트)
- `PostgreSqlDdlGeneratorTest.java` — B-DDL-01~13 (14개 테스트)
- `schemaConverter.test.ts` — F-CONV-01~05 (5개 테스트)
- `autoLayout.test.ts` — F-LAYOUT-01~04 (6개 테스트)
- `typeMapping.test.ts` — F-TYPE-01~04 (9개 테스트)

---

## 8. 추가 개선 사항 (후속 커밋)

### 버그 수정

| 커밋 | 내용 |
|------|------|
| `b92aa8f` | DDL 생성 시 `schema null` NPE 수정 — `DdlGenerateRequest`에 `schema` 필드 추가, 프론트엔드가 현재 스키마를 요청에 포함, 백엔드에서 요청 스키마 우선 사용 |
| `a026d1f` | H2 테스트 환경 BYTEA 호환성 수정 — `ErdDocument.yjsState`, `DocumentVersion.yjsState`에 `@Column(columnDefinition = "BYTEA")` 추가 |
| `7ce0e1f` | H2 JDBC URL에 `CREATE DOMAIN IF NOT EXISTS BYTEA AS VARBINARY` 추가 |
| `e7c7df5` | 테이블 전환 시 논리명/주석 입력 필드가 초기화되지 않는 버그 수정 — `<TableEditorPanel key={selectedTable.id} ...>` `key` prop 추가로 테이블 변경 시 컴포넌트 재마운트 |

### 기능 개선

| 커밋 | 내용 |
|------|------|
| `e7c7df5` | PostgreSQL DDL 컬럼 주석 개선 — 인라인 `-- comment` 대신 `COMMENT ON COLUMN "t"."c" IS '...'` 별도 구문으로 변경 (B-DDL-13 추가) |
| `d5ab48d` | ERD 테이블 노드 컬럼 표시 형식 개선 — 논리명 / 물리명 / 유형 / NULL여부 / 코멘트 순서의 5컬럼 그리드 레이아웃 |
| `d5ab48d` | DDL 미리보기 패널 — 선택된 테이블만 DDL 표시 (tableIds 필터링), 미선택 시 전체 표시 |

### 환경 설정 수정

| 커밋 | 내용 |
|------|------|
| `e7c7df5` | `pom.xml` — Lombok 1.18.36으로 업그레이드, `annotationProcessorPaths` 명시 추가 (JDK 25 환경에서 컴파일 오류 해결) |
