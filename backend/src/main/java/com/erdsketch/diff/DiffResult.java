package com.erdsketch.diff;

import java.util.List;

public record DiffResult(List<SchemaChange> changes, boolean hasChanges) {

    public static DiffResult empty() {
        return new DiffResult(List.of(), false);
    }

    public static DiffResult of(List<SchemaChange> changes) {
        return new DiffResult(changes, !changes.isEmpty());
    }
}
