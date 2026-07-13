package com.sbqs.service;

import com.sbqs.config.KeycloakProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Bao bọc Keycloak Admin REST API để quản lý danh tính, mật khẩu và vai trò người dùng.
 * Admin token chỉ tồn tại trong backend và không được trả về frontend.
 */
@Service
public class KeycloakAdminService {
    private static final Logger log = LoggerFactory.getLogger(KeycloakAdminService.class);

    private final RestTemplate restTemplate;
    private final KeycloakProperties properties;
    private final KeycloakFormClient formClient;

    public KeycloakAdminService(
            RestTemplate restTemplate,
            KeycloakProperties properties,
            KeycloakFormClient formClient) {
        this.restTemplate = restTemplate;
        this.properties = properties;
        this.formClient = formClient;
    }

    public String createUser(String fullName, String email, String password, String role) {
        return createUser(fullName, email, password, role, true, true);
    }

    public String createUser(
            String fullName,
            String email,
            String password,
            String role,
            boolean enabled,
            boolean emailVerified) {
        String adminToken = getAdminAccessToken();
        HttpHeaders headers = adminHeaders(adminToken);

        Map<String, Object> credential = new HashMap<>();
        credential.put("type", "password");
        credential.put("value", password);
        credential.put("temporary", false);

        NameParts name = splitName(fullName);
        Map<String, Object> payload = new HashMap<>();
        payload.put("username", email);
        payload.put("email", email);
        payload.put("firstName", name.firstName());
        payload.put("lastName", name.lastName());
        payload.put("enabled", enabled);
        payload.put("emailVerified", emailVerified);
        payload.put("requiredActions", List.of());
        payload.put("credentials", List.of(credential));
        payload.put("attributes", Map.of("role", List.of(role)));

        try {
            ResponseEntity<Void> response = restTemplate.exchange(
                    usersUrl(), HttpMethod.POST, new HttpEntity<>(payload, headers), Void.class);
            URI location = response.getHeaders().getLocation();
            if (location == null) {
                throw new RuntimeException("Keycloak khong tra ve user id");
            }
            String path = location.getPath();
            String userId = path.substring(path.lastIndexOf('/') + 1);
            assignRealmRole(userId, role, adminToken);
            return userId;
        } catch (HttpClientErrorException.Conflict ex) {
            String userId = findUserIdByEmail(email, adminToken);
            prepareExistingUser(userId, fullName, email, password, role, adminToken);
            if (!enabled || !emailVerified) {
                updateUserVerificationState(userId, enabled, emailVerified, adminToken);
            }
            return userId;
        } catch (HttpClientErrorException ex) {
            throw new RuntimeException(
                    "Khong tao duoc user tren Keycloak: " + ex.getResponseBodyAsString());
        }
    }

    public String findUserIdByEmail(String email) {
        return findUserIdByEmail(email, getAdminAccessToken());
    }

    public void clearRequiredActions(String userId) {
        updateUserSetup(userId, null, null, null, getAdminAccessToken());
    }

    public void setUserEnabled(String userId, boolean enabled) {
        if (userId == null || userId.isBlank()) {
            return;
        }
        try {
            restTemplate.exchange(
                    userUrl(userId), HttpMethod.PUT,
                    new HttpEntity<>(Map.of("enabled", enabled), adminHeaders(getAdminAccessToken())),
                    Void.class);
        } catch (HttpClientErrorException.NotFound ex) {
            log.warn("Keycloak user already missing id={}", userId);
        } catch (HttpClientErrorException ex) {
            throw new RuntimeException(
                    "Khong cap nhat duoc trang thai user tren Keycloak: " + ex.getResponseBodyAsString());
        }
    }

    public void updateUserProfile(String userId, String fullName, String email, String role) {
        if (userId == null || userId.isBlank()) {
            return;
        }
        try {
            updateUserSetup(userId, fullName, email, role, getAdminAccessToken());
        } catch (HttpClientErrorException.NotFound ex) {
            log.warn("Keycloak user already missing id={}", userId);
        } catch (HttpClientErrorException ex) {
            throw new RuntimeException(
                    "Khong cap nhat duoc thong tin user tren Keycloak: " + ex.getResponseBodyAsString());
        }
    }

    public void repairUserPasswordLogin(
            String userId, String fullName, String email, String password, String role) {
        String adminToken = getAdminAccessToken();
        String resolvedUserId = email == null || email.isBlank()
                ? userId
                : findUserIdByEmail(email, adminToken);
        prepareExistingUser(resolvedUserId, fullName, email, password, role, adminToken);
    }

    public void resetUserPassword(String userId, String password) {
        if (userId == null || userId.isBlank()) {
            throw new RuntimeException("Tai khoan chua duoc dong bo voi Keycloak");
        }
        resetPassword(userId, password, getAdminAccessToken());
    }

    public void verifyUserEmail(String userId) {
        if (userId == null || userId.isBlank()) {
            throw new RuntimeException("Tai khoan chua duoc dong bo voi Keycloak");
        }
        updateUserVerificationState(userId, true, true, getAdminAccessToken());
    }

