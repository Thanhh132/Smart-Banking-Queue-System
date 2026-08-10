package com.sbqs.dto.service;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record ServiceCatalogRequest(
        String serviceCode,
        @NotBlank String serviceName,
        @NotBlank String serviceType,
        String description,
        @NotNull @Positive Integer estimatedTime,
        boolean delegatable) {
}
