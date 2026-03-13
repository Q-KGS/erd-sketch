package com.erdsketch.dbml;

import org.springframework.stereotype.Component;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class DbmlParser {

    private static final Pattern TABLE_HEADER = Pattern.compile(
            "^\\s*[Tt]able\\s+(\\S+)\\s*\\{\\s*$");
    private static final Pattern COLUMN_LINE = Pattern.compile(
            "^\\s*(\\S+)\\s+(\\S+)(?:\\s+\\[([^]]*)])?\\s*$");
    private static final Pattern REF_LINE = Pattern.compile(
            "^\\s*[Rr]ef\\s*:\\s*(\\S+)\\.(\\S+)\\s*([><-])\\s*(\\S+)\\.(\\S+)(?:\\s+\\[([^]]*)])?\\s*$");
    private static final Pattern DEFAULT_VALUE = Pattern.compile(
            "default:\\s*'([^']*)'|default:\\s*(\\S+)");
    private static final Pattern INLINE_REF = Pattern.compile(
            "ref:\\s*([><-])\\s*(\\S+)\\.(\\S+)");

    public DbmlParseResult parse(String dbml) {
        List<Map<String, Object>> tables = new ArrayList<>();
        List<Map<String, Object>> relationships = new ArrayList<>();
        List<String> warnings = new ArrayList<>();
        Map<String, Map<String, Object>> tablesByName = new LinkedHashMap<>();
        List<String[]> inlineRefs = new ArrayList<>();

        if (dbml == null || dbml.isBlank()) {
            return new DbmlParseResult(tables, relationships, warnings);
        }

        String[] lines = dbml.split("\\r?\\n");
        int i = 0;
        while (i < lines.length) {
            String line = lines[i];
            String stripped = stripComment(line).strip();

            if (stripped.isEmpty()) {
                i++;
                continue;
            }

            // Table block
            Matcher tableMatcher = TABLE_HEADER.matcher(stripped);
            if (tableMatcher.matches()) {
                String tableName = tableMatcher.group(1);
                if (tableName == null || tableName.isBlank()) {
                    warnings.add("Line " + (i + 1) + ": Table has no name");
                    i++;
                    continue;
                }
                Map<String, Object> table = new LinkedHashMap<>();
                table.put("id", UUID.randomUUID().toString());
                table.put("name", tableName);
                table.put("position", new LinkedHashMap<>(Map.of("x", 0, "y", 0)));
                List<Map<String, Object>> columns = new ArrayList<>();
                table.put("columns", columns);
                table.put("indexes", new ArrayList<>());

                i++;
                while (i < lines.length) {
                    String colLine = stripComment(lines[i]).strip();
                    if (colLine.equals("}")) {
                        i++;
                        break;
                    }
                    if (!colLine.isEmpty()) {
                        parseColumn(colLine, columns, tableName, inlineRefs, warnings, i + 1);
                    }
                    i++;
                }

                tablesByName.put(tableName, table);
                tables.add(table);
                continue;
            }

            // Ref line
            Matcher refMatcher = REF_LINE.matcher(stripped);
            if (refMatcher.matches()) {
                String tableA = refMatcher.group(1);
                String colA = refMatcher.group(2);
                String operator = refMatcher.group(3);
                String tableB = refMatcher.group(4);
                String colB = refMatcher.group(5);
                String options = refMatcher.group(6);

                Map<String, Object> rel = buildRelationship(tableA, colA, operator, tableB, colB, options);
                relationships.add(rel);
                i++;
                continue;
            }

            i++;
        }

        // Process inline refs
        for (String[] ref : inlineRefs) {
            // ref: [tableName, colName, operator, refTable, refCol]
            String tableA = ref[0];
            String colA = ref[1];
            String operator = ref[2];
            String tableB = ref[3];
            String colB = ref[4];
            Map<String, Object> rel = buildRelationship(tableA, colA, operator, tableB, colB, null);
            relationships.add(rel);
        }

        return new DbmlParseResult(tables, relationships, warnings);
    }

    private void parseColumn(String line, List<Map<String, Object>> columns,
            String tableName, List<String[]> inlineRefs,
            List<String> warnings, int lineNum) {
        Matcher m = COLUMN_LINE.matcher(line);
        if (!m.matches()) {
            warnings.add("Line " + lineNum + ": Cannot parse column: " + line);
            return;
        }

        String colName = m.group(1);
        String colType = m.group(2);
        String optionsStr = m.group(3);

        Map<String, Object> col = new LinkedHashMap<>();
        col.put("id", UUID.randomUUID().toString());
        col.put("name", colName);
        col.put("type", colType);
        col.put("nullable", true);
        col.put("isPrimaryKey", false);
        col.put("isAutoIncrement", false);
        col.put("isUnique", false);
        col.put("defaultValue", null);

        if (optionsStr != null && !optionsStr.isBlank()) {
            parseColumnOptions(optionsStr, col, tableName, colName, inlineRefs);
        }

        columns.add(col);
    }

    private void parseColumnOptions(String optionsStr, Map<String, Object> col,
            String tableName, String colName, List<String[]> inlineRefs) {
        // Split by comma, but be careful with nested quotes
        String[] parts = optionsStr.split(",");

        for (String part : parts) {
            String opt = part.strip().toLowerCase();

            if (opt.equals("pk")) {
                col.put("isPrimaryKey", true);
                col.put("nullable", false);
            } else if (opt.equals("increment") || opt.equals("autoincrement") || opt.equals("auto_increment")) {
                col.put("isAutoIncrement", true);
            } else if (opt.equals("not null")) {
                col.put("nullable", false);
            } else if (opt.equals("null")) {
                col.put("nullable", true);
            } else if (opt.equals("unique")) {
                col.put("isUnique", true);
            } else if (opt.strip().toLowerCase().startsWith("default:")) {
                // Parse default value from original (not lowercased)
                Matcher dm = DEFAULT_VALUE.matcher(part.strip());
                if (dm.find()) {
                    String val = dm.group(1) != null ? dm.group(1) : dm.group(2);
                    col.put("defaultValue", val);
                }
            } else if (opt.strip().toLowerCase().startsWith("ref:")) {
                // Inline ref
                Matcher rm = INLINE_REF.matcher(part.strip());
                if (rm.find()) {
                    String operator = rm.group(1);
                    String refTable = rm.group(2);
                    String refCol = rm.group(3);
                    inlineRefs.add(new String[]{tableName, colName, operator, refTable, refCol});
                }
            }
        }
    }

    private Map<String, Object> buildRelationship(String tableA, String colA, String operator,
            String tableB, String colB, String options) {
        Map<String, Object> rel = new LinkedHashMap<>();
        rel.put("id", UUID.randomUUID().toString());

        String cardinality;
        String sourceTable;
        String sourceCol;
        String targetTable;
        String targetCol;

        switch (operator) {
            case ">" -> {
                // tableA.colA > tableB.colB => tableA has FK pointing to tableB (1:N from B's perspective)
                cardinality = "1:N";
                sourceTable = tableA;
                sourceCol = colA;
                targetTable = tableB;
                targetCol = colB;
            }
            case "<" -> {
                // tableA.colA < tableB.colB => tableB has FK pointing to tableA (N:1)
                cardinality = "1:N";
                sourceTable = tableB;
                sourceCol = colB;
                targetTable = tableA;
                targetCol = colA;
            }
            case "-" -> {
                // 1:1
                cardinality = "1:1";
                sourceTable = tableA;
                sourceCol = colA;
                targetTable = tableB;
                targetCol = colB;
            }
            default -> {
                cardinality = "1:N";
                sourceTable = tableA;
                sourceCol = colA;
                targetTable = tableB;
                targetCol = colB;
            }
        }

        rel.put("sourceTable", sourceTable);
        rel.put("sourceColumn", sourceCol);
        rel.put("targetTable", targetTable);
        rel.put("targetColumn", targetCol);
        rel.put("cardinality", cardinality);

        // Parse options
        String onDelete = "NO_ACTION";
        String onUpdate = "NO_ACTION";
        if (options != null) {
            String opts = options.toLowerCase();
            if (opts.contains("delete: cascade") || opts.contains("delete:cascade")) onDelete = "CASCADE";
            else if (opts.contains("delete: set null") || opts.contains("delete:set null")) onDelete = "SET_NULL";
            else if (opts.contains("delete: restrict") || opts.contains("delete:restrict")) onDelete = "RESTRICT";

            if (opts.contains("update: cascade") || opts.contains("update:cascade")) onUpdate = "CASCADE";
            else if (opts.contains("update: set null") || opts.contains("update:set null")) onUpdate = "SET_NULL";
            else if (opts.contains("update: restrict") || opts.contains("update:restrict")) onUpdate = "RESTRICT";
        }
        rel.put("onDelete", onDelete);
        rel.put("onUpdate", onUpdate);

        return rel;
    }

    private String stripComment(String line) {
        int idx = line.indexOf("//");
        if (idx >= 0) {
            return line.substring(0, idx);
        }
        return line;
    }
}
