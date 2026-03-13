package com.erdsketch.ddl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;

class DdlParserTest {

    DdlParser parser;

    @BeforeEach
    void setUp() {
        parser = new DdlParser();
    }

    // ─────────────── 헬퍼 메서드 ───────────────

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> columns(Map<String, Object> table) {
        return (List<Map<String, Object>>) table.get("columns");
    }

    private Map<String, Object> findCol(Map<String, Object> table, String name) {
        return columns(table).stream()
                .filter(c -> name.equals(c.get("name")))
                .findFirst()
                .orElseThrow(() -> new AssertionError("Column not found: " + name));
    }

    // ─────────────── 테스트 케이스 ───────────────

    // B-PARSE-01: 단순 CREATE TABLE → tables[0].name = "users", 컬럼 2개
    @Test
    void B_PARSE_01_단순_CREATE_TABLE_파싱() {
        String ddl = "CREATE TABLE users (id BIGINT PRIMARY KEY, email VARCHAR(255) NOT NULL);";
        DdlParser.ParseResult result = parser.parse(ddl);

        assertThat(result.tables()).hasSize(1);
        assertThat(result.tables().get(0).get("name")).isEqualTo("users");
        assertThat(columns(result.tables().get(0))).hasSize(2);
    }

    // B-PARSE-02: PK 컬럼 추출 → isPrimaryKey: true
    @Test
    void B_PARSE_02_PK_컬럼_추출() {
        String ddl = "CREATE TABLE users (id BIGINT NOT NULL, email VARCHAR(255), PRIMARY KEY (id));";
        DdlParser.ParseResult result = parser.parse(ddl);

        Map<String, Object> table = result.tables().get(0);
        Map<String, Object> idCol = findCol(table, "id");

        assertThat(idCol.get("isPrimaryKey")).isEqualTo(true);
        assertThat(idCol.get("nullable")).isEqualTo(false);
    }

    // B-PARSE-03: 복합 PK → 두 컬럼 모두 isPrimaryKey: true
    @Test
    void B_PARSE_03_복합_PK() {
        String ddl = """
                CREATE TABLE order_items (
                    order_id BIGINT NOT NULL,
                    product_id BIGINT NOT NULL,
                    PRIMARY KEY (order_id, product_id)
                );
                """;
        DdlParser.ParseResult result = parser.parse(ddl);

        Map<String, Object> table = result.tables().get(0);
        Map<String, Object> orderIdCol = findCol(table, "order_id");
        Map<String, Object> productIdCol = findCol(table, "product_id");

        assertThat(orderIdCol.get("isPrimaryKey")).isEqualTo(true);
        assertThat(productIdCol.get("isPrimaryKey")).isEqualTo(true);
    }

    // B-PARSE-04: FK REFERENCES 추출 (인라인) → relationships 배열에 관계 추가
    @Test
    void B_PARSE_04_인라인_FK_REFERENCES_추출() {
        String ddl = """
                CREATE TABLE users (
                    id BIGINT PRIMARY KEY
                );
                CREATE TABLE posts (
                    id BIGINT PRIMARY KEY,
                    user_id BIGINT NOT NULL,
                    FOREIGN KEY (user_id) REFERENCES users(id)
                );
                """;
        DdlParser.ParseResult result = parser.parse(ddl);

        assertThat(result.tables()).hasSize(2);
        assertThat(result.relationships()).hasSize(1);

        Map<String, Object> rel = result.relationships().get(0);
        @SuppressWarnings("unchecked")
        List<String> srcCols = (List<String>) rel.get("sourceColumnNames");
        assertThat(srcCols).contains("user_id");
    }

    // B-PARSE-05: FK ON DELETE CASCADE → onDelete: "CASCADE"
    @Test
    void B_PARSE_05_FK_ON_DELETE_CASCADE() {
        String ddl = """
                CREATE TABLE users (
                    id BIGINT PRIMARY KEY
                );
                CREATE TABLE posts (
                    id BIGINT PRIMARY KEY,
                    user_id BIGINT NOT NULL,
                    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
                );
                """;
        DdlParser.ParseResult result = parser.parse(ddl);

        assertThat(result.relationships()).hasSize(1);
        assertThat(result.relationships().get(0).get("onDelete")).isEqualTo("CASCADE");
    }

    // B-PARSE-06: UNIQUE 제약 → isUnique: true
    @Test
    void B_PARSE_06_UNIQUE_제약() {
        String ddl = "CREATE TABLE users (id BIGINT PRIMARY KEY, email VARCHAR(255) UNIQUE);";
        DdlParser.ParseResult result = parser.parse(ddl);

        Map<String, Object> table = result.tables().get(0);
        Map<String, Object> emailCol = findCol(table, "email");

        assertThat(emailCol.get("isUnique")).isEqualTo(true);
    }

