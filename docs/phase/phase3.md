# Phase 3 테스트 계획 — 다중 방언 & 내보내기 (8–10주)

## 전제 조건

Phase 1, 2 완료 기준 충족

## 범위

| 구현 항목 | 설명 |
|-----------|------|
| MySQL DDL 생성기 | backtick 식별자, AUTO_INCREMENT, InnoDB |
| Oracle DDL 생성기 | NUMBER, VARCHAR2, SEQUENCE/TRIGGER 기반 PK |
| MSSQL DDL 생성기 | 대괄호 식별자, IDENTITY, NVARCHAR |
| DDL 파서 (역공학) | JSqlParser로 SQL → ERD 테이블/관계 변환 |
| JSON 내보내기/가져오기 | ERD 스키마 백업/복원 |
| PNG/SVG 내보내기 | 클라이언트 html-to-image |
| DDL 프리뷰 패널 | CodeMirror SQL 구문 강조, 실시간 갱신 |

---

## 1. 백엔드 단위/통합 테스트

### 1.1 MySQL DDL 생성기 (`MySqlDdlGenerator`)

| # | 테스트 케이스 | 기대 DDL 요소 |
|---|--------------|--------------|
| B-MYSQL-01 | 기본 테이블 | `` CREATE TABLE `users` ``, `ENGINE=InnoDB`, `DEFAULT CHARSET=utf8mb4` |
| B-MYSQL-02 | AUTO_INCREMENT PK | `INT AUTO_INCREMENT`, `PRIMARY KEY (\`id\`)` |
| B-MYSQL-03 | BIGINT AUTO_INCREMENT | `BIGINT AUTO_INCREMENT` |
| B-MYSQL-04 | BOOLEAN 타입 | `TINYINT(1)` 로 변환 |
| B-MYSQL-05 | UUID 타입 | `VARCHAR(36)` 으로 변환 |
| B-MYSQL-06 | JSONB 타입 | `JSON` 으로 변환 |
| B-MYSQL-07 | TEXT 타입 | `TEXT` 유지 |
| B-MYSQL-08 | TIMESTAMP 타입 | `DATETIME` 으로 변환 |
| B-MYSQL-09 | includeDrops | `DROP TABLE IF EXISTS \`users\`` 선행 |
| B-MYSQL-10 | 테이블 설명 | `` COMMENT='...' `` 테이블 옵션 |
| B-MYSQL-11 | `SET FOREIGN_KEY_CHECKS` | 시작/끝에 0/1 설정 |
| B-MYSQL-12 | 컬럼 주석 | `` COMMENT 'text' `` 컬럼 정의 인라인 |

```java
@Test
void generate_autoIncrementBigint_producesBigintAutoIncrement() {
    Map<String, Object> schema = buildSchema("products",
        List.of(col("id", "BIGINT", true, false, false, true)));
    String ddl = mysqlGenerator.generate(schema, null, false, new ArrayList<>());
    assertThat(ddl).contains("BIGINT AUTO_INCREMENT")
                   .contains("ENGINE=InnoDB");
}
```

### 1.2 Oracle DDL 생성기 (`OracleDdlGenerator`)

| # | 테스트 케이스 | 기대 DDL 요소 |
|---|--------------|--------------|
| B-ORA-01 | 기본 테이블 | `CREATE TABLE "users" (` |
| B-ORA-02 | NUMBER 타입 | `NUMBER` 유지 |
| B-ORA-03 | VARCHAR → VARCHAR2 변환 | `VARCHAR2(255)` |
| B-ORA-04 | BOOLEAN → NUMBER(1) | `NUMBER(1)` |
| B-ORA-05 | UUID → VARCHAR2(36) | `VARCHAR2(36)` |
| B-ORA-06 | BIGINT AUTO_INCREMENT | `SEQUENCE` + `TRIGGER` 생성 또는 `GENERATED ALWAYS AS IDENTITY` (Oracle 12c+) |
| B-ORA-07 | 식별자 30자 초과 | `warnings` 에 경고 추가 |
| B-ORA-08 | JSONB → CLOB | `CLOB` 으로 변환 |

### 1.3 MSSQL DDL 생성기 (`MssqlDdlGenerator`)

