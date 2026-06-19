package com.sbqs.service;

import com.sbqs.config.KeycloakProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.util.UriComponentsBuilder;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.ResourceAccessException;
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

    public String createUser(
            String fullName,
            String email,
            String password,
            String role) {

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
        userPayload.put("enabled", true);
        userPayload.put("emailVerified", true);
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
            return userId;
        } catch (HttpClientErrorException ex) {
            throw new RuntimeException(
                    "Khong tao duoc user tren Keycloak: "
                            + ex.getResponseBodyAsString());
        }
    }

    @SuppressWarnings("unchecked")
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

        ResponseEntity<List> response =
                restTemplate.exchange(
                        url,
                        HttpMethod.GET,
                        new HttpEntity<>(headers),
                        List.class);

        if (response.getBody() == null || response.getBody().isEmpty()) {
            throw new RuntimeException("Email da ton tai tren Keycloak nhung khong tim thay user id");
        }

        Map<String, Object> user = (Map<String, Object>) response.getBody().get(0);
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
        ResponseEntity<Map> response =
                restTemplate.exchange(
                        userUrl(userId),
                        HttpMethod.GET,
                        new HttpEntity<>(headers),
                        Map.class);

        Map body = response.getBody();
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

    private void assignRealmRole(
            String userId,
            String role,
            String adminToken) {

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(adminToken);
        headers.setContentType(MediaType.APPLICATION_JSON);

        ResponseEntity<Map> roleResponse =
                restTemplate.exchange(
                        realmRoleUrl(role),
                        HttpMethod.GET,
                        new HttpEntity<>(headers),
                        Map.class);

        if (roleResponse.getBody() == null) {
            throw new RuntimeException("Khong tim thay role Keycloak: " + role);
        }

        HttpEntity<List<Map>> request =
                new HttpEntity<>(List.of(roleResponse.getBody()), headers);

        restTemplate.exchange(
                userRealmRoleMappingsUrl(userId),
                HttpMethod.POST,
                request,
                Void.class);
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> postFormForMap(
            String url,
            MultiValueMap<String, String> body) {

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        HttpEntity<MultiValueMap<String, String>> request =
                new HttpEntity<>(body, headers);

        try {
            log.info("Calling Keycloak form endpoint url={}", url);
            ResponseEntity<Map> response =
                    restTemplate.postForEntity(url, request, Map.class);

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
            throw new RuntimeException(
                    "Khong ket noi duoc Keycloak. Kiem tra Keycloak dang chay o "
                            + keycloakProperties.getServerUrl());
        }
    }

    private String tokenUrl() {
        return keycloakProperties.getServerUrl()
                + "/realms/"
                + keycloakProperties.getRealm()
                + "/protocol/openid-connect/token";
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