    private String findUserIdByEmail(String email, String adminToken) {
        String url = UriComponentsBuilder.fromUriString(usersUrl())
                .queryParam("email", email)
                .queryParam("exact", true)
                .toUriString();
        ResponseEntity<List<Map<String, Object>>> response = restTemplate.exchange(
                url, HttpMethod.GET, new HttpEntity<>(bearerHeaders(adminToken)),
                new ParameterizedTypeReference<>() {
                });
        if (response.getBody() == null || response.getBody().isEmpty()) {
            throw new RuntimeException("Email da ton tai tren Keycloak nhung khong tim thay user id");
        }
        Object id = response.getBody().get(0).get("id");
        if (id == null) {
            throw new RuntimeException("Keycloak user id khong hop le");
        }
        return id.toString();
    }

    private void updateUserVerificationState(
            String userId, boolean enabled, boolean emailVerified, String adminToken) {
        restTemplate.exchange(
                userUrl(userId), HttpMethod.PUT,
                new HttpEntity<>(Map.of("enabled", enabled, "emailVerified", emailVerified),
                        adminHeaders(adminToken)),
                Void.class);
    }

    private void prepareExistingUser(
            String userId,
            String fullName,
            String email,
            String password,
            String role,
            String adminToken) {
        updateUserSetup(userId, fullName, email, role, adminToken);
        resetPassword(userId, password, adminToken);
        assignRealmRole(userId, role, adminToken);
    }

    private void updateUserSetup(
            String userId, String fullName, String email, String role, String adminToken) {
        HttpHeaders headers = adminHeaders(adminToken);
        Map<String, Object> payload = new HashMap<>();
        payload.put("enabled", true);
        payload.put("emailVerified", true);
        payload.put("requiredActions", List.of());

        if (fullName != null && !fullName.isBlank()) {
            NameParts name = splitName(fullName);
            payload.put("firstName", name.firstName());
            payload.put("lastName", name.lastName());
        }
        if (email != null && !email.isBlank()) {
            payload.put("username", email);
            payload.put("email", email);
        }
        if (role != null && !role.isBlank()) {
            payload.put("attributes", Map.of("role", List.of(role)));
        }

        restTemplate.exchange(
                userUrl(userId), HttpMethod.PUT, new HttpEntity<>(payload, headers), Void.class);
        logKeycloakUserState(userId, headers);
    }

    private void resetPassword(String userId, String password, String adminToken) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("type", "password");
        payload.put("value", password);
        payload.put("temporary", false);
        restTemplate.exchange(
                resetPasswordUrl(userId), HttpMethod.PUT,
                new HttpEntity<>(payload, adminHeaders(adminToken)), Void.class);
    }

    private void logKeycloakUserState(String userId, HttpHeaders headers) {
        ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                userUrl(userId), HttpMethod.GET, new HttpEntity<>(headers),
                new ParameterizedTypeReference<>() {
                });
        Map<String, Object> body = response.getBody();
        if (body != null) {
            log.info(
                    "Keycloak user state id={} enabled={} emailVerified={} requiredActions={} username={} email={}",
                    userId, body.get("enabled"), body.get("emailVerified"), body.get("requiredActions"),
                    body.get("username"), body.get("email"));
        }
    }

    private void assignRealmRole(String userId, String role, String adminToken) {
        HttpHeaders headers = adminHeaders(adminToken);
        ResponseEntity<Map<String, Object>> roleResponse = restTemplate.exchange(
                realmRoleUrl(role), HttpMethod.GET, new HttpEntity<>(headers),
                new ParameterizedTypeReference<>() {
                });
        if (roleResponse.getBody() == null) {
            throw new RuntimeException("Khong tim thay role Keycloak: " + role);
        }
        restTemplate.exchange(
                userRealmRoleMappingsUrl(userId), HttpMethod.POST,
                new HttpEntity<>(List.of(roleResponse.getBody()), headers), Void.class);
    }

    private String getAdminAccessToken() {
        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("grant_type", "password");
        body.add("client_id", properties.getAdminClientId());
        body.add("username", properties.getAdminUsername());
        body.add("password", properties.getAdminPassword());
        Object accessToken = formClient.postForMap(masterTokenUrl(), body).get("access_token");
        if (accessToken == null) {
            throw new RuntimeException("Keycloak admin token khong hop le");
        }
        return accessToken.toString();
    }

    private HttpHeaders adminHeaders(String token) {
        HttpHeaders headers = bearerHeaders(token);
        headers.setContentType(MediaType.APPLICATION_JSON);
        return headers;
    }

    private HttpHeaders bearerHeaders(String token) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        return headers;
    }

    private NameParts splitName(String fullName) {
        if (fullName == null || fullName.isBlank()) {
            return new NameParts("SBQS", "User");
        }
        String trimmed = fullName.trim();
        int lastSpace = trimmed.lastIndexOf(' ');
        return lastSpace < 0
                ? new NameParts(trimmed, "User")
                : new NameParts(trimmed.substring(0, lastSpace).trim(), trimmed.substring(lastSpace + 1).trim());
    }

    private String masterTokenUrl() {
        return properties.getServerUrl() + "/realms/master/protocol/openid-connect/token";
    }

    private String usersUrl() {
        return properties.getServerUrl() + "/admin/realms/" + properties.getRealm() + "/users";
    }

    private String userUrl(String userId) {
        return usersUrl() + "/" + userId;
    }

    private String resetPasswordUrl(String userId) {
        return userUrl(userId) + "/reset-password";
    }

    private String realmRoleUrl(String role) {
        return properties.getServerUrl() + "/admin/realms/" + properties.getRealm() + "/roles/" + role;
    }

    private String userRealmRoleMappingsUrl(String userId) {
        return userUrl(userId) + "/role-mappings/realm";
    }

    private record NameParts(String firstName, String lastName) {
    }
}
