package com.sbqs.dto.delegation;

import jakarta.validation.constraints.*;
import java.time.LocalDateTime;
import java.time.LocalDate;

public record CreateDelegationRequest(
        @NotNull Long branchId,
        @NotNull Long serviceId,
        @NotBlank @Size(max = 150) String delegateName,
        @NotBlank @Pattern(regexp = "^\\d{12}$", message = "CCCD phải gồm đúng 12 chữ số") String delegateIdentityNumber,
        @NotNull @Past LocalDate delegateDateOfBirth,
        @NotBlank @Pattern(regexp = "^(0|\\+84)[0-9]{9}$") String delegatePhone,
        @NotNull @PastOrPresent LocalDate identityIssueDate,
        @NotNull @FutureOrPresent LocalDate identityExpiryDate,
        @NotBlank @Size(max = 150) String identityIssuePlace,
        @NotBlank @Size(max = 100) String relationship,
        @NotBlank @Size(min = 10, max = 500) String transactionScope,
        @NotNull @Future LocalDateTime validUntil,
        @AssertTrue boolean acceptedTerms) { }
