package com.erdsketch.template;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/templates")
public class ProjectTemplateController {

    private final ProjectTemplateService service;

    @GetMapping
    public ResponseEntity<List<ProjectTemplateInfo>> list() {
        List<ProjectTemplateInfo> templates = Arrays.asList(
                new ProjectTemplateInfo("ECOMMERCE", "이커머스", "users, categories, products, orders, order_items 5개 테이블"),
                new ProjectTemplateInfo("BLOG", "블로그", "users, posts, comments, tags, post_tags 5개 테이블")
        );
        return ResponseEntity.ok(templates);
    }

    @PostMapping("/{type}/apply")
    public ResponseEntity<Map<String, Object>> apply(@PathVariable String type) {
        ProjectTemplateType templateType = ProjectTemplateType.valueOf(type.toUpperCase());
        return ResponseEntity.ok(service.applyTemplate(templateType));
    }

    @PostMapping("/{type}/merge")
    public ResponseEntity<Map<String, Object>> merge(
            @PathVariable String type,
            @Valid @RequestBody TemplateMergeRequest request) {
        ProjectTemplateType templateType = ProjectTemplateType.valueOf(type.toUpperCase());
        return ResponseEntity.ok(service.mergeTemplate(templateType, request.existingSchemaJson()));
    }
}
