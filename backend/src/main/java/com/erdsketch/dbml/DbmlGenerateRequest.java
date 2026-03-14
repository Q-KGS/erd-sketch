package com.erdsketch.dbml;

import jakarta.validation.constraints.NotNull;

import java.util.List;
import java.util.Map;

public record DbmlGenerateRequest(
        @NotNull List<Map<String, Object>> tables,
        @NotNull List<Map<String, Object>> relationships
) {}
