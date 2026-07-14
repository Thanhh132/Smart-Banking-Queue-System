package com.sbqs.dto.delegation;

import java.time.LocalDateTime;

public record DelegationResponse(
        Long delegationId, String referenceCode, String delegateName, String maskedIdentity,
        java.time.LocalDate delegateDateOfBirth, String delegatePhone,
        java.time.LocalDate identityIssueDate, java.time.LocalDate identityExpiryDate, String identityIssuePlace,
        String relationship, String transactionScope, String status, String ownerName, String maskedOwnerEmail,
        Long branchId, String branchName, Long serviceId, String serviceName,
        LocalDateTime validFrom, LocalDateTime validUntil, LocalDateTime createdAt,
        LocalDateTime verifiedAt, LocalDateTime usedAt) { }
