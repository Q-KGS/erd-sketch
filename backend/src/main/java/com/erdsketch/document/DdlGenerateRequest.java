package com.erdsketch.document;

import com.erdsketch.project.DbType;
import java.util.List;
import java.util.Map;

public record DdlGenerateRequest(DbType dialect, List<String> tableIds, boolean includeDrops, Map<String, Object> schema) {}
