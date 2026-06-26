package com.sbqs.service;

import com.sbqs.dto.LoginRequest;
import com.sbqs.dto.LoginResponse;
import com.sbqs.dto.RegisterRequest;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sbqs.entity.User;
import com.sbqs.repository.UserRepository;
import com.sbqs.util.PasswordPolicy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class AuthService {

        private static final Logger log = LoggerFactory.getLogger(AuthService.class);
        private static final List<String> APP_ROLES =
                        List.of("SUPER_ADMIN", "BRANCH_ADMIN", "STAFF", "CUSTOMER");
        private static final Map<String, String> ROLE_ALIASES =
                        Map.of("ADMIN_BRANCH", "BRANCH_ADMIN");

        private final UserRepository userRepository;
        private final KeycloakService keycloakService;
        private final ObjectMapper objectMapper;
        private final PasswordResetService passwordResetService;

        public AuthService(
                        UserRepository userRepository,
                        KeycloakService keycloakService,
                        ObjectMapper objectMapper,
                        PasswordResetService passwordResetService) {

                this.userRepository = userRepository;
                this.keycloakService = keycloakService;
                this.objectMapper = objectMapper;
                this.passwordResetService = passwordResetService;
        }

        public User register(
                        RegisterRequest request) {

                if (userRepository.existsByEmail(request.getEmail())) {
                        throw new RuntimeException("Email da ton tai");
                }

                if (userRepository.existsByPhone(request.getPhone())) {
                        throw new RuntimeException("So dien thoai da ton tai");
                }

                PasswordPolicy.validate(request.getPassword());

                String keycloakUserId = keycloakService.createUser(
                                request.getFullName(),
                                request.getEmail(),
                                request.getPassword(),
                                "CUSTOMER");

                User user = new User();

                user.setFullName(request.getFullName());
                user.setEmail(request.getEmail());
                user.setPhone(request.getPhone());
                user.setRole("CUSTOMER");
                user.setPasswordHash("KEYCLOAK_MANAGED");
                user.setKeycloakUserId(keycloakUserId);

                return userRepository.save(user);
        }

        public LoginResponse login(
                        LoginRequest request) {

                log.info("Requesting Keycloak token for email={}", request.getEmail());

                Map<String, Object> token;

                try {
                        token = keycloakService.login(
                                        request.getEmail(),
                                        request.getPassword());
                } catch (RuntimeException ex) {
                        Optional<User> existingUser =
                                        userRepository.findByEmail(request.getEmail());

                        if (isAccountNotFullySetUp(ex)
                                        && existingUser.isPresent()
                                        && existingUser.get().getKeycloakUserId() != null) {
                                User user = existingUser.get();
                                log.info("Clearing Keycloak required actions for email={}", request.getEmail());
                                keycloakService.repairUserPasswordLogin(
                                                user.getKeycloakUserId(),
                                                user.getFullName(),
                                                user.getEmail(),
                                                request.getPassword(),
                                                user.getRole());
                                token = keycloakService.login(
                                                request.getEmail(),
                                                request.getPassword());
                        } else {
                                throw ex;
                        }
                }

                return buildLoginResponse(token, request.getEmail());
        }

        public LoginResponse refresh(String refreshToken) {
                Map<String, Object> token = keycloakService.refreshToken(refreshToken);
                return buildLoginResponse(token, null);
        }

        private LoginResponse buildLoginResponse(
                        Map<String, Object> token,
                        String fallbackEmail) {

                String accessToken = valueAsString(token.get("access_token"));
                Map<String, Object> tokenPayload = decodeTokenPayload(accessToken);
                String tokenEmail = firstNotBlank(
                                valueAsString(tokenPayload.get("email")),
                                fallbackEmail);
                String keycloakUserId = valueAsString(tokenPayload.get("sub"));
                String resolvedRole = resolveRoleFromPayload(tokenPayload, null);

                if (resolvedRole == null) {
                        throw new RuntimeException("Tai khoan Keycloak chua duoc gan role SBQS");
                }

                User user = userRepository.findByEmail(tokenEmail)
                                .orElseGet(() -> createLocalUserFromToken(
                                                tokenPayload,
                                                tokenEmail,
                                                keycloakUserId,
                                                resolvedRole));

                if (!"ACTIVE".equalsIgnoreCase(user.getStatus())) {
                        throw new RuntimeException("Tai khoan da bi khoa");
                }

                boolean changed = false;

                if (!resolvedRole.equals(user.getRole())) {
                        log.info(
                                        "Syncing app user role from DB role={} to Keycloak role={} for email={}",
                                        user.getRole(),
                                        resolvedRole,
                                        user.getEmail());
                        user.setRole(resolvedRole);
                        changed = true;
                }

                if (keycloakUserId != null
                                && !keycloakUserId.equals(user.getKeycloakUserId())) {
                        user.setKeycloakUserId(keycloakUserId);
                        changed = true;
                }

                if (changed) {
                        user = userRepository.save(user);
                }

                log.info(
                                "Login response profile email={} role={} branchId={}",
                                user.getEmail(),
                                resolvedRole,
                                user.getBranch() == null ? null : user.getBranch().getBranchId());

                return new LoginResponse(
                                accessToken,
                                valueAsString(token.get("refresh_token")),
                                valueAsString(token.get("token_type")),
                                valueAsInteger(token.get("expires_in")),
                                resolvedRole,
                                user.getFullName(),
                                user.getEmail(),
                                user.getBranch() == null ? null : user.getBranch().getBranchId());
        }

        public void requestPasswordReset(String email) {
                passwordResetService.requestReset(email);
        }

        public void resetPassword(String token, String newPassword) {
                passwordResetService.resetPassword(token, newPassword);
        }

        private String valueAsString(Object value) {
                return value == null ? null : value.toString();
        }

        private Integer valueAsInteger(Object value) {
                if (value instanceof Number number) {
                        return number.intValue();
                }

                if (value == null) {
                        return null;
                }

                return Integer.valueOf(value.toString());
        }

        private boolean isAccountNotFullySetUp(RuntimeException ex) {
                return ex.getMessage() != null
                                && ex.getMessage().contains("Account is not fully set up");
        }

        private User createLocalUserFromToken(
                        Map<String, Object> tokenPayload,
                        String email,
                        String keycloakUserId,
                        String role) {

                User user = new User();
                user.setEmail(email);
                user.setFullName(resolveFullName(tokenPayload, email));
                user.setRole(role);
                user.setStatus("ACTIVE");
                user.setPasswordHash("KEYCLOAK_MANAGED");
                user.setKeycloakUserId(keycloakUserId);

                log.info("Creating local app profile from Keycloak token email={} role={}", email, role);

                return userRepository.save(user);
        }

        private String resolveFullName(Map<String, Object> tokenPayload, String email) {
                String name = valueAsString(tokenPayload.get("name"));

                if (name != null && !name.isBlank()) {
                        return name;
                }

                String givenName = valueAsString(tokenPayload.get("given_name"));
                String familyName = valueAsString(tokenPayload.get("family_name"));
                String combinedName = firstNotBlank(givenName, "") + " " + firstNotBlank(familyName, "");

                if (!combinedName.isBlank()) {
                        return combinedName.trim();
                }

                return email;
        }

        private String firstNotBlank(String first, String second) {
                if (first != null && !first.isBlank()) {
                        return first;
                }

                return second;
        }

        private Map<String, Object> decodeTokenPayload(String accessToken) {
                if (accessToken == null || accessToken.isBlank()) {
                        throw new RuntimeException("Keycloak access token khong hop le");
                }

                try {
                        String[] parts = accessToken.split("\\.");
                        if (parts.length < 2) {
                                throw new RuntimeException("Keycloak access token khong hop le");
                        }

                        String payloadJson = new String(
                                        Base64.getUrlDecoder().decode(parts[1]),
                                        StandardCharsets.UTF_8);

                        return objectMapper.readValue(
                                        payloadJson,
                                        new TypeReference<Map<String, Object>>() {
                                        });
                } catch (RuntimeException ex) {
                        throw ex;
                } catch (Exception ex) {
                        throw new RuntimeException("Khong doc duoc Keycloak access token", ex);
                }
        }

        private String resolveRoleFromPayload(
                        Map<String, Object> payload,
                        String fallbackRole) {
                try {
                        Object realmAccessObject = payload.get("realm_access");
                        if (!(realmAccessObject instanceof Map<?, ?> realmAccess)) {
                                return fallbackRole;
                        }

                        Object rolesObject = realmAccess.get("roles");
                        if (!(rolesObject instanceof List<?> roles)) {
                                return fallbackRole;
                        }

                        for (String appRole : APP_ROLES) {
                                if (roles.contains(appRole)) {
                                        return appRole;
                                }
                        }

                        for (Object role : roles) {
                                String normalizedRole = ROLE_ALIASES.get(valueAsString(role));
                                if (normalizedRole != null) {
                                        return normalizedRole;
                                }
                        }

                        return fallbackRole;
                } catch (Exception ex) {
                        log.warn("Cannot resolve role from Keycloak token", ex);
                        return fallbackRole;
                }
        }
}
