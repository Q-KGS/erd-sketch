package com.erdsketch.diff;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
@RequiredArgsConstructor
public class SchemaDiffService {

    private final ObjectMapper objectMapper;

    public DiffResult diff(String baseSchemaJson, String currentSchemaJson) {
        if (baseSchemaJson == null && currentSchemaJson == null) {
            return DiffResult.empty();
        }

        Map<String, Object> baseSchema = parseSchema(baseSchemaJson);
        Map<String, Object> currentSchema = parseSchema(currentSchemaJson);

        Map<String, Map<String, Object>> baseTables = extractTablesByName(baseSchema);
        Map<String, Map<String, Object>> currentTables = extractTablesByName(currentSchema);

        List<SchemaChange> changes = new ArrayList<>();

        // ADD_TABLE: 현재에 있고 베이스에 없는 테이블
        for (String tableName : currentTables.keySet()) {
            if (!baseTables.containsKey(tableName)) {
                changes.add(new SchemaChange(
                        SchemaChangeType.ADD_TABLE,
                        tableName,
                        null,
                        null,
                        null,
                        "Table '" + tableName + "' added"
                ));
            }
        }

        // DROP_TABLE: 베이스에 있고 현재에 없는 테이블
        for (String tableName : baseTables.keySet()) {
            if (!currentTables.containsKey(tableName)) {
                changes.add(new SchemaChange(
                        SchemaChangeType.DROP_TABLE,
                        tableName,
                        null,
                        null,
                        null,
                        "Table '" + tableName + "' dropped"
                ));
            }
        }

        // 같은 이름의 테이블: 컬럼 비교
        for (String tableName : currentTables.keySet()) {
            if (baseTables.containsKey(tableName)) {
                Map<String, Map<String, Object>> baseColumns = extractColumnsByName(baseTables.get(tableName));
                Map<String, Map<String, Object>> currentColumns = extractColumnsByName(currentTables.get(tableName));

                // ADD_COLUMN
                for (String colName : currentColumns.keySet()) {
                    if (!baseColumns.containsKey(colName)) {
                        Map<String, Object> col = currentColumns.get(colName);
                        String dataType = String.valueOf(col.getOrDefault("dataType", ""));
                        changes.add(new SchemaChange(
                                SchemaChangeType.ADD_COLUMN,
                                tableName,
                                colName,
                                null,
                                dataType,
                                "Column '" + colName + "' added to table '" + tableName + "'"
                        ));
                    }
                }

                // DROP_COLUMN
                for (String colName : baseColumns.keySet()) {
                    if (!currentColumns.containsKey(colName)) {
                        Map<String, Object> col = baseColumns.get(colName);
                        String dataType = String.valueOf(col.getOrDefault("dataType", ""));
                        changes.add(new SchemaChange(
                                SchemaChangeType.DROP_COLUMN,
                                tableName,
                                colName,
                                dataType,
                                null,
                                "Column '" + colName + "' dropped from table '" + tableName + "'"
                        ));
                    }
                }

                // MODIFY_COLUMN
                for (String colName : currentColumns.keySet()) {
                    if (baseColumns.containsKey(colName)) {
                        Map<String, Object> baseCol = baseColumns.get(colName);
                        Map<String, Object> currentCol = currentColumns.get(colName);

                        String baseType = String.valueOf(baseCol.getOrDefault("dataType", ""));
                        String currentType = String.valueOf(currentCol.getOrDefault("dataType", ""));

                        if (!Objects.equals(baseType, currentType)) {
                            changes.add(new SchemaChange(
                                    SchemaChangeType.MODIFY_COLUMN,
                                    tableName,
                                    colName,
                                    baseType,
                                    currentType,
                                    "Column '" + colName + "' in table '" + tableName + "' type changed from " + baseType + " to " + currentType
                            ));
                        }

                        Object baseNullable = baseCol.get("nullable");
                        Object currentNullable = currentCol.get("nullable");

                        if (!Objects.equals(baseNullable, currentNullable)) {
                            String fromVal = baseNullable != null ? baseNullable.toString() : "true";
                            String toVal = currentNullable != null ? currentNullable.toString() : "true";
                            changes.add(new SchemaChange(
                                    SchemaChangeType.MODIFY_COLUMN,
                                    tableName,
                                    colName,
                                    fromVal,
                                    toVal,
                                    "Column '" + colName + "' in table '" + tableName + "' nullable changed from " + fromVal + " to " + toVal
                            ));
                        }
                    }
                }
            }
        }

        return DiffResult.of(changes);
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> parseSchema(String schemaJson) {
        if (schemaJson == null || schemaJson.isBlank()) {
            return Map.of();
        }
        try {
            return objectMapper.readValue(schemaJson, new TypeReference<Map<String, Object>>() {});
        } catch (Exception e) {
            return Map.of();
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, Map<String, Object>> extractTablesByName(Map<String, Object> schema) {
        Map<String, Map<String, Object>> result = new LinkedHashMap<>();
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
        return result;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Map<String, Object>> extractColumnsByName(Map<String, Object> tableData) {
        Map<String, Map<String, Object>> result = new LinkedHashMap<>();
        Object columnsObj = tableData.get("columns");
        if (!(columnsObj instanceof List)) {
            return result;
        }
        List<Object> columns = (List<Object>) columnsObj;
        for (Object colObj : columns) {
            if (colObj instanceof Map) {
                Map<String, Object> col = (Map<String, Object>) colObj;
                String colName = String.valueOf(col.getOrDefault("name", ""));
                if (!colName.isEmpty()) {
                    result.put(colName, col);
                }
            }
        }
        return result;
    }
}