    // B-PARSE-07: NOT NULL → nullable: false
    @Test
    void B_PARSE_07_NOT_NULL() {
        String ddl = "CREATE TABLE users (id BIGINT PRIMARY KEY, name VARCHAR(100) NOT NULL);";
        DdlParser.ParseResult result = parser.parse(ddl);

        Map<String, Object> table = result.tables().get(0);
        Map<String, Object> nameCol = findCol(table, "name");

        assertThat(nameCol.get("nullable")).isEqualTo(false);
    }

    // B-PARSE-08: DEFAULT 값 → defaultValue 설정
    @Test
    void B_PARSE_08_DEFAULT_값() {
        String ddl = "CREATE TABLE users (id BIGINT PRIMARY KEY, status VARCHAR(20) DEFAULT 'active');";
        DdlParser.ParseResult result = parser.parse(ddl);

        Map<String, Object> table = result.tables().get(0);
        Map<String, Object> statusCol = findCol(table, "status");

        assertThat(statusCol).containsKey("defaultValue");
        assertThat(statusCol.get("defaultValue").toString()).containsIgnoringCase("active");
    }

    // B-PARSE-09: AUTO_INCREMENT → isAutoIncrement: true
    @Test
    void B_PARSE_09_AUTO_INCREMENT() {
        String ddl = "CREATE TABLE users (id INT AUTO_INCREMENT PRIMARY KEY, name VARCHAR(100));";
        DdlParser.ParseResult result = parser.parse(ddl);

        Map<String, Object> table = result.tables().get(0);
        Map<String, Object> idCol = findCol(table, "id");

        assertThat(idCol.get("isAutoIncrement")).isEqualTo(true);
    }

    // B-PARSE-10: 여러 테이블 한 번에 → tables 배열 3개
    @Test
    void B_PARSE_10_여러_테이블_한번에() {
        String ddl = """
                CREATE TABLE table1 (id BIGINT PRIMARY KEY);
                CREATE TABLE table2 (id BIGINT PRIMARY KEY);
                CREATE TABLE table3 (id BIGINT PRIMARY KEY);
                """;
        DdlParser.ParseResult result = parser.parse(ddl);

        assertThat(result.tables()).hasSize(3);
    }

    // B-PARSE-11: 주석 포함 DDL → 주석 무시하고 파싱
    @Test
    void B_PARSE_11_주석_포함_DDL() {
        String ddl = """
                -- 사용자 테이블
                CREATE TABLE users (
                    id BIGINT PRIMARY KEY, -- 기본키
                    name VARCHAR(100) NOT NULL
                );
                """;
        DdlParser.ParseResult result = parser.parse(ddl);

        assertThat(result.tables()).hasSize(1);
        assertThat(result.tables().get(0).get("name")).isEqualTo("users");
        assertThat(columns(result.tables().get(0))).hasSize(2);
    }

    // B-PARSE-12: 잘못된 SQL → warnings에 에러 메시지, 예외 발생 안 함
    @Test
    void B_PARSE_12_잘못된_SQL_warnings_에러_예외없음() {
        String ddl = "CREATE TABL users";

        // 예외 발생 없이 결과 반환
        DdlParser.ParseResult result = parser.parse(ddl);

        assertThat(result).isNotNull();
        assertThat(result.warnings()).isNotEmpty();
    }

    // B-PARSE-13: MySQL backtick 식별자 → 정상 파싱
    @Test
    void B_PARSE_13_MySQL_backtick_식별자() {
        String ddl = "CREATE TABLE `users` (`id` BIGINT PRIMARY KEY, `email` VARCHAR(255) NOT NULL);";
        DdlParser.ParseResult result = parser.parse(ddl);

        assertThat(result.tables()).hasSize(1);
        assertThat(result.tables().get(0).get("name")).isEqualTo("users");
        Map<String, Object> idCol = findCol(result.tables().get(0), "id");
        assertThat(idCol.get("isPrimaryKey")).isEqualTo(true);
    }

    // B-PARSE-14: MSSQL 대괄호 식별자 → 정상 파싱
    @Test
    void B_PARSE_14_MSSQL_대괄호_식별자() {
        String ddl = "CREATE TABLE [users] ([id] BIGINT PRIMARY KEY, [email] NVARCHAR(255) NOT NULL);";
        DdlParser.ParseResult result = parser.parse(ddl);

        assertThat(result.tables()).hasSize(1);
        assertThat(result.tables().get(0).get("name")).isEqualTo("users");
        Map<String, Object> idCol = findCol(result.tables().get(0), "id");
        assertThat(idCol.get("isPrimaryKey")).isEqualTo(true);
    }
}
