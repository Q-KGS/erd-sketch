# Phase 5 테스트 계획 — 선택 기능 (14주+)

## 전제 조건

Phase 1–4 완료 기준 충족

## 범위

| 구현 항목 | 설명 |
|-----------|------|
| OAuth 로그인 | Google / GitHub OAuth2 |
| 다크 모드 | Tailwind dark 클래스 전환 |
| DBML 호환 | dbdiagram.io 형식 가져오기/내보내기 |
| 팔로우 모드 | 특정 사용자 뷰포트 따라가기 |
| 프로젝트 템플릿 | e-commerce, blog 등 스타터 스키마 |
| JDBC 연결 | 라이브 DB에서 직접 스키마 역공학 |

---

## 1. 백엔드 단위/통합 테스트

### 1.1 OAuth 인증 (`OAuthService`)

| # | 테스트 케이스 | 기대 결과 |
|---|--------------|-----------|
| B-OAUTH-01 | Google OAuth 콜백 처리 | JWT 발급, User 레코드 생성 또는 업데이트 |
| B-OAUTH-02 | GitHub OAuth 콜백 처리 | JWT 발급, User 레코드 생성 또는 업데이트 |
| B-OAUTH-03 | 기존 이메일 계정과 OAuth 연동 | 동일 이메일이면 기존 User에 연결 |
| B-OAUTH-04 | OAuth 토큰 위변조 | HTTP 401 |
| B-OAUTH-05 | OAuth provider 없는 경로 | HTTP 400 |
| B-OAUTH-06 | 이미 연동된 계정 재연동 | 정상 로그인, 기존 user_id 유지 |

### 1.2 DBML 파서/생성기 (`DbmlService`)

| # | 테스트 케이스 | 입력 | 기대 결과 |
|---|--------------|------|-----------|
| B-DBML-01 | DBML 가져오기 — 테이블 | `Table users { id int [pk] }` | `tables[0].name = "users"`, `id` PK |
| B-DBML-02 | DBML 가져오기 — 참조 | `Ref: posts.user_id > users.id` | `relationships` 배열에 추가 |
| B-DBML-03 | DBML 가져오기 — 1:1 참조 | `Ref: profiles.user_id - users.id` | `cardinality: "1:1"` |
| B-DBML-04 | DBML 가져오기 — 필드 옵션 | `email varchar [not null, unique]` | `nullable: false`, `is_unique: true` |
| B-DBML-05 | DBML 가져오기 — 기본값 | `status varchar [default: 'active']` | `default_value: "'active'"` |
| B-DBML-06 | DBML 내보내기 | TableDef 리스트 | 유효한 DBML 문자열 |
| B-DBML-07 | DBML 내보내기 — FK | RelationshipDef | `Ref:` 구문 포함 |
| B-DBML-08 | 잘못된 DBML | `Table { }` | `warnings` 에 에러, 부분 결과 |

```java
@Test
void parseDbml_withReference_createsRelationship() {
    String dbml = """
        Table posts {
          id int [pk]
          user_id int [not null]
        }
        Ref: posts.user_id > users.id [delete: cascade]
        """;
    DbmlParseResult result = dbmlService.parse(dbml);
    assertThat(result.relationships()).hasSize(1);
    assertThat(result.relationships().get(0).cardinality()).isEqualTo("1:N");
    assertThat(result.relationships().get(0).onDelete()).isEqualTo("CASCADE");
}
```

### 1.3 JDBC 역공학 (`JdbcSchemaExtractor`)

