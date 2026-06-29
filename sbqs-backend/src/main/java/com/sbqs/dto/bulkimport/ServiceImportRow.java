package com.sbqs.dto.bulkimport;

public record ServiceImportRow(
        int rowNumber,
        String serviceCode,
        String serviceName,
        String serviceType,
        String description,
        String estimatedTime,
        String status) {
}
