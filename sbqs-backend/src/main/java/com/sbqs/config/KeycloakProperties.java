package com.sbqs.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "keycloak")
public class KeycloakProperties {

    private String serverUrl;
    private String publicUrl;
    private String realm;
    private String clientId;
    private String clientSecret;
    private String adminClientId = "admin-cli";
    private String adminUsername;
    private String adminPassword;
    private boolean googleLoginEnabled = true;
    private String googleRedirectUri = "http://localhost:4200/auth/google/callback";
}
