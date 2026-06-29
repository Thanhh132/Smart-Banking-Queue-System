package com.sbqs.dto.bulkimport;

public record ImportError(
        int row,
        String identifier,
        String message) {
}
