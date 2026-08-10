package com.sbqs.service;

import com.sbqs.dto.LoginRequest;
import com.sbqs.dto.LoginResponse;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sbqs.entity.User;
import com.sbqs.repository.UserRepository;
import com.sbqs.config.FallbackAuthProperties;
import com.sbqs.config.KeycloakProperties;
import com.sbqs.dto.GoogleCodeExchangeRequest;
import com.sbqs.dto.GoogleLoginConfigResponse;
import com.sbqs.exception.KeycloakUnavailableException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Locale;

@Service
public class AuthService {

        private static final Logger log = LoggerFactory.getLogger(AuthService.class);
        private static final List<String> APP_ROLES =
                        List.of("SUPER_ADMIN", "BRANCH_ADMIN", "STAFF", "CUSTOMER");
        private static final Map<String, String> ROLE_ALIASES =
                        Map.of("ADMIN_BRANCH", "BRANCH_ADMIN");

        private final UserRepository userRepository;
        private final KeycloakService keycloakService;
        private final KeycloakAdminService keycloakAdminService;
        private final ObjectMapper objectMapper;
        private final PasswordEncoder passwordEncoder;
        private final FallbackTokenService fallbackTokenService;
        private final FallbackAuthProperties fallbackProperties;
        private final KeycloakProperties keycloakProperties;

        public AuthService(
                        UserRepository userRepository,
                        KeycloakService keycloakService,
                        KeycloakAdminService keycloakAdminService,
                        ObjectMapper objectMapper,
                        PasswordEncoder passwordEncoder,
                        FallbackTokenService fallbackTokenService,
                        FallbackAuthProperties fallbackProperties,
                        KeycloakProperties keycloakProperties) {

                this.userRepository = userRepository;
                this.keycloakService = keycloakService;
                this.keycloakAdminService = keycloakAdminService;
                this.objectMapper = objectMapper;
                this.passwordEncoder = passwordEncoder;
                this.fallbackTokenService = fallbackTokenService;
                this.fallbackProperties = fallbackProperties;
                this.keycloakProperties = keycloakProperties;
        }

        /**
         * Luồng đăng nhập chính: luôn thử Keycloak trước; chỉ chuyển sang database fallback
         * khi Keycloak thật sự mất kết nối/timeout, không fallback khi người dùng nhập sai mật khẩu.
         */
        public LoginResponse login(
                        LoginRequest request) {

                String email = normalizeEmail(request.getEmail());
                log.info("Requesting Keycloak token for email={}", email);

                userRepository.findByEmailIgnoreCase(email)
                                .filter(user -> "PENDING".equalsIgnoreCase(user.getStatus()))
                                .ifPresent(user -> {
                                        throw new RuntimeException("Vui long xac minh email truoc khi dang nhap");
                                });

                Map<String, Object> token;

                try {
                        token = keycloakService.login(
                                        email,
                                        request.getPassword());
                } catch (KeycloakUnavailableException ex) {
                        return fallbackLogin(email, request.getPassword(), ex);
                } catch (RuntimeException ex) {
                        Optional<User> existingUser =
                                        userRepository.findByEmailIgnoreCase(email);

                        if (isAccountNotFullySetUp(ex)
                                        && existingUser.isPresent()
                                        && existingUser.get().getKeycloakUserId() != null) {
                                User user = existingUser.get();
                                log.info("Clearing Keycloak required actions for email={}", email);
                                keycloakAdminService.repairUserPasswordLogin(
                                                user.getKeycloakUserId(),
                                                user.getFullName(),
                                                user.getEmail(),
                                                request.getPassword(),
                                                user.getRole());
                                token = keycloakService.login(
                                                email,
                                                request.getPassword());
                        } else if (canUseLocalFallback(existingUser, request.getPassword())) {
                                return fallbackLogin(email, request.getPassword(), ex);
                        } else {
                                throw ex;
                        }
                }

                LoginResponse response = buildLoginResponse(token, email, false);
                userRepository.findByEmailIgnoreCase(email).ifPresent(user -> {
                        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
                        userRepository.save(user);
                });
                return response;
        }

        /** Xác thực BCrypt nội bộ và cấp JWT ngắn hạn khi Keycloak không khả dụng. */
        private LoginResponse fallbackLogin(String email, String password, KeycloakUnavailableException cause) {
                return fallbackLogin(email, password, (RuntimeException) cause);
        }

        private LoginResponse fallbackLogin(String email, String password, RuntimeException cause) {
                if (!fallbackProperties.isEnabled()) {
                        throw cause;
                }

                User user = userRepository.findByEmailIgnoreCase(email)
                                .orElseThrow(() -> new RuntimeException("Email hoac mat khau khong dung"));

                if (!"ACTIVE".equalsIgnoreCase(user.getStatus())
                                || user.getPasswordHash() == null
                                || "KEYCLOAK_MANAGED".equals(user.getPasswordHash())
                                || !passwordEncoder.matches(password, user.getPasswordHash())) {
                        log.warn("Fallback authentication rejected for email={}", email);
                        throw new RuntimeException("Email hoac mat khau khong dung");
                }

                if (!fallbackProperties.getAllowedRoles().contains(user.getRole())) {
                        log.warn("Fallback authentication denied by role policy email={} role={}", user.getEmail(), user.getRole());
                        throw new RuntimeException("Dich vu dang nhap dang gian doan. Tai khoan nay khong duoc phep dang nhap du phong");
                }

                log.warn("KEYCLOAK_UNAVAILABLE: issuing short-lived fallback token email={} role={} expiresIn={}s",
                                user.getEmail(), user.getRole(), fallbackTokenService.expiresInSeconds());
                return new LoginResponse(
                                fallbackTokenService.issue(user),
                                null,
                                "Bearer",
                                fallbackTokenService.expiresInSeconds(),
                                user.getRole(),
                                user.getFullName(),
                                user.getEmail(),
                                user.getBranch() == null ? null : user.getBranch().getBranchId(),
                                "FALLBACK",
                                !"CUSTOMER".equals(user.getRole()) || CustomerProfilePolicy.isComplete(user));
        }

