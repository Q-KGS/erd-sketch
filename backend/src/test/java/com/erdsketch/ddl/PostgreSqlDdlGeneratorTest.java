package com.erdsketch.ddl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;

class PostgreSqlDdlGeneratorTest {

    PostgreSqlDdlGenerator generator;

    @BeforeEach
    void setUp() {
        generator = new PostgreSqlDdlGenerator();
    }

    // ─────────────── 헬퍼 메서드 ───────────────

    Map<String, Object> col(String name, String type, boolean isPk, boolean isUnique,
                            boolean nullable, boolean autoIncrement) {
        return col(name, type, isPk, isUnique, nullable, autoIncrement, null, null);
    }

    Map<String, Object> col(String name, String type, boolean isPk, boolean isUnique,
                            boolean nullable, boolean autoIncrement,
                            String defaultValue, String comment) {
        Map<String, Object> c = new LinkedHashMap<>();
        c.put("id", UUID.randomUUID().toString());
        c.put("name", name);
        c.put("dataType", type);
        c.put("isPrimaryKey", isPk);
        c.put("isUnique", isUnique);
        c.put("nullable", nullable);
        c.put("isAutoIncrement", autoIncrement);
        if (defaultValue != null) c.put("defaultValue", defaultValue);
        if (comment != null) c.put("comment", comment);
        c.put("order", 0);
        return c;
    }

    Map<String, Object> buildSchema(String tableName, List<Map<String, Object>> columns) {
        return buildSchema(tableName, columns, null, List.of());
    }

    Map<String, Object> buildSchema(String tableName, List<Map<String, Object>> columns,
                                    String comment, List<Map<String, Object>> indexes) {
        String tableId = UUID.randomUUID().toString();
        Map<String, Object> table = new LinkedHashMap<>();
        table.put("id", tableId);
        table.put("name", tableName);
        table.put("columns", columns);
        table.put("indexes", indexes);
        if (comment != null) table.put("comment", comment);

        return Map.of("tables", Map.of(tableId, table), "relationships", Map.of());
    }

    // ─────────────── 테스트 케이스 ───────────────

    // B-DDL-01: 기본 테이블 (BIGINT PK AI)
    @Test
    void B_DDL_01_기본_테이블_BIGSERIAL_PK() {
        var schema = buildSchema("users",
                List.of(col("id", "BIGINT", true, false, false, true)));
        String ddl = generator.generate(schema, null, false, new ArrayList<>());

        assertThat(ddl).contains("CREATE TABLE \"users\"");
        assertThat(ddl).contains("BIGSERIAL");
        assertThat(ddl).contains("PRIMARY KEY");
    }

    // B-DDL-02: NOT NULL 컬럼
    @Test
    void B_DDL_02_NOT_NULL_컬럼() {
        var schema = buildSchema("t",
                List.of(col("email", "VARCHAR", false, false, false, false)));
        String ddl = generator.generate(schema, null, false, new ArrayList<>());

        assertThat(ddl).contains("NOT NULL");
    }

    // B-DDL-03: UNIQUE 컬럼
    @Test
    void B_DDL_03_UNIQUE_컬럼() {
        var schema = buildSchema("t",
                List.of(col("email", "VARCHAR", false, true, true, false)));
        String ddl = generator.generate(schema, null, false, new ArrayList<>());

        assertThat(ddl).contains("UNIQUE");
    }

    // B-DDL-04: 기본값 설정
    @Test
    void B_DDL_04_기본값_설정() {
        var schema = buildSchema("t",
                List.of(col("created_at", "TIMESTAMP", false, false, true, false, "now()", null)));
        String ddl = generator.generate(schema, null, false, new ArrayList<>());

        assertThat(ddl).contains("DEFAULT now()");
    }

    // B-DDL-05: 복합 PK
    @Test
    void B_DDL_05_복합_PK() {
        var schema = buildSchema("order_items",
                List.of(
                        col("order_id", "BIGINT", true, false, false, false),
                        col("product_id", "BIGINT", true, false, false, false)));
        String ddl = generator.generate(schema, null, false, new ArrayList<>());

        assertThat(ddl).contains("PRIMARY KEY (\"order_id\", \"product_id\")");
    }

    // B-DDL-06: includeDrops
    @Test
    void B_DDL_06_includeDrops() {
        var schema = buildSchema("t", List.of(col("id", "INTEGER", true, false, false, false)));
        String ddl = generator.generate(schema, null, true, new ArrayList<>());

        assertThat(ddl).contains("DROP TABLE IF EXISTS \"t\" CASCADE");
        assertThat(ddl).contains("CREATE TABLE \"t\"");
    }

