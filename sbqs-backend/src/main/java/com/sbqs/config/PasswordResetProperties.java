package com.sbqs.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "sbqs.password-reset")
public class PasswordResetProperties {
    private String frontendUrl = "http://localhost:4200/reset-password";
    private int expiryMinutes = 15;
    private String fromEmail = "no-reply@sbqs.local";
}
