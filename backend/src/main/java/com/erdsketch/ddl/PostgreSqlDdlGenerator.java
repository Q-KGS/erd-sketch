package com.erdsketch.ddl;

import org.springframework.stereotype.Component;

import java.util.*;

@Component
public class PostgreSqlDdlGenerator {

    @SuppressWarnings("unchecked")
    public String generate(Map<String, Object> schema, List<String> tableIds, boolean includeDrops, List<String> warnings) {
        StringBuilder sb = new StringBuilder();
        Map<String, Object> tables = (Map<String, Object>) schema.getOrDefault("tables", Map.of());
        Map<String, Object> relationships = (Map<String, Object>) schema.getOrDefault("relationships", Map.of());

        // Filter tables if tableIds provided
        Collection<Object> tableValues = tableIds != null && !tableIds.isEmpty()
                ? tableIds.stream().map(tables::get).filter(Objects::nonNull).toList()
                : tables.values();

        if (tableValues.isEmpty() && relationships.values().isEmpty()) {
            return "-- No schema defined yet";
        }

        if (includeDrops) {
            tableValues.forEach(tableObj -> {
                Map<String, Object> table = (Map<String, Object>) tableObj;
                sb.append("DROP TABLE IF EXISTS ").append(quoteIdentifier((String) table.get("name"))).append(" CASCADE;\n");
            });
            sb.append("\n");
        }

        tableValues.forEach(tableObj -> {
            Map<String, Object> table = (Map<String, Object>) tableObj;
            generateTableDdl(sb, table, warnings);
            sb.append("\n");
        });

        // Foreign keys from relationships
        relationships.values().forEach(relObj -> {
            Map<String, Object> rel = (Map<String, Object>) relObj;
            generateForeignKey(sb, rel, tables, warnings);
        });

        return sb.toString();
    }

    @SuppressWarnings("unchecked")
    private void generateTableDdl(StringBuilder sb, Map<String, Object> table, List<String> warnings) {
        String tableName = (String) table.get("name");
        List<Map<String, Object>> columns = (List<Map<String, Object>>) table.getOrDefault("columns", List.of());
        List<Map<String, Object>> indexes = (List<Map<String, Object>>) table.getOrDefault("indexes", List.of());
        String comment = (String) table.get("comment");

        sb.append("CREATE TABLE ").append(quoteIdentifier(tableName)).append(" (\n");

        List<String> colDefs = new ArrayList<>();
        List<String> pkColumns = new ArrayList<>();

        for (Map<String, Object> col : columns) {
            String colName = (String) col.get("name");
            String dataType = mapDataType((String) col.get("dataType"));
            boolean nullable = (boolean) col.getOrDefault("nullable", true);
            boolean isPk = (boolean) col.getOrDefault("isPrimaryKey", false);
            boolean isUnique = (boolean) col.getOrDefault("isUnique", false);
            boolean isAutoIncrement = (boolean) col.getOrDefault("isAutoIncrement", false);
            String defaultVal = (String) col.get("defaultValue");
            String colComment = (String) col.get("comment");

            if (isAutoIncrement && (dataType.equals("INTEGER") || dataType.equals("INT"))) {
                dataType = "SERIAL";
            } else if (isAutoIncrement && (dataType.equals("BIGINT"))) {
                dataType = "BIGSERIAL";
            }

            StringBuilder colDef = new StringBuilder("    ")
                    .append(quoteIdentifier(colName)).append(" ").append(dataType);
            if (!nullable && !isPk) colDef.append(" NOT NULL");
            if (defaultVal != null && !defaultVal.isEmpty()) colDef.append(" DEFAULT ").append(defaultVal);
            if (isUnique && !isPk) colDef.append(" UNIQUE");

            colDefs.add(colDef.toString());
            if (isPk) pkColumns.add(quoteIdentifier(colName));
        }

        if (!pkColumns.isEmpty()) {
            colDefs.add("    PRIMARY KEY (" + String.join(", ", pkColumns) + ")");
        }

        sb.append(String.join(",\n", colDefs)).append("\n);\n");

        if (comment != null && !comment.isEmpty()) {
            sb.append("COMMENT ON TABLE ").append(quoteIdentifier(tableName))
              .append(" IS '").append(comment.replace("'", "''")).append("';\n");
        }

        // Column comments
        for (Map<String, Object> col : columns) {
            String colComment = (String) col.get("comment");
            if (colComment != null && !colComment.isEmpty()) {
                sb.append("COMMENT ON COLUMN ").append(quoteIdentifier(tableName))
                  .append(".").append(quoteIdentifier((String) col.get("name")))
                  .append(" IS '").append(colComment.replace("'", "''")).append("';\n");
            }
        }

        // Indexes
        for (Map<String, Object> idx : indexes) {
            generateIndex(sb, tableName, idx);
        }
    }

