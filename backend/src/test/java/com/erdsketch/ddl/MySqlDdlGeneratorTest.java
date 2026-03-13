package com.erdsketch.ddl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;

class MySqlDdlGeneratorTest {

    MySqlDdlGenerator generator;

    @BeforeEach
    void setUp() {
        generator = new MySqlDdlGenerator();
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

    private Map<String, Object> buildSchemaWithComment(String tableName, List<Map<String, Object>> columns,
                                                        String comment) {
        String tableId = UUID.randomUUID().toString();
        Map<String, Object> table = new LinkedHashMap<>();
        table.put("id", tableId);
        table.put("name", tableName);
        table.put("position", Map.of("x", 0, "y", 0));
        table.put("columns", columns);
        table.put("indexes", new ArrayList<>());
        if (comment != null) table.put("comment", comment);
        Map<String, Object> schema = new LinkedHashMap<>();
        schema.put("tables", Map.of(tableId, table));
        schema.put("relationships", Map.of());
        return schema;
    }

    // ─────────────── 테스트 케이스 ───────────────

    // B-MYSQL-01: CREATE TABLE `users`, ENGINE=InnoDB, DEFAULT CHARSET=utf8mb4
    @Test
    void B_MYSQL_01_CREATE_TABLE_backtick_ENGINE_InnoDB_CHARSET_utf8mb4() {
        var schema = buildSchema("users",
                List.of(col("id", "INTEGER", false, true, false, false)));
        String ddl = generator.generate(schema, null, false, new ArrayList<>());

        assertThat(ddl).contains("CREATE TABLE `users`");
        assertThat(ddl).contains("ENGINE=InnoDB");
        assertThat(ddl).contains("DEFAULT CHARSET=utf8mb4");
    }

    // B-MYSQL-02: INT AUTO_INCREMENT PK → INT AUTO_INCREMENT + PRIMARY KEY(`id`)
    @Test
    void B_MYSQL_02_INT_AUTO_INCREMENT_PK() {
        var schema = buildSchema("users",
                List.of(col("id", "INTEGER", false, true, false, true)));
        String ddl = generator.generate(schema, null, false, new ArrayList<>());

        assertThat(ddl).contains("`id` INT");
        assertThat(ddl).contains("AUTO_INCREMENT");
        assertThat(ddl).contains("PRIMARY KEY (`id`)");
    }

    // B-MYSQL-03: BIGINT AUTO_INCREMENT → BIGINT AUTO_INCREMENT
    @Test
    void B_MYSQL_03_BIGINT_AUTO_INCREMENT() {
        var schema = buildSchema("orders",
                List.of(col("id", "BIGINT", false, true, false, true)));
        String ddl = generator.generate(schema, null, false, new ArrayList<>());

        assertThat(ddl).contains("`id` BIGINT");
        assertThat(ddl).contains("AUTO_INCREMENT");
    }

    // B-MYSQL-04: BOOLEAN → TINYINT(1)
    @Test
    void B_MYSQL_04_BOOLEAN_to_TINYINT1() {
        var schema = buildSchema("users",
                List.of(col("is_active", "BOOLEAN", true, false, false, false)));
        String ddl = generator.generate(schema, null, false, new ArrayList<>());

        assertThat(ddl).contains("TINYINT(1)");
    }

    // B-MYSQL-05: UUID → VARCHAR(36)
    @Test
    void B_MYSQL_05_UUID_to_VARCHAR36() {
        var schema = buildSchema("users",
                List.of(col("uuid_col", "UUID", true, false, false, false)));
        String ddl = generator.generate(schema, null, false, new ArrayList<>());

        assertThat(ddl).contains("VARCHAR(36)");
    }

    // B-MYSQL-06: JSONB → JSON
    @Test
    void B_MYSQL_06_JSONB_to_JSON() {
        var schema = buildSchema("events",
                List.of(col("payload", "JSONB", true, false, false, false)));
        String ddl = generator.generate(schema, null, false, new ArrayList<>());

        assertThat(ddl).contains("JSON");
        assertThat(ddl).doesNotContain("JSONB");
    }

    // B-MYSQL-07: TEXT → TEXT
    @Test
    void B_MYSQL_07_TEXT_stays_TEXT() {
        var schema = buildSchema("articles",
                List.of(col("body", "TEXT", true, false, false, false)));
        String ddl = generator.generate(schema, null, false, new ArrayList<>());

        assertThat(ddl).contains("`body` TEXT");
    }

    // B-MYSQL-08: TIMESTAMP → DATETIME
    @Test
    void B_MYSQL_08_TIMESTAMP_to_DATETIME() {
        var schema = buildSchema("logs",
                List.of(col("created_at", "TIMESTAMP", true, false, false, false)));
        String ddl = generator.generate(schema, null, false, new ArrayList<>());

        assertThat(ddl).contains("DATETIME");
        assertThat(ddl).doesNotContain("TIMESTAMP");
    }

    // B-MYSQL-09: includeDrops=true → DROP TABLE IF EXISTS `users` 선행
    @Test
    void B_MYSQL_09_includeDrops_true() {
        var schema = buildSchema("users",
                List.of(col("id", "INTEGER", false, true, false, false)));
        String ddl = generator.generate(schema, null, true, new ArrayList<>());

        assertThat(ddl).contains("DROP TABLE IF EXISTS `users`");
        assertThat(ddl).contains("CREATE TABLE `users`");
        int dropIdx = ddl.indexOf("DROP TABLE IF EXISTS `users`");
        int createIdx = ddl.indexOf("CREATE TABLE `users`");
        assertThat(dropIdx).isLessThan(createIdx);
    }

    // B-MYSQL-10: 테이블 comment → COMMENT='...'
    @Test
    void B_MYSQL_10_테이블_comment() {
        var schema = buildSchemaWithComment("users",
                List.of(col("id", "INTEGER", false, true, false, false)),
                "사용자 테이블");
        String ddl = generator.generate(schema, null, false, new ArrayList<>());

        assertThat(ddl).contains("COMMENT='사용자 테이블'");
    }

    // B-MYSQL-11: SET FOREIGN_KEY_CHECKS 시작/끝에 0/1 설정
    @Test
    void B_MYSQL_11_SET_FOREIGN_KEY_CHECKS_wrap() {
        var schema = buildSchema("users",
                List.of(col("id", "INTEGER", false, true, false, false)));
        String ddl = generator.generate(schema, null, false, new ArrayList<>());

        assertThat(ddl).contains("SET FOREIGN_KEY_CHECKS = 0;");
        assertThat(ddl).contains("SET FOREIGN_KEY_CHECKS = 1;");
        int offIdx = ddl.indexOf("SET FOREIGN_KEY_CHECKS = 0;");
        int onIdx = ddl.lastIndexOf("SET FOREIGN_KEY_CHECKS = 1;");
        assertThat(offIdx).isLessThan(onIdx);
    }
}
