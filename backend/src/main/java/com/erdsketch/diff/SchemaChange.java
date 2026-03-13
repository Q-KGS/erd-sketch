package com.erdsketch.diff;

public record SchemaChange(
        SchemaChangeType type,
        String tableName,
        String columnName,
        String fromValue,
        String toValue,
        String description
) {
}
