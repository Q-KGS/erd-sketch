package com.erdsketch.jdbc;

import org.springframework.stereotype.Component;

import java.sql.*;
import java.util.*;

@Component
public class JdbcSchemaExtractor {

    public List<Map<String, Object>> extractSchema(JdbcConnectionRequest request) {
        String url = buildUrl(request);
        Properties props = new Properties();
        props.setProperty("user", request.username());
        props.setProperty("password", request.password());
        props.setProperty("loginTimeout", "5");
        props.setProperty("connectTimeout", "5");

        try (Connection conn = DriverManager.getConnection(url, props)) {
            return extractTables(conn);
        } catch (SQLTimeoutException e) {
            throw new RuntimeException("Connection timeout", e);
        } catch (SQLException e) {
            throw new IllegalArgumentException("Connection failed: " + e.getMessage(), e);
        }
    }

    private String buildUrl(JdbcConnectionRequest req) {
        String dbType = req.dbType() != null ? req.dbType().toUpperCase() : "POSTGRESQL";
        return switch (dbType) {
            case "MYSQL" -> "jdbc:mysql://" + req.host() + ":" + req.port() + "/" + req.database();
            default -> "jdbc:postgresql://" + req.host() + ":" + req.port() + "/" + req.database();
        };
    }

    String buildUrlPublic(JdbcConnectionRequest req) {
        return buildUrl(req);
    }

    List<Map<String, Object>> extractTables(Connection conn) throws SQLException {
        DatabaseMetaData meta = conn.getMetaData();
        List<Map<String, Object>> tables = new ArrayList<>();

        String schemaPattern = detectSchema(conn);

        try (ResultSet rs = meta.getTables(null, schemaPattern, "%", new String[]{"TABLE"})) {
            while (rs.next()) {
                String tableName = rs.getString("TABLE_NAME");
                Map<String, Object> table = new LinkedHashMap<>();
                table.put("id", UUID.randomUUID().toString());
                table.put("name", tableName);
                table.put("position", Map.of("x", 0, "y", 0));
                table.put("columns", extractColumns(conn, tableName, schemaPattern));
                table.put("indexes", List.of());
                tables.add(table);
            }
        }
        return tables;
    }

    private String detectSchema(Connection conn) {
        try {
            String url = conn.getMetaData().getURL();
            if (url != null && url.startsWith("jdbc:h2:")) {
                return null; // H2: no schema filter
            }
            return "public"; // PostgreSQL default
        } catch (SQLException e) {
            return null;
        }
    }

    private List<Map<String, Object>> extractColumns(Connection conn, String tableName, String schema) throws SQLException {
        DatabaseMetaData meta = conn.getMetaData();
        List<Map<String, Object>> columns = new ArrayList<>();

        // Get primary keys
        Set<String> pkColumns = new HashSet<>();
        try (ResultSet pkRs = meta.getPrimaryKeys(null, schema, tableName)) {
            while (pkRs.next()) {
                pkColumns.add(pkRs.getString("COLUMN_NAME"));
            }
        }

        // Get foreign keys
        Map<String, Map<String, String>> fkMap = new LinkedHashMap<>();
        try (ResultSet fkRs = meta.getImportedKeys(null, schema, tableName)) {
            while (fkRs.next()) {
                String fkColName = fkRs.getString("FKCOLUMN_NAME");
                String pkTableName = fkRs.getString("PKTABLE_NAME");
                String pkColName = fkRs.getString("PKCOLUMN_NAME");
                Map<String, String> fkInfo = new LinkedHashMap<>();
                fkInfo.put("referencedTable", pkTableName);
                fkInfo.put("referencedColumn", pkColName);
                fkMap.put(fkColName.toUpperCase(), fkInfo);
            }
        }

        try (ResultSet rs = meta.getColumns(null, schema, tableName, "%")) {
            while (rs.next()) {
                String colName = rs.getString("COLUMN_NAME");
                String colType = rs.getString("TYPE_NAME");
                int nullable = rs.getInt("NULLABLE");
                String defaultVal = rs.getString("COLUMN_DEF");
                int colSize = rs.getInt("COLUMN_SIZE");

                Map<String, Object> col = new LinkedHashMap<>();
                col.put("id", UUID.randomUUID().toString());
                col.put("name", colName);
                col.put("type", colType);
                col.put("nullable", nullable != DatabaseMetaData.columnNoNulls);
                col.put("isPrimaryKey", pkColumns.contains(colName));
                col.put("isUnique", false);
                col.put("isAutoIncrement", false);
                col.put("defaultValue", defaultVal);
                col.put("columnSize", colSize);

                Map<String, String> fkInfo = fkMap.get(colName.toUpperCase());
                if (fkInfo != null) {
                    col.put("foreignKey", fkInfo);
                }

                columns.add(col);
            }
        }

        return columns;
    }
}
