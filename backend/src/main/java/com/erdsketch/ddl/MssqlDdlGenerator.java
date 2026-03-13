package com.erdsketch.ddl;

import org.springframework.stereotype.Component;

import java.util.*;

@Component
public class MssqlDdlGenerator {

    @SuppressWarnings("unchecked")
    public String generate(Map<String, Object> schema, List<String> tableIds, boolean includeDrops, List<String> warnings) {
        StringBuilder sb = new StringBuilder();
        Map<String, Object> tables = (Map<String, Object>) schema.getOrDefault("tables", Map.of());
        Map<String, Object> relationships = (Map<String, Object>) schema.getOrDefault("relationships", Map.of());

        Collection<Object> tableValues = tableIds != null && !tableIds.isEmpty()
                ? tableIds.stream().map(tables::get).filter(Objects::nonNull).toList()
                : tables.values();

        if (tableValues.isEmpty() && relationships.values().isEmpty()) {
            return "-- No schema defined yet";
        }

        if (includeDrops) {
            tableValues.forEach(tableObj -> {
                Map<String, Object> table = (Map<String, Object>) tableObj;
                String name = (String) table.get("name");
                sb.append("IF OBJECT_ID(N'").append(name.replace("'", "''"))
                  .append("', N'U') IS NOT NULL DROP TABLE ").append(quoteIdentifier(name)).append(";\n");
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
        String tableComment = (String) table.get("comment");

        sb.append("CREATE TABLE ").append(quoteIdentifier(tableName)).append(" (\n");

        List<String> colDefs = new ArrayList<>();
        List<String> pkColumns = new ArrayList<>();

        for (Map<String, Object> col : columns) {
            String colName = (String) col.get("name");
            String rawType = (String) col.get("dataType");
            String dataType = mapDataType(rawType, warnings);
            boolean nullable = (boolean) col.getOrDefault("nullable", true);
            boolean isPk = (boolean) col.getOrDefault("isPrimaryKey", false);
            boolean isUnique = (boolean) col.getOrDefault("isUnique", false);
            boolean isAutoIncrement = (boolean) col.getOrDefault("isAutoIncrement", false);
            String defaultVal = (String) col.get("defaultValue");

            StringBuilder colDef = new StringBuilder("    ").append(quoteIdentifier(colName)).append(" ").append(dataType);
            if (isAutoIncrement) colDef.append(" IDENTITY(1,1)");
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

        // Table comment via extended property
        if (tableComment != null && !tableComment.isEmpty()) {
            sb.append("EXEC sp_addextendedproperty N'MS_Description', N'")
              .append(tableComment.replace("'", "''"))
              .append("', N'SCHEMA', N'dbo', N'TABLE', N'").append(tableName.replace("'", "''")).append("';\n");
        }

        // Column comments via extended property
        for (Map<String, Object> col : columns) {
            String colComment = (String) col.get("comment");
            String colName = (String) col.get("name");
            if (colComment != null && !colComment.isEmpty()) {
                sb.append("EXEC sp_addextendedproperty N'MS_Description', N'")
                  .append(colComment.replace("'", "''"))
                  .append("', N'SCHEMA', N'dbo', N'TABLE', N'").append(tableName.replace("'", "''"))
                  .append("', N'COLUMN', N'").append(colName.replace("'", "''")).append("';\n");
            }
        }
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

        List<String> srcColNames = resolveColumnNames(srcTable, srcCols);
        List<String> tgtColNames = resolveColumnNames(tgtTable, tgtCols);

        sb.append("ALTER TABLE ").append(quoteIdentifier(srcTableName))
          .append(" ADD CONSTRAINT fk_").append(srcTableName).append("_").append(tgtTableName)
          .append(" FOREIGN KEY (").append(String.join(", ", srcColNames.stream().map(this::quoteIdentifier).toList()))
          .append(") REFERENCES ").append(quoteIdentifier(tgtTableName))
          .append(" (").append(String.join(", ", tgtColNames.stream().map(this::quoteIdentifier).toList()))
          .append(") ON DELETE ").append(mapReferentialAction(onDelete))
          .append(" ON UPDATE ").append(mapReferentialAction(onUpdate))
          .append(";\n");
    }

    private String mapReferentialAction(String action) {
        if (action == null) return "NO ACTION";
        return switch (action.toUpperCase().replace(" ", "_")) {
            case "CASCADE" -> "CASCADE";
            case "SET_NULL" -> "SET NULL";
            case "RESTRICT" -> "NO ACTION"; // MSSQL doesn't have RESTRICT, use NO ACTION
            case "NO_ACTION" -> "NO ACTION";
            default -> "NO ACTION";
        };
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

    private String mapDataType(String type, List<String> warnings) {
        if (type == null) return "NVARCHAR(MAX)";
        String upper = type.toUpperCase();
        if (upper.equals("JSONB") || upper.equals("JSON")) {
            warnings.add("JSONB/JSON mapped to NVARCHAR(MAX) in MSSQL; consider using native JSON support");
        }
        return switch (upper) {
            case "INT", "INTEGER" -> "INT";
            case "BIGINT" -> "BIGINT";
            case "SMALLINT" -> "SMALLINT";
            case "SERIAL" -> "INT";
            case "BIGSERIAL" -> "BIGINT";
            case "DECIMAL", "NUMERIC" -> "DECIMAL";
            case "FLOAT" -> "FLOAT";
            case "REAL" -> "REAL";
            case "DOUBLE", "DOUBLE PRECISION" -> "FLOAT";
            case "BOOLEAN", "BOOL", "BIT" -> "BIT";
            case "DATE" -> "DATE";
            case "DATETIME" -> "DATETIME";
            case "TIMESTAMP", "TIMESTAMPTZ" -> "DATETIME2";
            case "UUID", "UNIQUEIDENTIFIER" -> "UNIQUEIDENTIFIER";
            case "VARCHAR", "VARCHAR2" -> "NVARCHAR(255)";
            case "NVARCHAR" -> "NVARCHAR(255)";
            case "CHAR" -> "CHAR";
            case "JSON", "JSONB" -> "NVARCHAR(MAX)";
            case "TEXT", "CLOB", "LONGTEXT", "NTEXT" -> "NVARCHAR(MAX)";
            case "BYTEA", "BLOB", "VARBINARY" -> "VARBINARY(MAX)";
            default -> type;
        };
    }

    private String quoteIdentifier(String name) {
        if (name == null) return "[]";
        return "[" + name.replace("]", "]]") + "]";
    }
}