| # | 테스트 케이스 | 기대 결과 |
|---|--------------|-----------|
| B-JDBC-01 | PostgreSQL 연결 성공 | 스키마 추출 성공 |
| B-JDBC-02 | MySQL 연결 성공 | 스키마 추출 성공 |
| B-JDBC-03 | 잘못된 연결 정보 | HTTP 400, 연결 실패 메시지 |
| B-JDBC-04 | 인증 실패 | HTTP 400 |
| B-JDBC-05 | 테이블 목록 추출 | `information_schema` 기반 테이블명 목록 |
| B-JDBC-06 | 컬럼 정보 추출 | 컬럼명, 타입, nullable, PK 정확히 반영 |
| B-JDBC-07 | FK 정보 추출 | 관계 추출 (FOREIGN KEY 메타데이터) |
| B-JDBC-08 | 연결 타임아웃 설정 | 5초 초과 시 HTTP 408 |
| B-JDBC-09 | 보안 — SQL 인젝션 방지 | 연결 파라미터 PreparedStatement 처리 |

### 1.4 프로젝트 템플릿 (`ProjectTemplateService`)

| # | 테스트 케이스 | 기대 결과 |
|---|--------------|-----------|
| B-TPL-01 | e-commerce 템플릿 적용 | `users`, `products`, `orders`, `order_items` 테이블 생성 |
| B-TPL-02 | blog 템플릿 적용 | `posts`, `comments`, `tags`, `users` 테이블 생성 |
| B-TPL-03 | 템플릿 테이블에 FK 관계 포함 | 관계 엣지 생성 |
| B-TPL-04 | 빈 프로젝트에 템플릿 적용 | 기존 테이블 없음 + 템플릿 추가 |
| B-TPL-05 | 기존 테이블에 템플릿 병합 | 충돌 처리 (테이블명 중복 시 suffix 추가) |

---

## 2. 프론트엔드 단위 테스트

### 2.1 다크 모드

| # | 테스트 케이스 | 기대 결과 |
|---|--------------|-----------|
| F-DARK-01 | 토글 전환 | `<html>` 에 `class="dark"` 추가/제거 |
| F-DARK-02 | 설정 저장 | localStorage에 `theme: dark/light` 저장 |
| F-DARK-03 | 페이지 새로고침 후 유지 | 저장된 테마 자동 적용 |
| F-DARK-04 | 시스템 다크모드 감지 | `prefers-color-scheme: dark` 초기 적용 |
| F-DARK-05 | TableNode 다크 스타일 | 배경, 텍스트 색상 다크 테마 적용 |

### 2.2 팔로우 모드

| # | 테스트 케이스 | 기대 결과 |
|---|--------------|-----------|
| F-FOLLOW-01 | 팔로우 시작 | 상대방 뷰포트로 캔버스 이동 |
| F-FOLLOW-02 | 팔로우 중 상대방 이동 | 내 뷰포트 자동 추적 |
| F-FOLLOW-03 | 팔로우 중 내 입력 | 팔로우 자동 해제 |
| F-FOLLOW-04 | 팔로우 상대방 오프라인 | 팔로우 자동 해제, 알림 토스트 |

---

## 3. E2E 시나리오

### 시나리오 E2E-OAUTH-01: Google OAuth 로그인

```
1. /login 페이지에서 "Google로 로그인" 버튼 클릭
2. Google 인증 페이지로 리다이렉트
3. Google 계정 선택 및 권한 허용
4. 앱으로 콜백 리다이렉트
5. 대시보드 진입 확인
6. /auth/me 응답에서 사용자 이메일이 Google 이메일과 일치 확인
```

**검증 포인트:**
- [ ] 콜백 URL이 올바른 state 파라미터 포함
- [ ] JWT 토큰 정상 발급
- [ ] 처음 로그인 시 신규 User 생성
- [ ] 재로그인 시 기존 User 재사용 (같은 `user.id`)

### 시나리오 E2E-OAUTH-02: GitHub OAuth 로그인

```
(Google과 동일한 흐름, GitHub 제공자 사용)
```

**추가 검증:**
- [ ] GitHub `display_name`이 GitHub 사용자명으로 설정

### 시나리오 E2E-DARK-01: 다크 모드 전환

