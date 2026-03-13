package com.erdsketch.template;

import java.util.*;

public class ProjectTemplate {

    public static Map<String, Object> getSchema(ProjectTemplateType type) {
        return switch (type) {
            case ECOMMERCE -> buildEcommerceSchema();
            case BLOG -> buildBlogSchema();
        };
    }

    private static Map<String, Object> buildEcommerceSchema() {
        List<Map<String, Object>> tables = new ArrayList<>();
        List<Map<String, Object>> relationships = new ArrayList<>();

        // users
        tables.add(buildTable("users", List.of(
                buildCol("id", "uuid", true, false, false, false, null),
                buildCol("email", "varchar", false, false, false, true, null),
                buildCol("display_name", "varchar", false, false, false, false, null),
                buildCol("password_hash", "varchar", false, false, false, false, null),
                buildCol("created_at", "timestamptz", false, false, false, false, "NOW()")
        ), 0, 0));

        // categories
        tables.add(buildTable("categories", List.of(
                buildCol("id", "uuid", true, false, false, false, null),
                buildCol("name", "varchar", false, false, false, false, null),
                buildCol("parent_id", "uuid", true, false, false, false, null),
                buildCol("created_at", "timestamptz", false, false, false, false, "NOW()")
        ), 250, 0));

        // products
        tables.add(buildTable("products", List.of(
                buildCol("id", "uuid", true, false, false, false, null),
                buildCol("category_id", "uuid", true, false, false, false, null),
                buildCol("name", "varchar", false, false, false, false, null),
                buildCol("description", "text", true, false, false, false, null),
                buildCol("price", "numeric", false, false, false, false, null),
                buildCol("stock", "int", false, false, false, false, "0"),
                buildCol("created_at", "timestamptz", false, false, false, false, "NOW()")
        ), 500, 0));

        // orders
        tables.add(buildTable("orders", List.of(
                buildCol("id", "uuid", true, false, false, false, null),
                buildCol("user_id", "uuid", false, false, false, false, null),
                buildCol("status", "varchar", false, false, false, false, "pending"),
                buildCol("total_amount", "numeric", false, false, false, false, null),
                buildCol("created_at", "timestamptz", false, false, false, false, "NOW()")
        ), 0, 250));

        // order_items
        tables.add(buildTable("order_items", List.of(
                buildCol("id", "uuid", true, false, false, false, null),
                buildCol("order_id", "uuid", false, false, false, false, null),
                buildCol("product_id", "uuid", false, false, false, false, null),
                buildCol("quantity", "int", false, false, false, false, "1"),
                buildCol("unit_price", "numeric", false, false, false, false, null)
        ), 250, 250));

        // relationships
        relationships.add(buildRel("orders", "user_id", "users", "id", "1:N"));
        relationships.add(buildRel("order_items", "order_id", "orders", "id", "1:N"));
        relationships.add(buildRel("order_items", "product_id", "products", "id", "1:N"));
        relationships.add(buildRel("products", "category_id", "categories", "id", "1:N"));

        Map<String, Object> schema = new LinkedHashMap<>();
        schema.put("tables", tables);
        schema.put("relationships", relationships);
        return schema;
    }

    private static Map<String, Object> buildBlogSchema() {
        List<Map<String, Object>> tables = new ArrayList<>();
        List<Map<String, Object>> relationships = new ArrayList<>();

        // users
        tables.add(buildTable("users", List.of(
                buildCol("id", "uuid", true, false, false, false, null),
                buildCol("email", "varchar", false, false, false, true, null),
                buildCol("display_name", "varchar", false, false, false, false, null),
                buildCol("password_hash", "varchar", false, false, false, false, null),
                buildCol("created_at", "timestamptz", false, false, false, false, "NOW()")
        ), 0, 0));

        // posts
        tables.add(buildTable("posts", List.of(
                buildCol("id", "uuid", true, false, false, false, null),
                buildCol("author_id", "uuid", false, false, false, false, null),
                buildCol("title", "varchar", false, false, false, false, null),
                buildCol("content", "text", true, false, false, false, null),
                buildCol("published", "boolean", false, false, false, false, "false"),
                buildCol("created_at", "timestamptz", false, false, false, false, "NOW()")
        ), 250, 0));

        // comments
        tables.add(buildTable("comments", List.of(
                buildCol("id", "uuid", true, false, false, false, null),
                buildCol("post_id", "uuid", false, false, false, false, null),
                buildCol("author_id", "uuid", false, false, false, false, null),
                buildCol("content", "text", false, false, false, false, null),
                buildCol("created_at", "timestamptz", false, false, false, false, "NOW()")
        ), 0, 250));

        // tags
        tables.add(buildTable("tags", List.of(
                buildCol("id", "uuid", true, false, false, false, null),
                buildCol("name", "varchar", false, false, false, true, null),
                buildCol("created_at", "timestamptz", false, false, false, false, "NOW()")
        ), 500, 0));

        // post_tags (junction table)
        tables.add(buildTable("post_tags", List.of(
                buildCol("post_id", "uuid", false, false, false, false, null),
                buildCol("tag_id", "uuid", false, false, false, false, null)
        ), 500, 250));

        // relationships
        relationships.add(buildRel("posts", "author_id", "users", "id", "1:N"));
        relationships.add(buildRel("comments", "post_id", "posts", "id", "1:N"));
        relationships.add(buildRel("comments", "author_id", "users", "id", "1:N"));
        relationships.add(buildRel("post_tags", "post_id", "posts", "id", "1:N"));
        relationships.add(buildRel("post_tags", "tag_id", "tags", "id", "1:N"));

        Map<String, Object> schema = new LinkedHashMap<>();
        schema.put("tables", tables);
        schema.put("relationships", relationships);
        return schema;
    }

    private static Map<String, Object> buildTable(String name, List<Map<String, Object>> columns,
            int x, int y) {
        Map<String, Object> table = new LinkedHashMap<>();
        table.put("id", UUID.randomUUID().toString());
        table.put("name", name);
        table.put("position", Map.of("x", x, "y", y));
        table.put("columns", columns);
        table.put("indexes", List.of());
        return table;
    }

    private static Map<String, Object> buildCol(String name, String type, boolean nullable,
            boolean isPk, boolean isAutoIncrement, boolean isUnique, String defaultValue) {
        Map<String, Object> col = new LinkedHashMap<>();
        col.put("id", UUID.randomUUID().toString());
        col.put("name", name);
        col.put("type", type);
        col.put("nullable", nullable);
        col.put("isPrimaryKey", isPk);
        col.put("isAutoIncrement", isAutoIncrement);
        col.put("isUnique", isUnique);
        col.put("defaultValue", defaultValue);
        return col;
    }

    private static Map<String, Object> buildRel(String sourceTable, String sourceCol,
            String targetTable, String targetCol, String cardinality) {
        Map<String, Object> rel = new LinkedHashMap<>();
        rel.put("id", UUID.randomUUID().toString());
        rel.put("sourceTable", sourceTable);
        rel.put("sourceColumn", sourceCol);
        rel.put("targetTable", targetTable);
        rel.put("targetColumn", targetCol);
        rel.put("cardinality", cardinality);
        rel.put("onDelete", "NO_ACTION");
        rel.put("onUpdate", "NO_ACTION");
        return rel;
    }
}
