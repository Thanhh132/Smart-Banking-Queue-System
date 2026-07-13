package com.sbqs.dto.bulkimport;

import java.util.List;

public final class ImportResult {
    private final int totalRows;
    private final int successCount;
    private final int failureCount;
    private final List<ImportError> errors;

    public ImportResult(int totalRows, int successCount, int failureCount, List<ImportError> errors) {
        this.totalRows = totalRows;
        this.successCount = successCount;
        this.failureCount = failureCount;
        this.errors = errors == null ? List.of() : List.copyOf(errors);
    }

    public int getTotalRows() { return totalRows; }
    public int getSuccessCount() { return successCount; }
    public int getFailureCount() { return failureCount; }
    public List<ImportError> getErrors() { return errors; }
}
