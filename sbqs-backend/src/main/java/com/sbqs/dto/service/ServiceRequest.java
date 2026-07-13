package com.sbqs.dto.service;

import com.sbqs.entity.FormFieldDefinition;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.util.List;

public record ServiceRequest(
        @NotBlank String serviceCode,
        @NotBlank String serviceName,
        @NotBlank String serviceType,
        String description,
        @NotNull @Positive Integer estimatedTime,
        @NotBlank String status,
        List<String> requiredCustomerFields,
        List<FormFieldDefinition> formSchema,
        @NotNull @Valid BranchReference branch) {

    public record BranchReference(@NotNull Long branchId) {}
}