    // B-DDL-07: FK 관계 생성 (ON DELETE CASCADE)
    @Test
    void B_DDL_07_FK_관계_생성() {
        String usersId = UUID.randomUUID().toString();
        String postsId = UUID.randomUUID().toString();
        String userColId = UUID.randomUUID().toString();
        String postUserColId = UUID.randomUUID().toString();

        Map<String, Object> userCol = col("id", "BIGINT", true, false, false, false);
        userCol.put("id", userColId);
        Map<String, Object> usersTable = Map.of("id", usersId, "name", "users",
                "columns", List.of(userCol), "indexes", List.of());

        Map<String, Object> postUserCol = col("user_id", "BIGINT", false, false, true, false);
        postUserCol.put("id", postUserColId);
        Map<String, Object> postsTable = Map.of("id", postsId, "name", "posts",
                "columns", List.of(postUserCol), "indexes", List.of());

        Map<String, Object> rel = Map.of(
                "id", UUID.randomUUID().toString(),
                "sourceTableId", postsId,
                "targetTableId", usersId,
                "sourceColumnIds", List.of(postUserColId),
                "targetColumnIds", List.of(userColId),
                "cardinality", "1:N",
                "onDelete", "CASCADE",
                "onUpdate", "RESTRICT");

        Map<String, Object> schema = Map.of(
                "tables", Map.of(usersId, usersTable, postsId, postsTable),
                "relationships", Map.of(rel.get("id"), rel));

        String ddl = generator.generate(schema, null, false, new ArrayList<>());

        assertThat(ddl).contains("ALTER TABLE \"posts\"");
        assertThat(ddl).contains("ADD CONSTRAINT fk_posts_users");
        assertThat(ddl).contains("ON DELETE CASCADE");
    }

    // B-DDL-08: BTREE 인덱스 생성
    @Test
    void B_DDL_08_BTREE_인덱스_생성() {
        Map<String, Object> idx = Map.of("name", "idx_email", "isUnique", false,
                "columns", List.of("email"), "type", "BTREE");
        var schema = buildSchema("users",
                List.of(col("email", "VARCHAR", false, false, true, false)),
                null, List.of(idx));
        String ddl = generator.generate(schema, null, false, new ArrayList<>());

        assertThat(ddl).contains("CREATE INDEX \"idx_email\"");
        assertThat(ddl).contains("USING BTREE");
    }

    // B-DDL-09: UNIQUE 인덱스
    @Test
    void B_DDL_09_UNIQUE_인덱스() {
        Map<String, Object> idx = Map.of("name", "uq_email", "isUnique", true,
                "columns", List.of("email"), "type", "BTREE");
        var schema = buildSchema("users",
                List.of(col("email", "VARCHAR", false, false, true, false)),
                null, List.of(idx));
        String ddl = generator.generate(schema, null, false, new ArrayList<>());

        assertThat(ddl).contains("CREATE UNIQUE INDEX \"uq_email\"");
    }

    // B-DDL-10: 빈 스키마
    @Test
    void B_DDL_10_빈_스키마() {
        Map<String, Object> schema = Map.of("tables", Map.of(), "relationships", Map.of());
        String ddl = generator.generate(schema, null, false, new ArrayList<>());

        assertThat(ddl).contains("-- No schema defined yet");
    }

    // B-DDL-11: 특수 문자 테이블명 이스케이프
    @Test
    void B_DDL_11_특수문자_테이블명_이스케이프() {
        var schema = buildSchema("user-data",
                List.of(col("id", "INTEGER", true, false, false, false)));
        String ddl = generator.generate(schema, null, false, new ArrayList<>());

        assertThat(ddl).contains("\"user-data\"");
    }

    // B-DDL-12: 테이블 설명 (COMMENT)
    @Test
    void B_DDL_12_테이블_설명() {
        var schema = buildSchema("users",
                List.of(col("id", "INTEGER", true, false, false, false)),
                "사용자 테이블", List.of());
        String ddl = generator.generate(schema, null, false, new ArrayList<>());

        assertThat(ddl).contains("COMMENT ON TABLE \"users\"");
        assertThat(ddl).contains("사용자 테이블");
    }

    // 추가: tableIds 필터링
    @Test
    void tableIds_필터링() {
        String id1 = UUID.randomUUID().toString();
        String id2 = UUID.randomUUID().toString();
        Map<String, Object> t1 = Map.of("id", id1, "name", "t1",
                "columns", List.of(col("id", "INTEGER", true, false, false, false)),
                "indexes", List.of());
        Map<String, Object> t2 = Map.of("id", id2, "name", "t2",
                "columns", List.of(col("id", "INTEGER", true, false, false, false)),
                "indexes", List.of());
        Map<String, Object> schema = Map.of("tables", Map.of(id1, t1, id2, t2),
                "relationships", Map.of());

        String ddl = generator.generate(schema, List.of(id1), false, new ArrayList<>());

        assertThat(ddl).contains("\"t1\"");
        assertThat(ddl).doesNotContain("\"t2\"");
    }
}
