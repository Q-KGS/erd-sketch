package com.erdsketch.diff;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
public class MigrationDdlGenerator {

    private final ObjectMapper objectMapper = new ObjectMapper();

    public String generate(DiffResult diff) {
        return generate(diff, "postgresql");
    }

    public String generate(DiffResult diff, String dialect) {
        return generate(diff, null, null, dialect);
    }

    public String generate(DiffResult diff, String baseSchema, String currentSchema) {
        return generate(diff, baseSchema, currentSchema, "postgresql");
    }

    public String generate(DiffResult diff, String baseSchema, String currentSchema, String dialect) {
        if (diff == null || !diff.hasChanges()) {
            return "-- No changes detected";
        }

        Map<String, Map<String, Object>> currentTablesByName = parseTablesByName(currentSchema);

        StringBuilder sb = new StringBuilder();

        for (SchemaChange change : diff.changes()) {
            switch (change.type()) {
                case ADD_TABLE -> {
                    Map<String, Object> tableData = currentTablesByName.get(change.tableName());
                    sb.append(generateCreateTable(change.tableName(), tableData, dialect));
                    sb.append("\n");
                }
                case DROP_TABLE -> {
                    sb.append("DROP TABLE IF EXISTS \"").append(change.tableName()).append("\";\n");
                }
                case ADD_COLUMN -> {
                    String colDef = buildColumnDef(change.columnName(), change.toValue(), true, dialect);
                    sb.append("ALTER TABLE \"").append(change.tableName()).append("\" ADD COLUMN ").append(colDef).append(";\n");
                }
                case DROP_COLUMN -> {
                    sb.append("ALTER TABLE \"").append(change.tableName()).append("\" DROP COLUMN \"").append(change.columnName()).append("\";\n");
                }
                case MODIFY_COLUMN -> {
                    String fromVal = change.fromValue();
                    String toVal = change.toValue();
                    boolean isNullableChange = "true".equals(fromVal) || "false".equals(fromVal)
                            || "true".equals(toVal) || "false".equals(toVal);
                    if (isNullableChange) {
                        boolean nullable = Boolean.parseBoolean(toVal);
                        sb.append(generateModifyNullable(change.tableName(), change.columnName(), nullable, dialect));
                    } else {
                        sb.append(generateModifyType(change.tableName(), change.columnName(), toVal, dialect));
                    }
                    sb.append("\n");
                }
            }
        }

        return sb.toString().trim();
    }

    private String generateCreateTable(String tableName, Map<String, Object> tableData, String dialect) {
        if (tableData == null) {
            return "CREATE TABLE \"" + tableName + "\" (-- see full schema\n);\n";
        }

        StringBuilder sb = new StringBuilder();
        sb.append("CREATE TABLE \"").append(tableName).append("\" (\n");

        @SuppressWarnings("unchecked")
        List<Object> columns = tableData.get("columns") instanceof List
                ? (List<Object>) tableData.get("columns")
                : List.of();

        List<String> colDefs = new ArrayList<>();
        for (Object colObj : columns) {
            if (colObj instanceof Map) {
                @SuppressWarnings("unchecked")
                Map<String, Object> col = (Map<String, Object>) colObj;
                String colName = String.valueOf(col.getOrDefault("name", ""));
                String dataType = String.valueOf(col.getOrDefault("dataType", "TEXT"));
                boolean nullable = col.get("nullable") == null || Boolean.parseBoolean(String.valueOf(col.get("nullable")));
                if (!colName.isEmpty()) {
                    String def = "    \"" + colName + "\" " + dataType + (nullable ? "" : " NOT NULL");
                    colDefs.add(def);
                }
            }
        }

        sb.append(String.join(",\n", colDefs));
        sb.append("\n);\n");
        return sb.toString();
    }

    private String buildColumnDef(String columnName, String dataType, boolean nullable, String dialect) {
        String type = dataType != null && !dataType.isEmpty() ? dataType : "TEXT";
        return "\"" + columnName + "\" " + type + (nullable ? "" : " NOT NULL");
    }

    private String generateModifyType(String tableName, String columnName, String newType, String dialect) {
        return switch (dialect.toLowerCase()) {
            case "mysql" -> "ALTER TABLE `" + tableName + "` MODIFY COLUMN `" + columnName + "` " + newType + ";";
            case "mssql", "sqlserver" -> "ALTER TABLE [" + tableName + "] ALTER COLUMN [" + columnName + "] " + newType + ";";
            default -> // postgresql
                    "ALTER TABLE \"" + tableName + "\" ALTER COLUMN \"" + columnName + "\" TYPE " + newType + ";";
        };
    }

    private String generateModifyNullable(String tableName, String columnName, boolean nullable, String dialect) {
        return switch (dialect.toLowerCase()) {
            case "mysql" -> "-- MySQL: Use MODIFY COLUMN to change nullable for `" + columnName + "` in `" + tableName + "`;";
            case "mssql", "sqlserver" -> "-- MSSQL: ALTER TABLE [" + tableName + "] ALTER COLUMN [" + columnName + "] " + (nullable ? "NULL" : "NOT NULL") + ";";
            default -> // postgresql
                    "ALTER TABLE \"" + tableName + "\" ALTER COLUMN \"" + columnName + "\" " + (nullable ? "DROP NOT NULL" : "SET NOT NULL") + ";";
        };
    }

    @SuppressWarnings("unchecked")
    private Map<String, Map<String, Object>> parseTablesByName(String schemaJson) {
        Map<String, Map<String, Object>> result = new LinkedHashMap<>();
        if (schemaJson == null || schemaJson.isBlank()) {
            return result;
        }
        try {
            Map<String, Object> schema = objectMapper.readValue(schemaJson, new TypeReference<>() {});
            Object tablesObj = schema.get("tables");
            if (!(tablesObj instanceof Map)) {
                return result;
            }
            Map<String, Object> tables = (Map<String, Object>) tablesObj;
            for (Map.Entry<String, Object> entry : tables.entrySet()) {
                if (entry.getValue() instanceof Map) {
                    Map<String, Object> tableData = (Map<String, Object>) entry.getValue();
                    String tableName = String.valueOf(tableData.getOrDefault("name", entry.getKey()));
                    result.put(tableName, tableData);
                }
            }
        } catch (Exception e) {
            // ignore
        }
        return result;
    }
}
