package com.sbqs.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "sbqs.email-verification")
public class EmailVerificationProperties {
    private String frontendUrl = "http://localhost:4200/verify-email";
    private long expiryMinutes = 30;
    private long resendCooldownMinutes = 1;
    private String fromEmail = "no-reply@sbqs.local";
}
