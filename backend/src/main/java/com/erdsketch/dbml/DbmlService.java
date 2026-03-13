package com.erdsketch.dbml;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class DbmlService {

    private final DbmlParser parser;
    private final DbmlGenerator generator;

    public DbmlParseResult parse(String dbml) {
        return parser.parse(dbml);
    }

    public String generate(List<Map<String, Object>> tables, List<Map<String, Object>> relationships) {
        return generator.generate(tables, relationships);
    }
}
