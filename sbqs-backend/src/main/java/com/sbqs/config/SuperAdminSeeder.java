package com.sbqs.config;

import com.sbqs.entity.User;
import com.sbqs.repository.UserRepository;
import com.sbqs.service.KeycloakAdminService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.security.crypto.password.PasswordEncoder;

@Component
public class SuperAdminSeeder implements ApplicationRunner {
    private static final Logger log = LoggerFactory.getLogger(SuperAdminSeeder.class);

    private final SuperAdminSeedProperties properties;
    private final UserRepository userRepository;
    private final KeycloakAdminService keycloakService;
    private final PasswordEncoder passwordEncoder;

    public SuperAdminSeeder(
            SuperAdminSeedProperties properties,
            UserRepository userRepository,
            KeycloakAdminService keycloakService,
            PasswordEncoder passwordEncoder) {

        this.properties = properties;
        this.userRepository = userRepository;
        this.keycloakService = keycloakService;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (!properties.isEnabled()) {
            return;
        }

        if (isBlank(properties.getEmail()) || isBlank(properties.getPassword())) {
            throw new IllegalStateException(
                    "Super admin seed requires SBQS_SUPER_ADMIN_EMAIL and SBQS_SUPER_ADMIN_PASSWORD");
        }

        if (userRepository.existsByEmail(properties.getEmail())) {
            log.info("Super admin seed skipped because local user already exists");
            return;
        }

        String keycloakUserId = keycloakService.createUser(
                properties.getFullName(),
                properties.getEmail(),
                properties.getPassword(),
                "SUPER_ADMIN");

        User user = new User();
        user.setFullName(properties.getFullName());
        user.setEmail(properties.getEmail());
        user.setPhone(properties.getPhone());
        user.setRole("SUPER_ADMIN");
        user.setStatus("ACTIVE");
        user.setPasswordHash(passwordEncoder.encode(properties.getPassword()));
        user.setKeycloakUserId(keycloakUserId);
        userRepository.save(user);

        log.info("Super admin seed completed for email={}", properties.getEmail());
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