| # | 테스트 케이스 | 기대 DDL 요소 |
|---|--------------|--------------|
| B-MSSQL-01 | 기본 테이블 | `CREATE TABLE [users] (` |
| B-MSSQL-02 | IDENTITY PK | `[id] BIGINT IDENTITY(1,1)` |
| B-MSSQL-03 | NVARCHAR 타입 | `NVARCHAR` 유지 |
| B-MSSQL-04 | BOOLEAN → BIT | `BIT` |
| B-MSSQL-05 | UUID → UNIQUEIDENTIFIER | `UNIQUEIDENTIFIER` |
| B-MSSQL-06 | JSONB → NVARCHAR(MAX) | `NVARCHAR(MAX)` |
| B-MSSQL-07 | 대괄호 식별자 이스케이프 | `[order]` (예약어 처리) |
| B-MSSQL-08 | 테이블 주석 | `sp_addextendedproperty N'MS_Description'` 테이블 레벨 |
| B-MSSQL-09 | 컬럼 주석 | `sp_addextendedproperty N'MS_Description'` 컬럼 레벨 |

### 1.4 DDL 파서 (`DdlParserService`)

| # | 테스트 케이스 | 입력 DDL | 기대 결과 |
|---|--------------|---------|-----------|
| B-PARSE-01 | 단순 CREATE TABLE | `CREATE TABLE users (id BIGINT PRIMARY KEY, email VARCHAR(255) NOT NULL)` | `tables[0].name = "users"`, 컬럼 2개 |
| B-PARSE-02 | PK 컬럼 추출 | `id BIGINT NOT NULL, PRIMARY KEY (id)` | `is_primary_key: true` |
| B-PARSE-03 | 복합 PK | `PRIMARY KEY (order_id, product_id)` | 두 컬럼 모두 `is_primary_key: true` |
| B-PARSE-04 | FK REFERENCES 추출 | `FOREIGN KEY (user_id) REFERENCES users(id)` | `relationships` 배열에 관계 추가 |
| B-PARSE-05 | FK ON DELETE CASCADE | `ON DELETE CASCADE` | `on_delete: "CASCADE"` |
| B-PARSE-06 | UNIQUE 제약 | `email VARCHAR(255) UNIQUE` | `is_unique: true` |
| B-PARSE-07 | NOT NULL | `name VARCHAR(100) NOT NULL` | `nullable: false` |
| B-PARSE-08 | DEFAULT 값 | `status VARCHAR(20) DEFAULT 'active'` | `default_value: "'active'"` |
| B-PARSE-09 | AUTO_INCREMENT | `id INT AUTO_INCREMENT` | `is_auto_increment: true` |
| B-PARSE-10 | 여러 테이블 한 번에 | 3개 CREATE TABLE | `tables` 배열 3개 |
| B-PARSE-11 | 주석 포함 DDL | `-- 사용자 테이블\nCREATE TABLE ...` | 주석 무시하고 파싱 |
| B-PARSE-12 | 잘못된 SQL | `CREATE TABL users` | `warnings` 에 에러 메시지, 부분 결과 반환 |
| B-PARSE-13 | MySQL backtick 식별자 | `` CREATE TABLE `users` `` | 정상 파싱 |
| B-PARSE-14 | MSSQL 대괄호 식별자 | `CREATE TABLE [users]` | 정상 파싱 |

```java
@Test
void parse_foreignKeyReferences_createsRelationship() throws Exception {
    String ddl = """
        CREATE TABLE posts (
            id BIGINT PRIMARY KEY,
            user_id BIGINT NOT NULL,
            FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
        );
        """;
    DdlParseResponse result = parser.parse(ddl, DbType.POSTGRESQL);
    assertThat(result.relationships()).hasSize(1);
    assertThat(result.relationships().get(0).get("onDelete")).isEqualTo("CASCADE");
}
```

---

## 2. 프론트엔드 단위 테스트

### 2.1 DDL 프리뷰 패널 (`DdlPreviewPanel`)

| # | 테스트 케이스 | 기대 결과 |
|---|--------------|-----------|
| F-DDL-01 | 패널 렌더링 | DDL 텍스트 또는 "테이블을 추가하면..." 표시 |
| F-DDL-02 | 방언 변경 | `generateDdl` 재호출 |
| F-DDL-03 | "복사" 클릭 | `navigator.clipboard.writeText` 호출 |
| F-DDL-04 | "다운로드" 클릭 | `<a>` 요소 생성 및 클릭 |
| F-DDL-05 | `warnings` 존재 | 노란 경고 배너 표시 |

### 2.2 JSON 내보내기/가져오기

| # | 테스트 케이스 | 기대 결과 |
|---|--------------|-----------|
| F-JSON-01 | 스키마 JSON 직렬화 | `{ tables: {...}, relationships: {...}, notes: {...} }` 구조 |
| F-JSON-02 | JSON 역직렬화 후 Yjs 적용 | 가져온 테이블이 캔버스에 표시 |
| F-JSON-03 | 손상된 JSON 가져오기 | 에러 토스트 표시, 기존 스키마 유지 |

