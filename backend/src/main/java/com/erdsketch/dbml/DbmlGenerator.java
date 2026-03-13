package com.erdsketch.dbml;

import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
public class DbmlGenerator {

    public String generate(List<Map<String, Object>> tables, List<Map<String, Object>> relationships) {
        StringBuilder sb = new StringBuilder();

        for (Map<String, Object> table : tables) {
            String tableName = (String) table.get("name");
            sb.append("Table ").append(tableName).append(" {\n");

            @SuppressWarnings("unchecked")
            List<Map<String, Object>> columns = (List<Map<String, Object>>) table.get("columns");
            if (columns != null) {
                for (Map<String, Object> col : columns) {
                    String colName = (String) col.get("name");
                    String colType = (String) col.get("type");
                    if (colType == null) colType = "varchar";

                    sb.append("  ").append(colName).append(" ").append(colType);

                    List<String> options = new java.util.ArrayList<>();
                    if (Boolean.TRUE.equals(col.get("isPrimaryKey"))) {
                        options.add("pk");
                    }
                    if (Boolean.TRUE.equals(col.get("isAutoIncrement"))) {
                        options.add("increment");
                    }
                    if (Boolean.FALSE.equals(col.get("nullable")) && !Boolean.TRUE.equals(col.get("isPrimaryKey"))) {
                        options.add("not null");
                    }
                    if (Boolean.TRUE.equals(col.get("isUnique"))) {
                        options.add("unique");
                    }
                    Object defaultVal = col.get("defaultValue");
                    if (defaultVal != null) {
                        options.add("default: '" + defaultVal + "'");
                    }

                    if (!options.isEmpty()) {
                        sb.append(" [").append(String.join(", ", options)).append("]");
                    }
                    sb.append("\n");
                }
            }

            sb.append("}\n\n");
        }

        for (Map<String, Object> rel : relationships) {
            String sourceTable = (String) rel.get("sourceTable");
            String sourceCol = (String) rel.get("sourceColumn");
            String targetTable = (String) rel.get("targetTable");
            String targetCol = (String) rel.get("targetColumn");
            String cardinality = (String) rel.get("cardinality");

            String operator = switch (cardinality != null ? cardinality : "1:N") {
                case "1:1" -> "-";
                default -> ">";
            };

            sb.append("Ref: ").append(sourceTable).append(".").append(sourceCol)
                    .append(" ").append(operator).append(" ")
                    .append(targetTable).append(".").append(targetCol);

            String onDelete = (String) rel.get("onDelete");
            String onUpdate = (String) rel.get("onUpdate");
            List<String> opts = new java.util.ArrayList<>();
            if (onDelete != null && !onDelete.equals("NO_ACTION")) {
                opts.add("delete: " + onDelete.toLowerCase().replace("_", " "));
            }
            if (onUpdate != null && !onUpdate.equals("NO_ACTION")) {
                opts.add("update: " + onUpdate.toLowerCase().replace("_", " "));
            }
            if (!opts.isEmpty()) {
                sb.append(" [").append(String.join(", ", opts)).append("]");
            }
            sb.append("\n");
        }

        return sb.toString().trim();
    }
}
