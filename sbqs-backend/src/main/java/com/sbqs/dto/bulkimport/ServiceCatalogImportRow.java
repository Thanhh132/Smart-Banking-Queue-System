package com.sbqs.dto.bulkimport;

public record ServiceCatalogImportRow(
        int rowNumber,
        String serviceCode,
        String serviceName,
        String serviceType,
        String description,
        String estimatedTime,
        String delegatable) {
}
