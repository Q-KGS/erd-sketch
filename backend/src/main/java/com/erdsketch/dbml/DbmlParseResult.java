package com.erdsketch.dbml;

import java.util.List;
import java.util.Map;

public record DbmlParseResult(
        List<Map<String, Object>> tables,
        List<Map<String, Object>> relationships,
        List<String> warnings
) {}
