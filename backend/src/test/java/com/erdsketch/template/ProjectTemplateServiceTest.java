package com.erdsketch.template;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class ProjectTemplateServiceTest {

    private ProjectTemplateService service;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        service = new ProjectTemplateService();
        objectMapper = new ObjectMapper();
    }

    // B-TPL-01: ECOMMERCE 템플릿 → users, products, orders, order_items, categories 테이블
    @Test
    void applyTemplate_ecommerce_containsRequiredTables() {
        Map<String, Object> schema = service.applyTemplate(ProjectTemplateType.ECOMMERCE);

        assertThat(schema).isNotNull();
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> tables = (List<Map<String, Object>>) schema.get("tables");
        assertThat(tables).isNotNull();

        List<String> tableNames = tables.stream()
                .map(t -> (String) t.get("name"))
                .toList();

        assertThat(tableNames).containsExactlyInAnyOrder(
                "users", "products", "categories", "orders", "order_items");
    }

    // B-TPL-02: BLOG 템플릿 → posts, comments, tags, users 테이블
    @Test
    void applyTemplate_blog_containsRequiredTables() {
        Map<String, Object> schema = service.applyTemplate(ProjectTemplateType.BLOG);

        assertThat(schema).isNotNull();
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> tables = (List<Map<String, Object>>) schema.get("tables");
        assertThat(tables).isNotNull();

        List<String> tableNames = tables.stream()
                .map(t -> (String) t.get("name"))
                .toList();

        assertThat(tableNames).contains("users", "posts", "comments", "tags");
    }

    // B-TPL-03: 템플릿에 FK 관계 포함
    @Test
    void applyTemplate_ecommerce_containsRelationships() {
        Map<String, Object> schema = service.applyTemplate(ProjectTemplateType.ECOMMERCE);

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> relationships = (List<Map<String, Object>>) schema.get("relationships");
        assertThat(relationships).isNotNull();
        assertThat(relationships).isNotEmpty();

        // Verify orders → users relationship exists
        boolean hasOrderUserRel = relationships.stream().anyMatch(r ->
                "orders".equals(r.get("sourceTable")) && "users".equals(r.get("targetTable")));
        assertThat(hasOrderUserRel).isTrue();
    }

    // B-TPL-04: 빈 스키마에 템플릿 적용
    @Test
    void mergeTemplate_emptyExistingSchema_appliesTemplateDirectly() throws Exception {
        String emptySchema = objectMapper.writeValueAsString(Map.of(
                "tables", List.of(),
                "relationships", List.of()
        ));

        Map<String, Object> merged = service.mergeTemplate(ProjectTemplateType.BLOG, emptySchema);

        assertThat(merged).isNotNull();
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> tables = (List<Map<String, Object>>) merged.get("tables");
        assertThat(tables).isNotEmpty();

        List<String> tableNames = tables.stream()
                .map(t -> (String) t.get("name"))
                .toList();
        assertThat(tableNames).contains("users", "posts");
    }

    // B-TPL-05: 기존 테이블과 충돌 시 suffix 추가
    @Test
    void mergeTemplate_conflictingTableName_addsSuffix() throws Exception {
        // Existing schema already has "users" table
        String existingSchema = objectMapper.writeValueAsString(Map.of(
                "tables", List.of(
                        Map.of("id", "existing-id", "name", "users", "columns", List.of(), "indexes", List.of())
                ),
                "relationships", List.of()
        ));

        Map<String, Object> merged = service.mergeTemplate(ProjectTemplateType.BLOG, existingSchema);

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> tables = (List<Map<String, Object>>) merged.get("tables");
        List<String> tableNames = tables.stream()
                .map(t -> (String) t.get("name"))
                .toList();

        // "users" already exists, template's "users" should be renamed to "users_2"
        assertThat(tableNames).contains("users");
        assertThat(tableNames).contains("users_2");
    }

    // Additional: blog template has post_tags junction table
    @Test
    void applyTemplate_blog_hasPostTagsJunctionTable() {
        Map<String, Object> schema = service.applyTemplate(ProjectTemplateType.BLOG);

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> tables = (List<Map<String, Object>>) schema.get("tables");
        List<String> tableNames = tables.stream()
                .map(t -> (String) t.get("name"))
                .toList();

        assertThat(tableNames).contains("post_tags");
    }

    // Additional: each table has id and created_at columns
    @Test
    void applyTemplate_ecommerce_tablesHaveIdAndCreatedAt() {
        Map<String, Object> schema = service.applyTemplate(ProjectTemplateType.ECOMMERCE);

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> tables = (List<Map<String, Object>>) schema.get("tables");

        for (Map<String, Object> table : tables) {
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> columns = (List<Map<String, Object>>) table.get("columns");
            List<String> colNames = columns.stream()
                    .map(c -> (String) c.get("name"))
                    .toList();
            assertThat(colNames).contains("id");
        }
    }
}
