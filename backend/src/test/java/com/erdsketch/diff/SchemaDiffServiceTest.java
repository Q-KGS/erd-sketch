package com.erdsketch.diff;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class SchemaDiffServiceTest {

    private final SchemaDiffService service = new SchemaDiffService(new ObjectMapper());

    private static final String SCHEMA_WITH_USERS = """
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

    private static final String SCHEMA_WITH_USERS_AND_ORDERS = """
            {
              "tables": {
                "t1": {
                  "name": "users",
                  "columns": [
                    {"name": "id", "dataType": "BIGINT", "nullable": false},
                    {"name": "email", "dataType": "VARCHAR(255)", "nullable": false}
                  ]
                },
                "t2": {
                  "name": "orders",
                  "columns": [
                    {"name": "id", "dataType": "BIGINT", "nullable": false},
                    {"name": "total", "dataType": "DECIMAL", "nullable": true}
                  ]
                }
              }
            }
            """;

    // ───── B-DIFF-01: ADD_TABLE ─────
    @Test
    void B_DIFF_01_테이블_추가_감지() {
        // given
        String base = SCHEMA_WITH_USERS;
        String current = SCHEMA_WITH_USERS_AND_ORDERS;

        // when
        DiffResult result = service.diff(base, current);

        // then
        assertThat(result.hasChanges()).isTrue();
        List<SchemaChange> addTables = result.changes().stream()
                .filter(c -> c.type() == SchemaChangeType.ADD_TABLE)
                .toList();
        assertThat(addTables).hasSize(1);
        assertThat(addTables.get(0).tableName()).isEqualTo("orders");
    }

    // ───── B-DIFF-02: DROP_TABLE ─────
    @Test
    void B_DIFF_02_테이블_삭제_감지() {
        // given
        String base = SCHEMA_WITH_USERS_AND_ORDERS;
        String current = SCHEMA_WITH_USERS;

        // when
        DiffResult result = service.diff(base, current);

        // then
        assertThat(result.hasChanges()).isTrue();
        List<SchemaChange> dropTables = result.changes().stream()
                .filter(c -> c.type() == SchemaChangeType.DROP_TABLE)
                .toList();
        assertThat(dropTables).hasSize(1);
        assertThat(dropTables.get(0).tableName()).isEqualTo("orders");
    }

    // ───── B-DIFF-03: ADD_COLUMN ─────
    @Test
    void B_DIFF_03_컬럼_추가_감지() {
        // given
        String base = """
                {
                  "tables": {
                    "t1": {
                      "name": "users",
                      "columns": [
                        {"name": "id", "dataType": "BIGINT", "nullable": false}
                      ]
                    }
                  }
                }
                """;
        String current = """
                {
                  "tables": {
                    "t1": {
                      "name": "users",
                      "columns": [
                        {"name": "id", "dataType": "BIGINT", "nullable": false},
                        {"name": "name", "dataType": "VARCHAR(100)", "nullable": true}
                      ]
                    }
                  }
                }
                """;

        // when
        DiffResult result = service.diff(base, current);

        // then
        assertThat(result.hasChanges()).isTrue();
        List<SchemaChange> addCols = result.changes().stream()
                .filter(c -> c.type() == SchemaChangeType.ADD_COLUMN)
                .toList();
        assertThat(addCols).hasSize(1);
        assertThat(addCols.get(0).columnName()).isEqualTo("name");
        assertThat(addCols.get(0).tableName()).isEqualTo("users");
    }

    // ───── B-DIFF-04: DROP_COLUMN ─────
    @Test
    void B_DIFF_04_컬럼_삭제_감지() {
        // given
        String base = """
                {
                  "tables": {
                    "t1": {
                      "name": "users",
                      "columns": [
                        {"name": "id", "dataType": "BIGINT", "nullable": false},
                        {"name": "name", "dataType": "VARCHAR(100)", "nullable": true}
                      ]
                    }
                  }
                }
                """;
        String current = """
                {
                  "tables": {
                    "t1": {
                      "name": "users",
                      "columns": [
                        {"name": "id", "dataType": "BIGINT", "nullable": false}
                      ]
                    }
                  }
                }
                """;

        // when
        DiffResult result = service.diff(base, current);

        // then
        List<SchemaChange> dropCols = result.changes().stream()
                .filter(c -> c.type() == SchemaChangeType.DROP_COLUMN)
                .toList();
        assertThat(dropCols).hasSize(1);
        assertThat(dropCols.get(0).columnName()).isEqualTo("name");
    }

    // ───── B-DIFF-05: MODIFY_COLUMN (type 변경) ─────
    @Test
    void B_DIFF_05_컬럼_타입_변경_감지() {
        // given
        String base = """
                {
                  "tables": {
                    "t1": {
                      "name": "users",
                      "columns": [
                        {"name": "id", "dataType": "INT", "nullable": false}
                      ]
                    }
                  }
                }
                """;
        String current = """
                {
                  "tables": {
                    "t1": {
                      "name": "users",
                      "columns": [
                        {"name": "id", "dataType": "BIGINT", "nullable": false}
                      ]
                    }
                  }
                }
                """;

        // when
        DiffResult result = service.diff(base, current);

        // then
        List<SchemaChange> modifyCols = result.changes().stream()
                .filter(c -> c.type() == SchemaChangeType.MODIFY_COLUMN)
                .toList();
        assertThat(modifyCols).hasSize(1);
        assertThat(modifyCols.get(0).fromValue()).isEqualTo("INT");
        assertThat(modifyCols.get(0).toValue()).isEqualTo("BIGINT");
    }

    // ───── B-DIFF-06: MODIFY_COLUMN (nullable 변경) ─────
    @Test
    void B_DIFF_06_컬럼_nullable_변경_감지() {
        // given
        String base = """
                {
                  "tables": {
                    "t1": {
                      "name": "users",
                      "columns": [
                        {"name": "email", "dataType": "VARCHAR(255)", "nullable": true}
                      ]
                    }
                  }
                }
                """;
        String current = """
                {
                  "tables": {
                    "t1": {
                      "name": "users",
                      "columns": [
                        {"name": "email", "dataType": "VARCHAR(255)", "nullable": false}
                      ]
                    }
                  }
                }
                """;

        // when
        DiffResult result = service.diff(base, current);

        // then
        List<SchemaChange> modifyCols = result.changes().stream()
                .filter(c -> c.type() == SchemaChangeType.MODIFY_COLUMN)
                .toList();
        assertThat(modifyCols).hasSize(1);
        assertThat(modifyCols.get(0).fromValue()).isEqualTo("true");
        assertThat(modifyCols.get(0).toValue()).isEqualTo("false");
    }

    // ───── B-DIFF-07: 변경 없음 ─────
    @Test
    void B_DIFF_07_변경_없을_때_빈_결과() {
        // given
        String schema = SCHEMA_WITH_USERS;

        // when
        DiffResult result = service.diff(schema, schema);

        // then
        assertThat(result.hasChanges()).isFalse();
        assertThat(result.changes()).isEmpty();
    }

    // ───── B-DIFF-08: null 스키마 처리 ─────
    @Test
    void B_DIFF_08_null_스키마_처리() {
        // given & when
        DiffResult result = service.diff(null, null);

        // then
        assertThat(result.hasChanges()).isFalse();
        assertThat(result.changes()).isEmpty();
    }
}