    @SuppressWarnings("unchecked")
    private void generateIndex(StringBuilder sb, String tableName, Map<String, Object> idx) {
        String idxName = (String) idx.get("name");
        boolean isUnique = (boolean) idx.getOrDefault("isUnique", false);
        List<String> cols = (List<String>) idx.getOrDefault("columns", List.of());
        String type = (String) idx.getOrDefault("type", "BTREE");

        sb.append(isUnique ? "CREATE UNIQUE INDEX " : "CREATE INDEX ")
          .append(quoteIdentifier(idxName)).append(" ON ").append(quoteIdentifier(tableName))
          .append(" USING ").append(type).append(" (")
          .append(String.join(", ", cols.stream().map(this::quoteIdentifier).toList()))
          .append(");\n");
    }

    @SuppressWarnings("unchecked")
    private void generateForeignKey(StringBuilder sb, Map<String, Object> rel, Map<String, Object> tables, List<String> warnings) {
        String srcTableId = (String) rel.get("sourceTableId");
        String tgtTableId = (String) rel.get("targetTableId");
        Map<String, Object> srcTable = (Map<String, Object>) tables.get(srcTableId);
        Map<String, Object> tgtTable = (Map<String, Object>) tables.get(tgtTableId);

        if (srcTable == null || tgtTable == null) {
            warnings.add("Skipped FK: could not find table for relationship " + rel.get("id"));
            return;
        }

        List<String> srcCols = (List<String>) rel.getOrDefault("sourceColumnIds", List.of());
        List<String> tgtCols = (List<String>) rel.getOrDefault("targetColumnIds", List.of());

        if (srcCols.isEmpty() || tgtCols.isEmpty()) return;

        String srcTableName = (String) srcTable.get("name");
        String tgtTableName = (String) tgtTable.get("name");
        String onDelete = (String) rel.getOrDefault("onDelete", "RESTRICT");
        String onUpdate = (String) rel.getOrDefault("onUpdate", "RESTRICT");

        // Resolve column names from ids
        List<String> srcColNames = resolveColumnNames(srcTable, srcCols);
        List<String> tgtColNames = resolveColumnNames(tgtTable, tgtCols);

        sb.append("ALTER TABLE ").append(quoteIdentifier(srcTableName))
          .append(" ADD CONSTRAINT fk_").append(srcTableName).append("_").append(tgtTableName)
          .append(" FOREIGN KEY (").append(String.join(", ", srcColNames.stream().map(this::quoteIdentifier).toList()))
          .append(") REFERENCES ").append(quoteIdentifier(tgtTableName))
          .append(" (").append(String.join(", ", tgtColNames.stream().map(this::quoteIdentifier).toList()))
          .append(") ON DELETE ").append(onDelete.replace("_", " "))
          .append(" ON UPDATE ").append(onUpdate.replace("_", " ")).append(";\n");
    }

    @SuppressWarnings("unchecked")
    private List<String> resolveColumnNames(Map<String, Object> table, List<String> columnIds) {
        List<Map<String, Object>> columns = (List<Map<String, Object>>) table.getOrDefault("columns", List.of());
        return columnIds.stream()
                .map(id -> columns.stream()
                        .filter(c -> id.equals(c.get("id")))
                        .map(c -> (String) c.get("name"))
                        .findFirst().orElse(id))
                .toList();
    }

    private String mapDataType(String type) {
        if (type == null) return "TEXT";
        return switch (type.toUpperCase()) {
            case "INT", "INTEGER" -> "INTEGER";
            case "BIGINT" -> "BIGINT";
            case "SMALLINT" -> "SMALLINT";
            case "DECIMAL", "NUMERIC" -> "NUMERIC";
            case "FLOAT", "DOUBLE", "DOUBLE PRECISION" -> "DOUBLE PRECISION";
            case "REAL" -> "REAL";
            case "BOOLEAN", "BOOL", "BIT" -> "BOOLEAN";
            case "DATE" -> "DATE";
            case "DATETIME", "TIMESTAMP" -> "TIMESTAMP";
            case "TIMESTAMPTZ" -> "TIMESTAMPTZ";
            case "UUID", "UNIQUEIDENTIFIER" -> "UUID";
            case "JSON", "JSONB" -> "JSONB";
            case "TEXT", "CLOB", "LONGTEXT", "NTEXT" -> "TEXT";
            case "BYTEA", "BLOB", "VARBINARY" -> "BYTEA";
            default -> type;
        };
    }

    private String quoteIdentifier(String name) {
        if (name == null) return "\"\"";
        return "\"" + name.replace("\"", "\"\"") + "\"";
    }
}
