package com.sbqs.dto.report;

public enum ReportFormat {
    PDF("application/pdf", "pdf"),
    XLSX("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", "xlsx");

    private final String contentType;
    private final String extension;

    ReportFormat(String contentType, String extension) {
        this.contentType = contentType;
        this.extension = extension;
    }

    public String getContentType() {
        return contentType;
    }

    public String getExtension() {
        return extension;
    }

    public static ReportFormat from(String value) {
        try {
            return ReportFormat.valueOf(value == null ? "PDF" : value.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw new RuntimeException("Dinh dang report chi ho tro PDF hoac XLSX");
        }
    }
}