        private boolean canUseLocalFallback(Optional<User> user, String password) {
                return user
                                .filter(existingUser -> "ACTIVE".equalsIgnoreCase(existingUser.getStatus()))
                                .filter(existingUser -> existingUser.getPasswordHash() != null)
                                .filter(existingUser -> !"KEYCLOAK_MANAGED".equals(existingUser.getPasswordHash()))
                                .filter(existingUser -> passwordEncoder.matches(password, existingUser.getPasswordHash()))
                                .isPresent();
        }

        /** Đổi refresh token Keycloak lấy bộ token mới; JWT fallback không có refresh token. */
        public LoginResponse refresh(String refreshToken) {
                Map<String, Object> token = keycloakService.refreshToken(refreshToken);
                return buildLoginResponse(token, null, false);
        }

        public GoogleLoginConfigResponse getGoogleLoginConfig() {
                String publicUrl = firstNotBlank(keycloakProperties.getPublicUrl(), keycloakProperties.getServerUrl());
                return new GoogleLoginConfigResponse(
                                keycloakProperties.isGoogleLoginEnabled(),
                                publicUrl + "/realms/" + keycloakProperties.getRealm()
                                                + "/protocol/openid-connect/auth",
                                keycloakProperties.getClientId(),
                                keycloakProperties.getGoogleRedirectUri());
        }

        public LoginResponse exchangeGoogleCode(GoogleCodeExchangeRequest request) {
                Map<String, Object> token = keycloakService.exchangeAuthorizationCode(
                                request.code(), request.codeVerifier(), keycloakProperties.getGoogleRedirectUri());
                Map<String, Object> payload = decodeTokenPayload(valueAsString(token.get("access_token")));
                String email = normalizeEmail(valueAsString(payload.get("email")));
                String keycloakUserId = valueAsString(payload.get("sub"));
                if (email == null || email.isBlank() || keycloakUserId == null || keycloakUserId.isBlank()) {
                        throw new RuntimeException("Google khong cung cap du thong tin dinh danh");
                }

                boolean googleClaim = "google".equalsIgnoreCase(
                                valueAsString(payload.get("identity_provider")));
                if (!googleClaim && !keycloakAdminService.hasFederatedIdentity(keycloakUserId, "google")) {
                        throw new RuntimeException("Phien dang nhap khong den tu Google");
                }

                userRepository.findByEmailIgnoreCase(email).ifPresent(user -> {
                        if (!"CUSTOMER".equals(user.getRole())) {
                                throw new RuntimeException("Dang nhap Google chi danh cho tai khoan khach hang");
                        }
                });

                if (!"CUSTOMER".equals(resolveRoleFromPayload(payload, null))) {
                        keycloakAdminService.assignRealmRole(keycloakUserId, "CUSTOMER");
                        token = keycloakService.refreshToken(valueAsString(token.get("refresh_token")));
                }

                return buildLoginResponse(token, email, true);
        }

        public void logout(String refreshToken) {
                keycloakService.logout(refreshToken);
        }

        /** Chuẩn hóa token Keycloak thành response chung mà Angular sử dụng cho mọi role. */
        private LoginResponse buildLoginResponse(
                        Map<String, Object> token,
                        String fallbackEmail,
                        boolean googleLogin) {

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

                User user = userRepository.findByEmailIgnoreCase(tokenEmail)
                                .orElseGet(() -> createLocalUserFromToken(
                                                tokenPayload,
                                                tokenEmail,
                                                keycloakUserId,
                                                resolvedRole,
                                                googleLogin));

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

                if (googleLogin && !"GOOGLE".equals(user.getIdentityProvider())) {
                        user.setIdentityProvider("GOOGLE");
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
                                user.getBranch() == null ? null : user.getBranch().getBranchId(),
                                "KEYCLOAK",
                                !"CUSTOMER".equals(resolvedRole) || CustomerProfilePolicy.isComplete(user));
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
                        String role,
                        boolean googleLogin) {

                User user = new User();
                user.setEmail(email);
                user.setFullName(googleLogin ? "" : resolveFullName(tokenPayload, email));
                user.setRole(role);
                user.setStatus("ACTIVE");
                user.setPasswordHash("KEYCLOAK_MANAGED");
                user.setKeycloakUserId(keycloakUserId);
                user.setIdentityProvider(googleLogin ? "GOOGLE" : "KEYCLOAK");

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

        private String normalizeEmail(String email) {
                return email == null ? null : email.trim().toLowerCase(Locale.ROOT);
        }

        /** Chỉ đọc claim để dựng response; việc xác minh chữ ký JWT được Spring Security thực hiện ở request sau. */
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