### 2.3 PNG 내보내기

| # | 테스트 케이스 | 기대 결과 |
|---|--------------|-----------|
| F-PNG-01 | 캔버스 캡처 | `html-to-image` `toPng` 호출 |
| F-PNG-02 | 다운로드 파일명 | `erdsketch_export_YYYYMMDD.png` 형식 |
| F-PNG-03 | 빈 캔버스 내보내기 | 빈 이미지 생성 (에러 없음) |

---

## 3. E2E 시나리오

### 시나리오 E2E-DDL-01: 다중 방언 DDL 생성 비교

```
1. ERD 에디터에서 다음 테이블 생성:
   - "users": id(BIGINT, PK, AI), email(VARCHAR, NOT NULL, UNIQUE), created_at(TIMESTAMP)
   - "posts": id(BIGINT, PK, AI), title(VARCHAR), user_id(BIGINT)
2. users.id → posts.user_id FK 관계 생성 (1:N, ON DELETE CASCADE)
3. DDL 패널에서 PostgreSQL 방언 선택 → DDL 확인
4. MySQL 방언으로 변경 → DDL 변경 확인
5. Oracle 방언으로 변경 → DDL 변경 확인
6. MSSQL 방언으로 변경 → DDL 변경 확인
```

**각 방언별 검증 포인트:**

| 방언 | 식별자 | ID 타입 | AI 방식 | FK |
|------|--------|---------|---------|-----|
| PostgreSQL | `"users"` | `BIGSERIAL` | SERIAL | `ALTER TABLE ... ADD CONSTRAINT fk_...` |
| MySQL | `` `users` `` | `BIGINT AUTO_INCREMENT` | AUTO_INCREMENT | (FK는 별도 또는 테이블 내) |
| Oracle | `"users"` | `NUMBER` | SEQUENCE/IDENTITY | `ALTER TABLE ... ADD CONSTRAINT` |
| MSSQL | `[users]` | `BIGINT IDENTITY(1,1)` | IDENTITY | `ALTER TABLE ... ADD CONSTRAINT` |

### 시나리오 E2E-DDL-02: DDL 파싱 → ERD 자동 생성

```
1. DDL 가져오기 모달 열기
2. 다음 PostgreSQL DDL 입력:
   CREATE TABLE customers (
     id BIGSERIAL PRIMARY KEY,
     name VARCHAR(100) NOT NULL,
     email VARCHAR(255) UNIQUE
   );
   CREATE TABLE orders (
     id BIGSERIAL PRIMARY KEY,
     customer_id BIGINT NOT NULL REFERENCES customers(id) ON DELETE CASCADE,
     total NUMERIC(10,2),
     created_at TIMESTAMPTZ DEFAULT NOW()
   );
3. "가져오기" 실행
4. 캔버스에 customers, orders 테이블 노드 생성 확인
5. customers → orders FK 관계 엣지 생성 확인
```

**검증 포인트:**
- [ ] `customers` 테이블: 컬럼 3개 (`id` PK, `name` NOT NULL, `email` UNIQUE)
- [ ] `orders` 테이블: 컬럼 4개
- [ ] `customer_id` → `id` FK 관계 엣지 표시
- [ ] 관계 ON DELETE CASCADE 속성 반영

### 시나리오 E2E-DDL-03: MySQL DDL 실제 실행 검증

```
1. ERD에서 MySQL 방언으로 DDL 생성
2. 생성된 DDL을 로컬 MySQL에서 실행:
   mysql -u root -p test_db < exported.sql
3. 에러 없이 실행됨 확인
4. DESCRIBE users; 로 컬럼 확인
```

**검증 포인트:**
- [ ] SQL 구문 에러 없음
- [ ] 컬럼 타입, NOT NULL, AUTO_INCREMENT 적용 확인
- [ ] FK 제약 생성 확인 (`SHOW CREATE TABLE orders`)

### 시나리오 E2E-DDL-04: PostgreSQL DDL 실제 실행 검증

```bash
# docker 환경에서 실행
docker exec -i erdsketch-postgres psql -U erdsketch -c "$(cat exported_pg.sql)"
# 에러 없이 실행되어야 함
```

### 시나리오 E2E-EXPORT-01: JSON 내보내기 → 가져오기 (라운드트립)

