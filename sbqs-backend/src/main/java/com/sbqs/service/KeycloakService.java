package com.sbqs.service;

import com.sbqs.config.KeycloakProperties;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import java.util.Map;

/**
 * Chỉ xử lý phiên đăng nhập Keycloak: đăng nhập, làm mới token và đăng xuất.
 * Các thao tác có quyền quản trị được tách sang {@link KeycloakAdminService}.
 */
@Service
public class KeycloakService {
    private final KeycloakProperties properties;
    private final KeycloakFormClient formClient;

    public KeycloakService(KeycloakProperties properties, KeycloakFormClient formClient) {
        this.properties = properties;
        this.formClient = formClient;
    }

    public Map<String, Object> login(String email, String password) {
        MultiValueMap<String, String> body = clientCredentials();
        body.add("grant_type", "password");
        body.add("username", email);
        body.add("password", password);
        return formClient.postForMap(tokenUrl(), body);
    }

    public Map<String, Object> refreshToken(String refreshToken) {
        requireRefreshToken(refreshToken);
        MultiValueMap<String, String> body = clientCredentials();
        body.add("grant_type", "refresh_token");
        body.add("refresh_token", refreshToken);
        return formClient.postForMap(tokenUrl(), body);
    }

    public Map<String, Object> exchangeAuthorizationCode(String code, String codeVerifier, String redirectUri) {
        if (!properties.isGoogleLoginEnabled()) {
            throw new RuntimeException("Dang nhap Google chua duoc bat");
        }
        MultiValueMap<String, String> body = clientCredentials();
        body.add("grant_type", "authorization_code");
        body.add("code", code);
        body.add("code_verifier", codeVerifier);
        body.add("redirect_uri", redirectUri);
        return formClient.postForMap(tokenUrl(), body);
    }

    public void logout(String refreshToken) {
        requireRefreshToken(refreshToken);
        MultiValueMap<String, String> body = clientCredentials();
        body.add("refresh_token", refreshToken);
        formClient.postForVoid(logoutUrl(), body);
    }

    private MultiValueMap<String, String> clientCredentials() {
        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("client_id", properties.getClientId());
        if (properties.getClientSecret() != null && !properties.getClientSecret().isBlank()) {
            body.add("client_secret", properties.getClientSecret());
        }
        return body;
    }

    private void requireRefreshToken(String refreshToken) {
        if (refreshToken == null || refreshToken.isBlank()) {
            throw new RuntimeException("Refresh token khong hop le");
        }
    }

    private String tokenUrl() {
        return properties.getServerUrl() + "/realms/" + properties.getRealm()
                + "/protocol/openid-connect/token";
    }

    private String logoutUrl() {
        return properties.getServerUrl() + "/realms/" + properties.getRealm()
                + "/protocol/openid-connect/logout";
    }
}
