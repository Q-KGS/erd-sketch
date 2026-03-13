package com.erdsketch.template;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class ProjectTemplateService {

    private final ObjectMapper objectMapper = new ObjectMapper();

    public Map<String, Object> applyTemplate(ProjectTemplateType type) {
        return ProjectTemplate.getSchema(type);
    }

    public Map<String, Object> mergeTemplate(ProjectTemplateType type, String existingSchemaJson) {
        Map<String, Object> template = ProjectTemplate.getSchema(type);

        Map<String, Object> existing;
        try {
            existing = objectMapper.readValue(existingSchemaJson,
                    new TypeReference<Map<String, Object>>() {});
        } catch (Exception e) {
            return template;
        }

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> existingTables = (List<Map<String, Object>>) existing.getOrDefault("tables", new ArrayList<>());
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> existingRels = (List<Map<String, Object>>) existing.getOrDefault("relationships", new ArrayList<>());

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> templateTables = (List<Map<String, Object>>) template.getOrDefault("tables", new ArrayList<>());
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> templateRels = (List<Map<String, Object>>) template.getOrDefault("relationships", new ArrayList<>());

        // Collect existing table names
        Set<String> existingNames = new HashSet<>();
        for (Map<String, Object> t : existingTables) {
            existingNames.add((String) t.get("name"));
        }

        // Merge: rename conflicting template tables
        List<Map<String, Object>> mergedTables = new ArrayList<>(existingTables);
        Map<String, String> renamedMap = new LinkedHashMap<>(); // original -> new name

        for (Map<String, Object> tplTable : templateTables) {
            String originalName = (String) tplTable.get("name");
            String newName = originalName;
            int suffix = 2;
            while (existingNames.contains(newName)) {
                newName = originalName + "_" + suffix;
                suffix++;
            }
            existingNames.add(newName);

            Map<String, Object> copy = new LinkedHashMap<>(tplTable);
            if (!newName.equals(originalName)) {
                copy.put("name", newName);
                renamedMap.put(originalName, newName);
            }
            mergedTables.add(copy);
        }

        // Merge relationships, updating table names if renamed
        List<Map<String, Object>> mergedRels = new ArrayList<>(existingRels);
        for (Map<String, Object> rel : templateRels) {
            Map<String, Object> relCopy = new LinkedHashMap<>(rel);
            String srcTable = (String) relCopy.get("sourceTable");
            String tgtTable = (String) relCopy.get("targetTable");
            if (renamedMap.containsKey(srcTable)) relCopy.put("sourceTable", renamedMap.get(srcTable));
            if (renamedMap.containsKey(tgtTable)) relCopy.put("targetTable", renamedMap.get(tgtTable));
            mergedRels.add(relCopy);
        }

        Map<String, Object> merged = new LinkedHashMap<>();
        merged.put("tables", mergedTables);
        merged.put("relationships", mergedRels);
        return merged;
    }
}
