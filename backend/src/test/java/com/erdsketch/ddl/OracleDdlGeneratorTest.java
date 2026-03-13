package com.erdsketch.ddl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;

class OracleDdlGeneratorTest {

    OracleDdlGenerator generator;

    @BeforeEach
    void setUp() {
        generator = new OracleDdlGenerator();
    }

    // ─────────────── 헬퍼 메서드 ───────────────

    private Map<String, Object> col(String name, String dataType, boolean nullable, boolean isPk,
                                    boolean isUnique, boolean isAutoIncrement) {
        Map<String, Object> col = new LinkedHashMap<>();
        col.put("id", UUID.randomUUID().toString());
        col.put("name", name);
        col.put("dataType", dataType);
        col.put("nullable", nullable);
        col.put("isPrimaryKey", isPk);
        col.put("isUnique", isUnique);
        col.put("isAutoIncrement", isAutoIncrement);
        col.put("order", 0);
        return col;
    }

    private Map<String, Object> buildSchema(String tableName, List<Map<String, Object>> columns) {
        String tableId = UUID.randomUUID().toString();
        Map<String, Object> table = new LinkedHashMap<>();
        table.put("id", tableId);
        table.put("name", tableName);
        table.put("position", Map.of("x", 0, "y", 0));
        table.put("columns", columns);
        table.put("indexes", new ArrayList<>());
        Map<String, Object> schema = new LinkedHashMap<>();
        schema.put("tables", Map.of(tableId, table));
        schema.put("relationships", Map.of());
        return schema;
    }

    // ─────────────── 테스트 케이스 ───────────────

    // B-ORA-01: CREATE TABLE "users" (
    @Test
    void B_ORA_01_CREATE_TABLE_double_quote() {
        var schema = buildSchema("users",
                List.of(col("id", "INTEGER", false, true, false, false)));
        String ddl = generator.generate(schema, null, false, new ArrayList<>());

        assertThat(ddl).contains("CREATE TABLE \"users\"");
    }

    // B-ORA-02: NUMBER type 유지 (INTEGER → NUMBER(10), BIGINT → NUMBER(19))
    @Test
    void B_ORA_02_INTEGER_to_NUMBER() {
        var schema = buildSchema("orders",
                List.of(
                        col("id", "INTEGER", false, true, false, false),
                        col("count", "BIGINT", false, false, false, false)));
        String ddl = generator.generate(schema, null, false, new ArrayList<>());

        assertThat(ddl).contains("NUMBER(10)");
        assertThat(ddl).contains("NUMBER(19)");
    }

    // B-ORA-03: VARCHAR → VARCHAR2(255)
    @Test
    void B_ORA_03_VARCHAR_to_VARCHAR2_255() {
        var schema = buildSchema("users",
                List.of(col("name", "VARCHAR", false, false, false, false)));
        String ddl = generator.generate(schema, null, false, new ArrayList<>());

        assertThat(ddl).contains("VARCHAR2(255)");
        assertThat(ddl).doesNotContain(" VARCHAR ");
    }

    // B-ORA-04: BOOLEAN → NUMBER(1)
    @Test
    void B_ORA_04_BOOLEAN_to_NUMBER1() {
        var schema = buildSchema("users",
                List.of(col("is_active", "BOOLEAN", true, false, false, false)));
        String ddl = generator.generate(schema, null, false, new ArrayList<>());

        assertThat(ddl).contains("NUMBER(1)");
    }

    // B-ORA-05: UUID → VARCHAR2(36)
    @Test
    void B_ORA_05_UUID_to_VARCHAR2_36() {
        var schema = buildSchema("users",
                List.of(col("uuid_col", "UUID", true, false, false, false)));
        String ddl = generator.generate(schema, null, false, new ArrayList<>());

        assertThat(ddl).contains("VARCHAR2(36)");
    }

    // B-ORA-06: BIGINT + isAutoIncrement=true → GENERATED ALWAYS AS IDENTITY
    @Test
    void B_ORA_06_BIGINT_autoIncrement_GENERATED_ALWAYS_AS_IDENTITY() {
        var schema = buildSchema("orders",
                List.of(col("id", "BIGINT", false, true, false, true)));
        String ddl = generator.generate(schema, null, false, new ArrayList<>());

        assertThat(ddl).contains("GENERATED ALWAYS AS IDENTITY");
    }

    // B-ORA-07: 식별자 30자 초과 → warnings에 경고
    @Test
    void B_ORA_07_식별자_30자_초과_warnings_경고() {
        // 31자 테이블명
        String longTableName = "this_is_a_very_long_table_name_x"; // 33자
        assertThat(longTableName.length()).isGreaterThan(30);

        var schema = buildSchema(longTableName,
                List.of(col("id", "INTEGER", false, true, false, false)));
        List<String> warnings = new ArrayList<>();
        generator.generate(schema, null, false, warnings);

        assertThat(warnings).isNotEmpty();
        assertThat(warnings.stream().anyMatch(w -> w.contains("Identifier too long"))).isTrue();
    }

    // B-ORA-08: JSONB → CLOB
    @Test
    void B_ORA_08_JSONB_to_CLOB() {
        var schema = buildSchema("events",
                List.of(col("payload", "JSONB", true, false, false, false)));
        String ddl = generator.generate(schema, null, false, new ArrayList<>());

        assertThat(ddl).contains("CLOB");
        assertThat(ddl).doesNotContain("JSONB");
    }
}
