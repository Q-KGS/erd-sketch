package com.erdsketch.document;

import java.util.List;
import java.util.Map;

public record DdlParseResponse(List<Map<String, Object>> tables, List<Map<String, Object>> relationships, List<String> warnings) {}
