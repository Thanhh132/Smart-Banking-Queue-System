package com.sbqs.dto;

import jakarta.validation.constraints.NotNull;

import java.util.Map;

public record UpdateCustomerPaperlessProfileRequest(
        Long serviceId,
        @NotNull Map<String, String> values) {
}
