package com.sbqs.dto.service;

import com.sbqs.entity.FormFieldDefinition;

import java.util.List;

public record ServiceResponse(
        Long serviceId,
        String serviceCode,
        String serviceName,
        String serviceType,
        String description,
        Integer estimatedTime,
        String status,
        List<String> requiredCustomerFields,
        List<FormFieldDefinition> formSchema,
        BranchReference branch) {

    public record BranchReference(Long branchId) {}
}
