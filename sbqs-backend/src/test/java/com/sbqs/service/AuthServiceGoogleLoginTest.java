package com.sbqs.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sbqs.config.FallbackAuthProperties;
import com.sbqs.config.KeycloakProperties;
import com.sbqs.dto.GoogleCodeExchangeRequest;
import com.sbqs.dto.LoginResponse;
import com.sbqs.entity.User;
import com.sbqs.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AuthServiceGoogleLoginTest {
    private final ObjectMapper objectMapper = new ObjectMapper();
    private UserRepository userRepository;
    private KeycloakService keycloakService;
    private KeycloakAdminService keycloakAdminService;
    private AuthService authService;

    @BeforeEach
    void setUp() {
        userRepository = mock(UserRepository.class);
        keycloakService = mock(KeycloakService.class);
        keycloakAdminService = mock(KeycloakAdminService.class);

        FallbackAuthProperties fallback = new FallbackAuthProperties();
        KeycloakProperties keycloak = new KeycloakProperties();
        keycloak.setServerUrl("http://localhost:8080");
        keycloak.setPublicUrl("http://localhost:8080");
        keycloak.setRealm("SBQS");
        keycloak.setClientId("sbqs-frontend");
        keycloak.setGoogleRedirectUri("http://localhost:4200/auth/google/callback");
        keycloak.setGoogleLoginEnabled(true);

        authService = new AuthService(
                userRepository,
                keycloakService,
                keycloakAdminService,
                objectMapper,
                new BCryptPasswordEncoder(),
                mock(FallbackTokenService.class),
                fallback,
                keycloak);
    }

    @Test
    void createsIncompleteCustomerProfileAndAssignsRoleForFirstGoogleLogin() throws Exception {
        String initialAccessToken = token(Map.of(
                "sub", "kc-user-1",
                "email", "new.customer@gmail.com"));
        String customerAccessToken = token(Map.of(
                "sub", "kc-user-1",
                "email", "new.customer@gmail.com",
                "identity_provider", "google",
                "realm_access", Map.of("roles", List.of("CUSTOMER"))));

        when(keycloakService.exchangeAuthorizationCode(
                "authorization-code", "abcdefghijklmnopqrstuvwxyz0123456789ABCDEFG",
                "http://localhost:4200/auth/google/callback"))
                .thenReturn(Map.of("access_token", initialAccessToken, "refresh_token", "refresh-1"));
        when(keycloakService.refreshToken("refresh-1")).thenReturn(Map.of(
                "access_token", customerAccessToken,
                "refresh_token", "refresh-2",
                "token_type", "Bearer",
                "expires_in", 300));
        when(userRepository.findByEmailIgnoreCase("new.customer@gmail.com"))
                .thenReturn(Optional.empty());
        when(keycloakAdminService.hasFederatedIdentity("kc-user-1", "google")).thenReturn(true);
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        LoginResponse response = authService.exchangeGoogleCode(new GoogleCodeExchangeRequest(
                "authorization-code", "abcdefghijklmnopqrstuvwxyz0123456789ABCDEFG"));

        verify(keycloakAdminService).assignRealmRole("kc-user-1", "CUSTOMER");
        verify(keycloakAdminService).hasFederatedIdentity("kc-user-1", "google");
        assertEquals("CUSTOMER", response.getRole());
        assertEquals("new.customer@gmail.com", response.getEmail());
        assertFalse(response.isProfileComplete());
    }

    private String token(Map<String, Object> payload) throws Exception {
        String header = Base64.getUrlEncoder().withoutPadding()
                .encodeToString("{\"alg\":\"none\"}".getBytes(StandardCharsets.UTF_8));
        String body = Base64.getUrlEncoder().withoutPadding()
                .encodeToString(objectMapper.writeValueAsBytes(payload));
        return header + "." + body + ".signature";
    }
}
