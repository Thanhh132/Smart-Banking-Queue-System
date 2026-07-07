package com.sbqs.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "sbqs.account-change")
public class AccountChangeProperties {
    private String frontendUrl = "http://localhost:4200/confirm-account-change";
    private String fromEmail = "no-reply@sbqs.local";
    private long expiryMinutes = 15;
}