```
1. 복잡한 ERD 생성 (5개 테이블, FK 관계 3개)
2. "내보내기 → JSON" 실행 → .json 파일 다운로드
3. 새 ERD 문서에서 "가져오기 → JSON 파일 선택"
4. 원본과 동일한 테이블/관계 복원 확인
```

**검증 포인트:**
- [ ] 테이블 수, 이름, 컬럼 수 일치
- [ ] FK 관계 수 및 카디널리티 일치
- [ ] 테이블 위치(position) 유지

### 시나리오 E2E-EXPORT-02: PNG 내보내기

```
1. 테이블 3개가 있는 ERD 캔버스
2. "내보내기 → PNG" 클릭
3. 브라우저 다운로드 확인
4. 이미지 파일 열어 테이블 노드 시각적 확인
```

**검증 포인트:**
- [ ] 파일이 유효한 PNG 포맷
- [ ] 이미지에 테이블 노드가 보임
- [ ] 배경색, 노드 색상 반영

### 시나리오 E2E-PREVIEW-01: DDL 프리뷰 패널 실시간 업데이트

```
1. DDL 패널 열기 (초기 상태: 테이블 없음)
2. 테이블 "inventory" 생성
3. DDL 패널에서 "재생성" 또는 자동 갱신 클릭
4. CREATE TABLE "inventory" 구문 표시 확인
5. 컬럼 추가
6. 패널 재생성 후 새 컬럼 포함 확인
```

---

## 4. DDL 유효성 교차 검증

> 생성된 DDL을 실제 DB 엔진에서 실행하여 문법 유효성 확인

| DB | 검증 도구 | 명령 |
|----|----------|------|
| PostgreSQL | `psql` | `psql -c "$(cat schema.sql)"` |
| MySQL | `mysql` CLI | `mysql < schema.sql` |
| Oracle | SQL*Plus / SQLcl | `@schema.sql` |
| MSSQL | `sqlcmd` | `sqlcmd -i schema.sql` |

### 검증 테이블 세트

모든 방언에서 다음 구조를 동일하게 생성:
```
category (id PK AI, name NOT NULL, description)
product (id PK AI, name NOT NULL, price DECIMAL, category_id FK→category, created_at TIMESTAMP)
tag (id PK AI, label UNIQUE NOT NULL)
product_tag (product_id FK→product, tag_id FK→tag) -- M:N 조인 테이블
```

---

## 5. 경계 조건

| 케이스 | 기대 결과 |
|--------|-----------|
| Oracle 식별자 31자 초과 | `warnings`에 `"Identifier too long"` 추가 |
| MSSQL에 `JSONB` 타입 컬럼 | `NVARCHAR(MAX)` 변환 + 경고 |
| MySQL에서 `ON UPDATE SET NULL` 단독 | `ON UPDATE SET NULL` 생성 |
| 빈 FK (source/target 컬럼 미지정) | FK 구문 생략, warnings 추가 |
| 동일 테이블명으로 DDL 파싱 | 두 번째 테이블을 병합 또는 덮어쓰기, 경고 추가 |
| 1,000줄 이상 DDL 파싱 | 30초 이내 완료 |

---

## 6. 완료 기준 (Definition of Done)

- [x] MySQL DDL 생성기 단위 테스트 통과 (B-MYSQL-01~11, 총 11개)
- [x] Oracle DDL 생성기 단위 테스트 통과 (B-ORA-01~08, 총 8개)
- [x] MSSQL DDL 생성기 단위 테스트 통과 (B-MSSQL-01~07, 총 7개)
- [x] DDL 파서 단위 테스트 B-PARSE-01 ~ B-PARSE-14 통과 (총 14개, 인라인 FK 지원 포함)
- [x] DDL 프리뷰 패널 테스트 통과 (F-DDL-01~05, 총 5개)
- [x] JSON 내보내기/가져오기 테스트 통과 (F-JSON-01~03, 총 3개)
- [x] PNG 내보내기 테스트 통과 (F-PNG-01~03, 총 3개)
- [x] 백엔드 전체 테스트 104개 통과 (`mvn test`)
- [x] 프론트엔드 전체 테스트 50개 통과 (`npm test`)
- [ ] 생성된 PostgreSQL/MySQL DDL을 실제 DB에서 에러 없이 실행 확인
- [ ] E2E-DDL-02 (DDL 파싱 → ERD 자동 생성) 검증 완료
- [ ] E2E-EXPORT-01 (JSON 라운드트립) 데이터 손실 없음
- [ ] E2E-EXPORT-02 (PNG 내보내기) 유효한 이미지 파일 생성
