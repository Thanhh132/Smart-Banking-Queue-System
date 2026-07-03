package com.sbqs.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.security.SecureRandom;
import java.util.Base64;
import java.util.List;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "sbqs.auth.fallback")
public class FallbackAuthProperties {
    private static final Logger log = LoggerFactory.getLogger(FallbackAuthProperties.class);
    private boolean enabled = true;
    private String issuer = "sbqs-fallback";
    private String secret;
    private long accessTokenMinutes = 5;
    private List<String> allowedRoles = List.of("CUSTOMER", "STAFF", "BRANCH_ADMIN", "SUPER_ADMIN");

    @PostConstruct
    void initializeSecret() {
        if (secret == null || secret.isBlank()) {
            byte[] randomSecret = new byte[32];
            new SecureRandom().nextBytes(randomSecret);
            secret = Base64.getEncoder().encodeToString(randomSecret);
            log.warn("SBQS_FALLBACK_JWT_SECRET is not configured; using an ephemeral key. "
                    + "Fallback sessions will be invalid after restart.");
        }
    }
}
