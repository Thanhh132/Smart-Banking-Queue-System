package com.sbqs.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "sbqs.seed.super-admin")
public class SuperAdminSeedProperties {
    private boolean enabled;
    private String fullName = "SBQS Super Admin";
    private String email;
    private String password;
    private String phone;
}
