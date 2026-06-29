package com.sbqs.dto.bulkimport;

import java.util.List;

public record ImportResult(
        int totalRows,
        int successCount,
        int failureCount,
        List<ImportError> errors) {
}
