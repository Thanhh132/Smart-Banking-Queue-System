package com.sbqs.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record GoogleCodeExchangeRequest(
        @NotBlank @Size(max = 4096) String code,
        @NotBlank @Size(min = 43, max = 128)
        @Pattern(regexp = "^[A-Za-z0-9._~-]+$", message = "PKCE verifier khong hop le")
        String codeVerifier) {
}
