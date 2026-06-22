package com.sbqs.dto.report;

public record ReportDocument(
        byte[] content,
        String contentType,
        String fileName) {
}
