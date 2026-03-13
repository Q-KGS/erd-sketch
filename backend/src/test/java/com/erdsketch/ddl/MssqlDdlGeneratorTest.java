package com.erdsketch.ddl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;

class MssqlDdlGeneratorTest {

    MssqlDdlGenerator generator;

    @BeforeEach
    void setUp() {
        generator = new MssqlDdlGenerator();
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

    // B-MSSQL-01: CREATE TABLE [users] (
    @Test
    void B_MSSQL_01_CREATE_TABLE_bracket_quote() {
        var schema = buildSchema("users",
                List.of(col("id", "INTEGER", false, true, false, false)));
        String ddl = generator.generate(schema, null, false, new ArrayList<>());

        assertThat(ddl).contains("CREATE TABLE [users]");
    }

    // B-MSSQL-02: BIGINT + isAutoIncrement=true → [id] BIGINT IDENTITY(1,1)
    @Test
    void B_MSSQL_02_BIGINT_autoIncrement_IDENTITY() {
        var schema = buildSchema("orders",
                List.of(col("id", "BIGINT", false, true, false, true)));
        String ddl = generator.generate(schema, null, false, new ArrayList<>());

        assertThat(ddl).contains("[id] BIGINT IDENTITY(1,1)");
    }

    // B-MSSQL-03: NVARCHAR 타입 → NVARCHAR(255) 유지
    @Test
    void B_MSSQL_03_NVARCHAR_stays_NVARCHAR255() {
        var schema = buildSchema("users",
                List.of(col("name", "NVARCHAR", false, false, false, false)));
        String ddl = generator.generate(schema, null, false, new ArrayList<>());

        assertThat(ddl).contains("NVARCHAR(255)");
    }

    // B-MSSQL-04: BOOLEAN → BIT
    @Test
    void B_MSSQL_04_BOOLEAN_to_BIT() {
        var schema = buildSchema("users",
                List.of(col("is_active", "BOOLEAN", true, false, false, false)));
        String ddl = generator.generate(schema, null, false, new ArrayList<>());

        assertThat(ddl).contains("BIT");
        assertThat(ddl).doesNotContain("BOOLEAN");
    }

    // B-MSSQL-05: UUID → UNIQUEIDENTIFIER
    @Test
    void B_MSSQL_05_UUID_to_UNIQUEIDENTIFIER() {
        var schema = buildSchema("users",
                List.of(col("uuid_col", "UUID", true, false, false, false)));
        String ddl = generator.generate(schema, null, false, new ArrayList<>());

        assertThat(ddl).contains("UNIQUEIDENTIFIER");
    }

    // B-MSSQL-06: JSONB → NVARCHAR(MAX) + warnings에 경고
    @Test
    void B_MSSQL_06_JSONB_to_NVARCHAR_MAX_with_warning() {
        var schema = buildSchema("events",
                List.of(col("payload", "JSONB", true, false, false, false)));
        List<String> warnings = new ArrayList<>();
        String ddl = generator.generate(schema, null, false, warnings);

        assertThat(ddl).contains("NVARCHAR(MAX)");
        assertThat(warnings).isNotEmpty();
        assertThat(warnings.stream().anyMatch(w -> w.contains("JSONB") || w.contains("NVARCHAR(MAX)"))).isTrue();
    }

    // B-MSSQL-07: includeDrops=true → IF OBJECT_ID(...) IS NOT NULL DROP TABLE [users]
    @Test
    void B_MSSQL_07_includeDrops_true_IF_OBJECT_ID() {
        var schema = buildSchema("users",
                List.of(col("id", "INTEGER", false, true, false, false)));
        String ddl = generator.generate(schema, null, true, new ArrayList<>());

        assertThat(ddl).contains("IF OBJECT_ID(");
        assertThat(ddl).contains("IS NOT NULL DROP TABLE [users]");
        assertThat(ddl).contains("CREATE TABLE [users]");
        int dropIdx = ddl.indexOf("IF OBJECT_ID(");
        int createIdx = ddl.indexOf("CREATE TABLE [users]");
        assertThat(dropIdx).isLessThan(createIdx);
    }
}
