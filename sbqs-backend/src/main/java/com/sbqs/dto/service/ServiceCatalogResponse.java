package com.sbqs.dto.service;

public record ServiceCatalogResponse(
        Long catalogId,
        String serviceCode,
        String serviceName,
        String serviceType,
        String description,
        Integer estimatedTime,
        String status,
        boolean delegatable) {
}
