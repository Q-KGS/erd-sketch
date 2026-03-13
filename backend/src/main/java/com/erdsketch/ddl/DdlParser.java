package com.erdsketch.ddl;

import net.sf.jsqlparser.parser.CCJSqlParserUtil;
import net.sf.jsqlparser.statement.Statement;
import net.sf.jsqlparser.statement.Statements;
import net.sf.jsqlparser.statement.alter.Alter;
import net.sf.jsqlparser.statement.alter.AlterExpression;
import net.sf.jsqlparser.statement.create.index.CreateIndex;
import net.sf.jsqlparser.statement.create.table.ColDataType;
import net.sf.jsqlparser.statement.create.table.ColumnDefinition;
import net.sf.jsqlparser.statement.create.table.CreateTable;
import net.sf.jsqlparser.statement.create.table.ForeignKeyIndex;
import net.sf.jsqlparser.statement.create.table.Index;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
public class DdlParser {

    public ParseResult parse(String ddl) {
        List<Map<String, Object>> tables = new ArrayList<>();
        List<Map<String, Object>> relationships = new ArrayList<>();
        List<String> warnings = new ArrayList<>();
        Map<String, Map<String, Object>> tablesByName = new LinkedHashMap<>();

        try {
            Statements statements = CCJSqlParserUtil.parseStatements(
                    ddl.endsWith(";") ? ddl : ddl + ";");

            for (Statement statement : statements.getStatements()) {
                if (statement instanceof CreateTable ct) {
                    Map<String, Object> table = parseCreateTable(ct, warnings);
                    tables.add(table);
                    tablesByName.put(unquote(ct.getTable().getName()).toLowerCase(), table);
                } else if (statement instanceof CreateIndex ci) {
                    String tableName = unquote(ci.getTable().getName()).toLowerCase();
                    Map<String, Object> targetTable = tablesByName.get(tableName);
                    if (targetTable != null) {
                        addIndexToTable(targetTable, ci);
                    } else {
                        warnings.add("CREATE INDEX: table not found: " + tableName);
                    }
                } else if (statement instanceof Alter alter) {
                    parseAlterTable(alter, tablesByName, relationships, warnings);
                }
            }
        } catch (Exception e) {
            warnings.add("Parse error: " + e.getMessage());
        }

        return new ParseResult(tables, relationships, warnings);
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> parseCreateTable(CreateTable ct, List<String> warnings) {
        String tableName = unquote(ct.getTable().getName());

        Map<String, Object> table = new LinkedHashMap<>();
        table.put("id", UUID.randomUUID().toString());
        table.put("name", tableName);
        table.put("position", Map.of("x", 0, "y", 0));

        List<Map<String, Object>> columnMaps = new ArrayList<>();
        Set<String> pkColumnNames = new LinkedHashSet<>();

        if (ct.getColumnDefinitions() != null) {
            int order = 0;
            for (ColumnDefinition col : ct.getColumnDefinitions()) {
                Map<String, Object> colMap = parseColumnDefinition(col, order++);
                columnMaps.add(colMap);
                if ((boolean) colMap.getOrDefault("isPrimaryKey", false)) {
                    pkColumnNames.add((String) colMap.get("name"));
                }
            }
        }

        // Table-level constraints
        if (ct.getIndexes() != null) {
            for (Index idx : ct.getIndexes()) {
                String type = idx.getType() != null ? idx.getType().toUpperCase() : "";
                if (type.contains("PRIMARY KEY") && idx.getColumnsNames() != null) {
                    idx.getColumnsNames().stream().map(this::unquote).forEach(pkColumnNames::add);
                }
            }
        }

        // Apply table-level PKs
        for (Map<String, Object> col : columnMaps) {
            if (pkColumnNames.contains(col.get("name"))) {
                col.put("isPrimaryKey", true);
                col.put("nullable", false);
            }
        }

        table.put("columns", columnMaps);
        table.put("indexes", new ArrayList<>());
        return table;
    }

    private Map<String, Object> parseColumnDefinition(ColumnDefinition col, int order) {
        Map<String, Object> colMap = new LinkedHashMap<>();
        colMap.put("id", UUID.randomUUID().toString());
        colMap.put("name", unquote(col.getColumnName()));

        ColDataType dataType = col.getColDataType();
        String typeName = dataType.getDataType().toUpperCase();

        boolean isAutoIncrement = false;
        if ("SERIAL".equals(typeName)) {
            isAutoIncrement = true;
            typeName = "INTEGER";
        } else if ("BIGSERIAL".equals(typeName)) {
            isAutoIncrement = true;
            typeName = "BIGINT";
        } else if ("SMALLSERIAL".equals(typeName)) {
            isAutoIncrement = true;
            typeName = "SMALLINT";
        }

        colMap.put("dataType", typeName);

        boolean nullable = true;
        boolean isPk = false;
        boolean isUnique = false;
        String defaultValue = null;
        String comment = null;

        List<String> specs = col.getColumnSpecs();
        if (specs != null) {
            for (int i = 0; i < specs.size(); i++) {
                String spec = specs.get(i).toUpperCase();
                if ("NOT".equals(spec) && i + 1 < specs.size()
                        && "NULL".equalsIgnoreCase(specs.get(i + 1))) {
                    nullable = false;
                } else if ("PRIMARY".equals(spec) && i + 1 < specs.size()
                        && "KEY".equalsIgnoreCase(specs.get(i + 1))) {
                    isPk = true;
                    nullable = false;
                } else if ("UNIQUE".equals(spec)) {
                    isUnique = true;
                } else if ("AUTO_INCREMENT".equals(spec) || "AUTOINCREMENT".equals(spec)) {
                    isAutoIncrement = true;
                } else if ("DEFAULT".equals(spec) && i + 1 < specs.size()) {
                    defaultValue = specs.get(i + 1);
                } else if ("COMMENT".equals(spec) && i + 1 < specs.size()) {
                    comment = specs.get(i + 1).replaceAll("^['\"]|['\"]$", "");
                }
            }
        }

        colMap.put("nullable", nullable);
        colMap.put("isPrimaryKey", isPk);
        colMap.put("isUnique", isUnique);
        colMap.put("isAutoIncrement", isAutoIncrement);
        if (defaultValue != null) colMap.put("defaultValue", defaultValue);
        if (comment != null) colMap.put("comment", comment);
        colMap.put("order", order);

        return colMap;
    }

    @SuppressWarnings("unchecked")
    private void addIndexToTable(Map<String, Object> table, CreateIndex ci) {
        List<Map<String, Object>> indexes = (List<Map<String, Object>>) table.get("indexes");

        Map<String, Object> idxMap = new LinkedHashMap<>();
        idxMap.put("id", UUID.randomUUID().toString());

        String name = ci.getIndex().getName() != null
                ? unquote(ci.getIndex().getName()) : "idx_unnamed";
        idxMap.put("name", name);

        // UNIQUE 여부는 Index.getType()이 "UNIQUE INDEX" 또는 "UNIQUE" 포함 여부로 판단
        String idxType = ci.getIndex().getType();
        boolean isUnique = idxType != null && idxType.toUpperCase().contains("UNIQUE");
        idxMap.put("isUnique", isUnique);

        List<String> cols = ci.getIndex().getColumnsNames() != null
                ? ci.getIndex().getColumnsNames().stream().map(this::unquote).toList()
                : List.of();
        idxMap.put("columns", cols);

        // USING BTREE/HASH 등은 Index.getUsing()으로 가져옴
        String usingClause = ci.getIndex().getUsing();
        String indexType = (usingClause != null && !usingClause.isBlank())
                ? usingClause.toUpperCase() : "BTREE";
        idxMap.put("type", indexType);

        indexes.add(idxMap);
    }

    private void parseAlterTable(Alter alter, Map<String, Map<String, Object>> tablesByName,
                                  List<Map<String, Object>> relationships, List<String> warnings) {
        String srcName = unquote(alter.getTable().getName()).toLowerCase();
        Map<String, Object> srcTable = tablesByName.get(srcName);
        if (srcTable == null) {
            warnings.add("ALTER TABLE: source table not found: " + srcName);
            return;
        }

        for (AlterExpression expr : alter.getAlterExpressions()) {
            if (expr.getIndex() instanceof ForeignKeyIndex fk) {
                Map<String, Object> rel = parseForeignKey(fk, srcTable, tablesByName, warnings);
                if (rel != null) relationships.add(rel);
            }
        }
    }

    private Map<String, Object> parseForeignKey(ForeignKeyIndex fk, Map<String, Object> srcTable,
                                                 Map<String, Map<String, Object>> tablesByName,
                                                 List<String> warnings) {
        if (fk.getTable() == null) return null;

        String tgtName = unquote(fk.getTable().getName()).toLowerCase();
        Map<String, Object> tgtTable = tablesByName.get(tgtName);
        if (tgtTable == null) {
            warnings.add("FK: target table not found: " + tgtName);
            return null;
        }

        Map<String, Object> rel = new LinkedHashMap<>();
        rel.put("id", UUID.randomUUID().toString());
        rel.put("sourceTableId", srcTable.get("id"));
        rel.put("targetTableId", tgtTable.get("id"));

        List<String> srcCols = fk.getColumnsNames() != null
                ? fk.getColumnsNames().stream().map(this::unquote).toList() : List.of();
        List<String> tgtCols = fk.getReferencedColumnNames() != null
                ? fk.getReferencedColumnNames().stream().map(this::unquote).toList() : List.of();

        rel.put("sourceColumnNames", srcCols);
        rel.put("targetColumnNames", tgtCols);
        rel.put("cardinality", "1:N");

        String deleteRule = resolveRule(fk, true);
        String updateRule = resolveRule(fk, false);
        rel.put("onDelete", deleteRule);
        rel.put("onUpdate", updateRule);

        return rel;
    }

    private String resolveRule(ForeignKeyIndex fk, boolean isDelete) {
        try {
            String rule = isDelete
                    ? fk.getOnDeleteReferenceOption()
                    : fk.getOnUpdateReferenceOption();
            if (rule != null && !rule.isBlank()) {
                return rule.toUpperCase().replace(" ", "_");
            }
        } catch (Exception ignored) {}
        return "RESTRICT";
    }

    private String unquote(String name) {
        if (name == null) return "";
        String s = name.trim();
        if (s.length() >= 2) {
            char first = s.charAt(0), last = s.charAt(s.length() - 1);
            if ((first == '"' && last == '"') || (first == '`' && last == '`')
                    || (first == '[' && last == ']')) {
                return s.substring(1, s.length() - 1);
            }
        }
        return s;
    }

    public record ParseResult(
            List<Map<String, Object>> tables,
            List<Map<String, Object>> relationships,
            List<String> warnings
    ) {}
}
