package com.sbqs.service;

import com.sbqs.config.KeycloakProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.util.UriComponentsBuilder;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.HttpServerErrorException;
import com.sbqs.exception.KeycloakUnavailableException;
import org.springframework.web.client.RestTemplate;

import java.net.URI;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class KeycloakService {

    private static final Logger log = LoggerFactory.getLogger(KeycloakService.class);

    private final RestTemplate restTemplate;
    private final KeycloakProperties keycloakProperties;

    public KeycloakService(
            RestTemplate restTemplate,
            KeycloakProperties keycloakProperties) {

        this.restTemplate = restTemplate;
        this.keycloakProperties = keycloakProperties;
    }

    /** Gọi token endpoint bằng password grant; đây là nơi xác minh email/mật khẩu chính. */
    public Map<String, Object> login(
            String email,
            String password) {

        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("grant_type", "password");
        body.add("client_id", keycloakProperties.getClientId());
        body.add("username", email);
        body.add("password", password);

        if (keycloakProperties.getClientSecret() != null
                && !keycloakProperties.getClientSecret().isBlank()) {
            body.add("client_secret", keycloakProperties.getClientSecret());
        }

        return postFormForMap(tokenUrl(), body);
    }

    /** Yêu cầu Keycloak xoay vòng/cấp lại token cho phiên đăng nhập đang còn hiệu lực. */
    public Map<String, Object> refreshToken(String refreshToken) {
        if (refreshToken == null || refreshToken.isBlank()) {
            throw new RuntimeException("Refresh token khong hop le");
        }

        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("grant_type", "refresh_token");
        body.add("client_id", keycloakProperties.getClientId());
        body.add("refresh_token", refreshToken);

        if (keycloakProperties.getClientSecret() != null
                && !keycloakProperties.getClientSecret().isBlank()) {
            body.add("client_secret", keycloakProperties.getClientSecret());
        }

        return postFormForMap(tokenUrl(), body);
    }

    public void logout(String refreshToken) {
        if (refreshToken == null || refreshToken.isBlank()) {
            throw new RuntimeException("Refresh token khong hop le");
        }

        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("client_id", keycloakProperties.getClientId());
        body.add("refresh_token", refreshToken);

        if (keycloakProperties.getClientSecret() != null
                && !keycloakProperties.getClientSecret().isBlank()) {
            body.add("client_secret", keycloakProperties.getClientSecret());
        }

        postFormForVoid(logoutUrl(), body);
    }

    /** Tạo user Keycloak đã kích hoạt, dùng cho tài khoản nhân sự do quản trị viên cấp. */
    public String createUser(
            String fullName,
            String email,
            String password,
            String role) {

        return createUser(fullName, email, password, role, true, true);
    }

    /** Tạo user với trạng thái enabled/emailVerified tùy luồng đăng ký hay quản trị nội bộ. */
    public String createUser(
            String fullName,
            String email,
            String password,
            String role,
            boolean enabled,
            boolean emailVerified) {

        String adminToken = getAdminAccessToken();

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(adminToken);
        headers.setContentType(MediaType.APPLICATION_JSON);

        Map<String, Object> passwordCredential = new HashMap<>();
        passwordCredential.put("type", "password");
        passwordCredential.put("value", password);
        passwordCredential.put("temporary", false);

        Map<String, Object> userPayload = new HashMap<>();
        userPayload.put("username", email);
        userPayload.put("email", email);
        NameParts nameParts = splitName(fullName);
        userPayload.put("firstName", nameParts.firstName());
        userPayload.put("lastName", nameParts.lastName());
        userPayload.put("enabled", enabled);
        userPayload.put("emailVerified", emailVerified);
        userPayload.put("requiredActions", List.of());
        userPayload.put("credentials", List.of(passwordCredential));
        userPayload.put("attributes", Map.of("role", List.of(role)));

        HttpEntity<Map<String, Object>> request =
                new HttpEntity<>(userPayload, headers);

        try {
            ResponseEntity<Void> response =
                    restTemplate.exchange(
                            usersUrl(),
                            HttpMethod.POST,
                            request,
                            Void.class);

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
                    "Khong tao duoc user tren Keycloak: "
                            + ex.getResponseBodyAsString());
        }
    }

    public String findUserIdByEmail(String email) {
        return findUserIdByEmail(email, getAdminAccessToken());
    }

    private String findUserIdByEmail(
            String email,
            String adminToken) {

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(adminToken);

        String url = UriComponentsBuilder.fromUriString(usersUrl())
                .queryParam("email", email)
                .queryParam("exact", true)
                .toUriString();

        ResponseEntity<List<Map<String, Object>>> response =
                restTemplate.exchange(
                        url,
                        HttpMethod.GET,
                        new HttpEntity<>(headers),
                        new ParameterizedTypeReference<>() {
                        });

        if (response.getBody() == null || response.getBody().isEmpty()) {
            throw new RuntimeException("Email da ton tai tren Keycloak nhung khong tim thay user id");
        }

        Map<String, Object> user = response.getBody().get(0);
        Object id = user.get("id");

        if (id == null) {
            throw new RuntimeException("Keycloak user id khong hop le");
        }

        return id.toString();
    }

    public void clearRequiredActions(String userId) {
        String adminToken = getAdminAccessToken();
        updateUserSetup(userId, null, null, null, adminToken);
    }

    public void setUserEnabled(String userId, boolean enabled) {
        if (userId == null || userId.isBlank()) {
            return;
        }

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(getAdminAccessToken());
        headers.setContentType(MediaType.APPLICATION_JSON);

        try {
            restTemplate.exchange(
                    userUrl(userId),
                    HttpMethod.PUT,
                    new HttpEntity<>(Map.of("enabled", enabled), headers),
                    Void.class);
        } catch (HttpClientErrorException.NotFound ex) {
            log.warn("Keycloak user already missing id={}", userId);
        } catch (HttpClientErrorException ex) {
            throw new RuntimeException(
                    "Khong cap nhat duoc trang thai user tren Keycloak: "
                            + ex.getResponseBodyAsString());
        }
    }

    /** Đồng bộ họ tên, email và role từ database nghiệp vụ sang danh tính Keycloak. */
    public void updateUserProfile(
            String userId,
            String fullName,
            String email,
            String role) {

        if (userId == null || userId.isBlank()) {
            return;
        }

        try {
            updateUserSetup(userId, fullName, email, role, getAdminAccessToken());
        } catch (HttpClientErrorException.NotFound ex) {
            log.warn("Keycloak user already missing id={}", userId);
        } catch (HttpClientErrorException ex) {
            throw new RuntimeException(
                    "Khong cap nhat duoc thong tin user tren Keycloak: "
                            + ex.getResponseBodyAsString());
        }
    }

    /** Sửa tài khoản Keycloak cũ thiếu cấu hình password/role để có thể đăng nhập lại. */
    public void repairUserPasswordLogin(
            String userId,
            String fullName,
            String email,
            String password,
            String role) {

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

    private void updateUserVerificationState(
            String userId, boolean enabled, boolean emailVerified, String adminToken) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(adminToken);
        headers.setContentType(MediaType.APPLICATION_JSON);
        restTemplate.exchange(
                userUrl(userId),
                HttpMethod.PUT,
                new HttpEntity<>(Map.of(
                        "enabled", enabled,
                        "emailVerified", emailVerified), headers),
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
            String userId,
            String fullName,
            String email,
            String role,
            String adminToken) {

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(adminToken);
        headers.setContentType(MediaType.APPLICATION_JSON);

        Map<String, Object> payload = new HashMap<>();
        payload.put("enabled", true);
        payload.put("emailVerified", true);
        payload.put("requiredActions", List.of());

        if (fullName != null && !fullName.isBlank()) {
            NameParts nameParts = splitName(fullName);
            payload.put("firstName", nameParts.firstName());
            payload.put("lastName", nameParts.lastName());
        }

        if (email != null && !email.isBlank()) {
            payload.put("username", email);
            payload.put("email", email);
        }

        if (role != null && !role.isBlank()) {
            payload.put("attributes", Map.of("role", List.of(role)));
        }

        restTemplate.exchange(
                userUrl(userId),
                HttpMethod.PUT,
                new HttpEntity<>(payload, headers),
                Void.class);

        logKeycloakUserState(userId, headers);
    }

    private void resetPassword(
            String userId,
            String password,
            String adminToken) {

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(adminToken);
        headers.setContentType(MediaType.APPLICATION_JSON);

        Map<String, Object> payload = new HashMap<>();
        payload.put("type", "password");
        payload.put("value", password);
        payload.put("temporary", false);

        restTemplate.exchange(
                resetPasswordUrl(userId),
                HttpMethod.PUT,
                new HttpEntity<>(payload, headers),
                Void.class);
    }

    private void logKeycloakUserState(String userId, HttpHeaders headers) {
        ResponseEntity<Map<String, Object>> response =
                restTemplate.exchange(
                        userUrl(userId),
                        HttpMethod.GET,
                        new HttpEntity<>(headers),
                        new ParameterizedTypeReference<>() {
                        });

        Map<String, Object> body = response.getBody();
        if (body == null) {
            return;
        }

        log.info(
                "Keycloak user state id={} enabled={} emailVerified={} requiredActions={} username={} email={} firstName={} lastName={}",
                userId,
                body.get("enabled"),
                body.get("emailVerified"),
                body.get("requiredActions"),
                body.get("username"),
                body.get("email"),
                body.get("firstName"),
                body.get("lastName"));
    }

    private NameParts splitName(String fullName) {
        if (fullName == null || fullName.isBlank()) {
            return new NameParts("SBQS", "User");
        }

        String trimmed = fullName.trim();
        int lastSpace = trimmed.lastIndexOf(' ');

        if (lastSpace < 0) {
            return new NameParts(trimmed, "User");
        }

        return new NameParts(
                trimmed.substring(0, lastSpace).trim(),
                trimmed.substring(lastSpace + 1).trim());
    }

    private record NameParts(String firstName, String lastName) {
    }

    /** Lấy admin token chỉ dùng ở backend để gọi Keycloak Admin REST API. */
    private String getAdminAccessToken() {

        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("grant_type", "password");
        body.add("client_id", keycloakProperties.getAdminClientId());
        body.add("username", keycloakProperties.getAdminUsername());
        body.add("password", keycloakProperties.getAdminPassword());

        Map<String, Object> response =
                postFormForMap(masterTokenUrl(), body);

        Object accessToken = response.get("access_token");
        if (accessToken == null) {
            throw new RuntimeException("Keycloak admin token khong hop le");
        }

        return accessToken.toString();
    }

    /** Gán realm role vào user để Spring Security ánh xạ thành ROLE_* khi đọc JWT. */
    private void assignRealmRole(
            String userId,
            String role,
            String adminToken) {

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(adminToken);
        headers.setContentType(MediaType.APPLICATION_JSON);

        ResponseEntity<Map<String, Object>> roleResponse =
                restTemplate.exchange(
                        realmRoleUrl(role),
                        HttpMethod.GET,
                        new HttpEntity<>(headers),
                        new ParameterizedTypeReference<>() {
                        });

        if (roleResponse.getBody() == null) {
            throw new RuntimeException("Khong tim thay role Keycloak: " + role);
        }

        HttpEntity<List<Map<String, Object>>> request =
                new HttpEntity<>(List.of(roleResponse.getBody()), headers);

        restTemplate.exchange(
                userRealmRoleMappingsUrl(userId),
                HttpMethod.POST,
                request,
                Void.class);
    }

    /** Bao bọc các token endpoint và chuyển timeout/lỗi 5xx thành KeycloakUnavailableException. */
    private Map<String, Object> postFormForMap(
            String url,
            MultiValueMap<String, String> body) {

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        HttpEntity<MultiValueMap<String, String>> request =
                new HttpEntity<>(body, headers);

        try {
            log.info("Calling Keycloak form endpoint url={}", url);
            ResponseEntity<Map<String, Object>> response =
                    restTemplate.exchange(
                            url,
                            HttpMethod.POST,
                            request,
                            new ParameterizedTypeReference<>() {
                            });

            log.info("Keycloak form endpoint responded url={} status={}", url, response.getStatusCode());

            if (response.getBody() == null) {
                throw new RuntimeException("Keycloak khong tra ve du lieu");
            }

            return response.getBody();
        } catch (HttpClientErrorException ex) {
            throw new RuntimeException(
                    "Keycloak tu choi yeu cau: "
                            + ex.getResponseBodyAsString());
        } catch (ResourceAccessException ex) {
            log.warn("Cannot connect to Keycloak url={}", url, ex);
            throw new KeycloakUnavailableException("Dich vu xac thuc tam thoi khong san sang", ex);
        } catch (HttpServerErrorException ex) {
            log.warn("Keycloak server error url={} status={}", url, ex.getStatusCode());
            throw new KeycloakUnavailableException("Dich vu xac thuc tam thoi khong san sang", ex);
        }
    }

    private void postFormForVoid(
            String url,
            MultiValueMap<String, String> body) {

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(body, headers);

        try {
            restTemplate.postForEntity(url, request, Void.class);
        } catch (HttpClientErrorException ex) {
            throw new RuntimeException("Keycloak tu choi logout: " + ex.getResponseBodyAsString());
        } catch (ResourceAccessException ex) {
            log.warn("Cannot connect to Keycloak logout endpoint url={}", url, ex);
            throw new RuntimeException("Khong ket noi duoc Keycloak de dang xuat");
        }
    }

    private String tokenUrl() {
        return keycloakProperties.getServerUrl()
                + "/realms/"
                + keycloakProperties.getRealm()
                + "/protocol/openid-connect/token";
    }

    private String logoutUrl() {
        return keycloakProperties.getServerUrl()
                + "/realms/"
                + keycloakProperties.getRealm()
                + "/protocol/openid-connect/logout";
    }

    private String masterTokenUrl() {
        return keycloakProperties.getServerUrl()
                + "/realms/master/protocol/openid-connect/token";
    }

    private String usersUrl() {
        return keycloakProperties.getServerUrl()
                + "/admin/realms/"
                + keycloakProperties.getRealm()
                + "/users";
    }

    private String userUrl(String userId) {
        return usersUrl() + "/" + userId;
    }

    private String resetPasswordUrl(String userId) {
        return userUrl(userId) + "/reset-password";
    }

    private String realmRoleUrl(String role) {
        return keycloakProperties.getServerUrl()
                + "/admin/realms/"
                + keycloakProperties.getRealm()
                + "/roles/"
                + role;
    }

    private String userRealmRoleMappingsUrl(String userId) {
        return keycloakProperties.getServerUrl()
                + "/admin/realms/"
                + keycloakProperties.getRealm()
                + "/users/"
                + userId
                + "/role-mappings/realm";
    }
}
