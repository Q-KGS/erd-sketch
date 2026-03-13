package com.erdsketch.diff;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class MigrationDdlGeneratorTest {

    private final MigrationDdlGenerator generator = new MigrationDdlGenerator();

    private static final String CURRENT_SCHEMA_WITH_USERS = """
            {
              "tables": {
                "t1": {
                  "name": "users",
                  "columns": [
                    {"name": "id", "dataType": "BIGINT", "nullable": false},
                    {"name": "email", "dataType": "VARCHAR(255)", "nullable": false}
                  ]
                }
              }
            }
            """;

    // ───── B-MIG-01: ADD_TABLE → CREATE TABLE ─────
    @Test
    void B_MIG_01_테이블_추가_CREATE_TABLE_생성() {
        // given
        DiffResult diff = DiffResult.of(List.of(
                new SchemaChange(SchemaChangeType.ADD_TABLE, "users", null, null, null, "Table added")
        ));

        // when
        String ddl = generator.generate(diff, null, CURRENT_SCHEMA_WITH_USERS, "postgresql");

        // then
        assertThat(ddl).containsIgnoringCase("CREATE TABLE");
        assertThat(ddl).contains("users");
    }

    // ───── B-MIG-02: DROP_TABLE → DROP TABLE ─────
    @Test
    void B_MIG_02_테이블_삭제_DROP_TABLE_생성() {
        // given
        DiffResult diff = DiffResult.of(List.of(
                new SchemaChange(SchemaChangeType.DROP_TABLE, "old_table", null, null, null, "Table dropped")
        ));

        // when
        String ddl = generator.generate(diff);

        // then
        assertThat(ddl).containsIgnoringCase("DROP TABLE");
        assertThat(ddl).contains("old_table");
    }

    // ───── B-MIG-03: ADD_COLUMN → ALTER TABLE ADD COLUMN ─────
    @Test
    void B_MIG_03_컬럼_추가_ALTER_TABLE_ADD_COLUMN() {
        // given
        DiffResult diff = DiffResult.of(List.of(
                new SchemaChange(SchemaChangeType.ADD_COLUMN, "users", "phone", null, "VARCHAR(20)", "Column added")
        ));

        // when
        String ddl = generator.generate(diff);

        // then
        assertThat(ddl).containsIgnoringCase("ALTER TABLE");
        assertThat(ddl).containsIgnoringCase("ADD COLUMN");
        assertThat(ddl).contains("phone");
        assertThat(ddl).contains("VARCHAR(20)");
    }

    // ───── B-MIG-04: DROP_COLUMN → ALTER TABLE DROP COLUMN ─────
    @Test
    void B_MIG_04_컬럼_삭제_ALTER_TABLE_DROP_COLUMN() {
        // given
        DiffResult diff = DiffResult.of(List.of(
                new SchemaChange(SchemaChangeType.DROP_COLUMN, "users", "old_col", "VARCHAR(100)", null, "Column dropped")
        ));

        // when
        String ddl = generator.generate(diff);

        // then
        assertThat(ddl).containsIgnoringCase("DROP COLUMN");
        assertThat(ddl).contains("old_col");
    }

    // ───── B-MIG-05: MODIFY_COLUMN type → ALTER COLUMN TYPE (PostgreSQL) ─────
    @Test
    void B_MIG_05_컬럼_타입_변경_PostgreSQL_ALTER_COLUMN_TYPE() {
        // given
        DiffResult diff = DiffResult.of(List.of(
                new SchemaChange(SchemaChangeType.MODIFY_COLUMN, "users", "id", "INT", "BIGINT", "Type changed")
        ));

        // when
        String ddl = generator.generate(diff, "postgresql");

        // then
        assertThat(ddl).containsIgnoringCase("ALTER COLUMN");
        assertThat(ddl).contains("BIGINT");
    }

    // ───── B-MIG-06: MySQL 방언 ─────
    @Test
    void B_MIG_06_MySQL_방언_MODIFY_COLUMN() {
        // given
        DiffResult diff = DiffResult.of(List.of(
                new SchemaChange(SchemaChangeType.MODIFY_COLUMN, "users", "id", "INT", "BIGINT", "Type changed")
        ));

        // when
        String ddl = generator.generate(diff, "mysql");

        // then
        assertThat(ddl).containsIgnoringCase("MODIFY COLUMN");
    }

    // ───── B-MIG-07: 변경 없음 → "No changes" ─────
    @Test
    void B_MIG_07_변경_없을_때_no_changes_메시지() {
        // given
        DiffResult diff = DiffResult.empty();

        // when
        String ddl = generator.generate(diff);

        // then
        assertThat(ddl).containsIgnoringCase("No changes");
    }
}
