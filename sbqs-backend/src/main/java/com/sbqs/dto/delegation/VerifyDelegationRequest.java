package com.sbqs.dto.delegation;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record VerifyDelegationRequest(
        @NotBlank String referenceCode,
        @NotBlank @Pattern(regexp = "^\\d{12}$", message = "CCCD phải gồm đúng 12 chữ số") String delegateIdentityNumber) { }
