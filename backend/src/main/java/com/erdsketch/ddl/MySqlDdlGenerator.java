package com.erdsketch.ddl;

import org.springframework.stereotype.Component;

import java.util.*;

@Component
public class MySqlDdlGenerator {

    @SuppressWarnings("unchecked")
    public String generate(Map<String, Object> schema, List<String> tableIds, boolean includeDrops, List<String> warnings) {
        StringBuilder sb = new StringBuilder();
        sb.append("SET FOREIGN_KEY_CHECKS = 0;\n\n");

        Map<String, Object> tables = (Map<String, Object>) schema.getOrDefault("tables", Map.of());
        Collection<Object> tableValues = tableIds != null && !tableIds.isEmpty()
                ? tableIds.stream().map(tables::get).filter(Objects::nonNull).toList()
                : tables.values();

        if (includeDrops) {
            tableValues.forEach(tableObj -> {
                Map<String, Object> table = (Map<String, Object>) tableObj;
                sb.append("DROP TABLE IF EXISTS `").append(table.get("name")).append("`;\n");
            });
            sb.append("\n");
        }

        tableValues.forEach(tableObj -> {
            Map<String, Object> table = (Map<String, Object>) tableObj;
            generateTableDdl(sb, table, warnings);
            sb.append("\n");
        });

        sb.append("SET FOREIGN_KEY_CHECKS = 1;\n");
        return sb.toString();
    }

    @SuppressWarnings("unchecked")
    private void generateTableDdl(StringBuilder sb, Map<String, Object> table, List<String> warnings) {
        String tableName = (String) table.get("name");
        List<Map<String, Object>> columns = (List<Map<String, Object>>) table.getOrDefault("columns", List.of());
        String comment = (String) table.get("comment");

        sb.append("CREATE TABLE `").append(tableName).append("` (\n");

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

            StringBuilder colDef = new StringBuilder("    `").append(colName).append("` ").append(dataType);
            if (!nullable) colDef.append(" NOT NULL");
            if (isAutoIncrement) colDef.append(" AUTO_INCREMENT");
            if (defaultVal != null && !defaultVal.isEmpty()) colDef.append(" DEFAULT ").append(defaultVal);
            if (isUnique && !isPk) colDef.append(" UNIQUE");

            colDefs.add(colDef.toString());
            if (isPk) pkColumns.add("`" + colName + "`");
        }

        if (!pkColumns.isEmpty()) {
            colDefs.add("    PRIMARY KEY (" + String.join(", ", pkColumns) + ")");
        }

        sb.append(String.join(",\n", colDefs)).append("\n)");
        if (comment != null && !comment.isEmpty()) {
            sb.append(" COMMENT='").append(comment.replace("'", "\\'")).append("'");
        }
        sb.append(" ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;\n");
    }

    private String mapDataType(String type) {
        if (type == null) return "TEXT";
        return switch (type.toUpperCase()) {
            case "INTEGER", "INT" -> "INT";
            case "BIGINT" -> "BIGINT";
            case "SMALLINT" -> "SMALLINT";
            case "SERIAL" -> "INT";
            case "BIGSERIAL" -> "BIGINT";
            case "DECIMAL", "NUMERIC" -> "DECIMAL";
            case "FLOAT", "REAL" -> "FLOAT";
            case "DOUBLE", "DOUBLE PRECISION" -> "DOUBLE";
            case "BOOLEAN", "BOOL" -> "TINYINT(1)";
            case "DATE" -> "DATE";
            case "TIMESTAMP", "TIMESTAMPTZ", "DATETIME" -> "DATETIME";
            case "UUID", "UNIQUEIDENTIFIER" -> "VARCHAR(36)";
            case "JSONB", "JSON" -> "JSON";
            case "TEXT", "CLOB", "NTEXT" -> "TEXT";
            case "LONGTEXT" -> "LONGTEXT";
            case "BYTEA", "BLOB", "VARBINARY" -> "BLOB";
            default -> type;
        };
    }
}