```
1. 로그인 후 설정 메뉴 → "다크 모드" 토글
2. 전체 UI가 어두운 테마로 전환 확인
3. 페이지 새로고침 후 다크 모드 유지 확인
4. 에디터 페이지에서 캔버스, 패널, 툴바 다크 테마 확인
5. 다시 토글 → 라이트 모드 복원
```

**검증 포인트:**
- [ ] 배경색 `bg-gray-50` → `dark:bg-gray-900` 변경
- [ ] 텍스트 가독성 유지 (명도 대비 4.5:1 이상)
- [ ] TableNode 노드 배경 다크 적용

### 시나리오 E2E-DBML-01: DBML 가져오기

```
1. DBML 가져오기 모달 열기
2. 다음 DBML 붙여넣기:
   Table users {
     id int [pk, increment]
     username varchar [not null, unique]
     email varchar [not null, unique]
   }
   Table posts {
     id int [pk, increment]
     title varchar [not null]
     author_id int [ref: > users.id]
   }
3. 가져오기 실행
4. 캔버스에 users, posts 테이블 생성 확인
5. users → posts FK 관계 확인
```

**검증 포인트:**
- [ ] `users` 테이블: id(PK), username(UNIQUE, NOT NULL), email(UNIQUE, NOT NULL)
- [ ] `posts` 테이블: id(PK), title(NOT NULL), author_id
- [ ] `1:N` 관계 엣지 표시

### 시나리오 E2E-DBML-02: DBML 내보내기 → dbdiagram.io 호환성

```
1. ERD에서 3개 테이블, FK 관계 2개 생성
2. "내보내기 → DBML" 클릭
3. 다운로드된 .dbml 파일을 dbdiagram.io에 붙여넣기
4. 동일한 ERD 구조 렌더링 확인
```

**검증 포인트:**
- [ ] dbdiagram.io에서 파싱 에러 없음
- [ ] 테이블 수, FK 관계 수 일치

### 시나리오 E2E-FOLLOW-01: 팔로우 모드

```
1. 탭A, 탭B 모두 같은 ERD 문서 접속
2. 탭A: 프레즌스 아바타에서 탭B 사용자 클릭 → "따라가기" 선택
3. 탭B: 캔버스를 500px 오른쪽으로 이동
4. 탭A: 뷰포트가 자동으로 탭B와 같은 위치로 이동 확인
5. 탭A: 마우스 휠 줌 → 팔로우 자동 해제 확인
```

**검증 포인트:**
- [ ] 팔로우 중 상단에 "○○님을 따라가는 중" 배너 표시
- [ ] 뷰포트 이동 < 100ms 지연
- [ ] 팔로우 해제 후 독립적 뷰포트 제어 가능

### 시나리오 E2E-TPL-01: e-commerce 템플릿 적용

```
1. 새 ERD 문서 생성 (빈 상태)
2. "템플릿 적용 → e-commerce" 클릭
3. 다음 테이블 자동 생성 확인:
   - users (id, email, name, created_at)
   - products (id, name, price, stock, category_id)
   - categories (id, name, parent_id)
   - orders (id, user_id, status, total, created_at)
   - order_items (id, order_id, product_id, quantity, price)
4. FK 관계 확인
5. 자동 레이아웃 적용 확인
```

**검증 포인트:**
- [ ] 5개 테이블 생성
- [ ] 4개 FK 관계 (orders→users, order_items→orders, order_items→products, products→categories)
- [ ] 각 테이블 최소 감사 컬럼 포함 (created_at)

### 시나리오 E2E-JDBC-01: 라이브 DB 역공학

```
1. JDBC 연결 모달 열기
2. 로컬 PostgreSQL 연결 정보 입력:
   - host: localhost, port: 5432
   - database: test_db, user: test, password: test
3. "연결 테스트" → 성공 확인
4. "스키마 가져오기" 실행
5. test_db의 테이블들이 ERD 캔버스에 표시 확인
```

**검증 포인트:**
- [ ] 연결 테스트 성공 토스트
- [ ] 가져온 테이블 수 = DB 실제 테이블 수
- [ ] 컬럼 타입, NOT NULL, PK 정확히 반영
- [ ] FK 관계 엣지 자동 생성

---

## 4. 보안 테스트

### JDBC 연결 보안

| # | 테스트 케이스 | 기대 결과 |
|---|--------------|-----------|
| SEC-01 | 내부 네트워크 IP 차단 | `192.168.x.x`, `10.x.x.x` 허용 여부 정책 적용 |
| SEC-02 | SSRF 방지 | `host: localhost`, `host: 127.0.0.1` 등 차단 (프로덕션) |
| SEC-03 | 연결 정보 로그 미노출 | 서버 로그에 패스워드 미출력 |
| SEC-04 | SQL 인젝션 시도 | `database: "test'; DROP TABLE users; --"` → 에러 처리 |

### OAuth 보안

| # | 테스트 케이스 | 기대 결과 |
|---|--------------|-----------|
| SEC-05 | OAuth state 파라미터 검증 | CSRF 방지, state 불일치 시 거부 |
| SEC-06 | OAuth code 재사용 | 한 번 사용된 code 재사용 시 거부 |

---

## 5. 성능 기준

| 기능 | 목표 |
|------|------|
| 팔로우 모드 뷰포트 동기화 | < 100ms |
| DBML 100개 테이블 파싱 | < 2초 |
| JDBC 역공학 (100개 테이블 DB) | < 10초 |
| 다크 모드 전환 애니메이션 | < 200ms |
| 템플릿 적용 후 렌더링 | < 1초 |

---

## 6. 브라우저 호환성 체크리스트

모든 Phase 5 기능을 다음 환경에서 검증:

| 브라우저 | 버전 |
|---------|------|
| Chrome | 최신 2버전 |
| Edge | 최신 2버전 |
| Firefox | 최신 2버전 |

| 기능 | Chrome | Edge | Firefox |
|------|--------|------|---------|
| OAuth 팝업/리다이렉트 | ☐ | ☐ | ☐ |
| 다크 모드 CSS | ☐ | ☐ | ☐ |
| DBML 파일 업로드 | ☐ | ☐ | ☐ |
| 팔로우 모드 부드러운 이동 | ☐ | ☐ | ☐ |
| JDBC 결과 캔버스 렌더링 | ☐ | ☐ | ☐ |

---

## 7. 완료 기준 (Definition of Done)

- [ ] OAuth Google/GitHub E2E 플로우 수동 검증 완료
- [x] DBML 가져오기/내보내기 단위 테스트 (B-DBML-01 ~ B-DBML-08) 통과
- [x] OAuth 단위 테스트 (B-OAUTH-01 ~ B-OAUTH-06) 통과
- [x] JDBC 스키마 추출 단위 테스트 (B-JDBC-01 ~ B-JDBC-09) 통과 (H2 기반)
- [x] 프로젝트 템플릿 단위 테스트 (B-TPL-01 ~ B-TPL-05) 통과
- [x] 다크 모드 단위 테스트 (F-DARK-01 ~ F-DARK-05) 통과
- [x] 팔로우 모드 단위 테스트 (F-FOLLOW-01 ~ F-FOLLOW-04) 통과
- [ ] DBML 내보내기 파일을 dbdiagram.io에서 에러 없이 로드 확인
- [ ] 다크 모드 에디터 전체 페이지 시각적 검토 완료
- [ ] E2E-TPL-01 (e-commerce 템플릿) FK 관계 포함 정상 생성
- [ ] JDBC 역공학 로컬 DB 검증 완료 (보안 정책 포함)
- [ ] 팔로우 모드 < 100ms 동기화 확인
- [ ] Chrome/Edge/Firefox 호환성 체크리스트 완료
