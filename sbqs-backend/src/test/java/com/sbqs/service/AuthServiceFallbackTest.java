package com.sbqs.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sbqs.config.FallbackAuthProperties;
import com.sbqs.dto.LoginRequest;
import com.sbqs.dto.LoginResponse;
import com.sbqs.entity.User;
import com.sbqs.exception.KeycloakUnavailableException;
import com.sbqs.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AuthServiceFallbackTest {
    private UserRepository userRepository;
    private KeycloakService keycloakService;
    private KeycloakAdminService keycloakAdminService;
    private FallbackTokenService fallbackTokenService;
    private AuthService authService;
    private PasswordEncoder passwordEncoder;

    @BeforeEach
    void setUp() {
        userRepository = mock(UserRepository.class);
        keycloakService = mock(KeycloakService.class);
        keycloakAdminService = mock(KeycloakAdminService.class);
        fallbackTokenService = mock(FallbackTokenService.class);
        passwordEncoder = new BCryptPasswordEncoder();

        FallbackAuthProperties properties = new FallbackAuthProperties();
        properties.setEnabled(true);
        properties.setAllowedRoles(java.util.List.of(
                "CUSTOMER", "STAFF", "BRANCH_ADMIN", "SUPER_ADMIN"));

        authService = new AuthService(
                userRepository,
                keycloakService,
                keycloakAdminService,
                new ObjectMapper(),
                passwordEncoder,
                fallbackTokenService,
                properties);
    }

    @Test
    void usesDatabaseFallbackOnlyWhenKeycloakIsUnavailable() {
        LoginRequest request = loginRequest(" Admin@Example.com ", "Correct#123");
        User user = activeUser("admin@example.com", "Correct#123", "BRANCH_ADMIN");

        when(keycloakService.login("admin@example.com", "Correct#123"))
                .thenThrow(new KeycloakUnavailableException("down", new RuntimeException("connection refused")));
        when(userRepository.findByEmailIgnoreCase("admin@example.com")).thenReturn(Optional.of(user));
        when(fallbackTokenService.issue(user)).thenReturn("fallback.jwt");
        when(fallbackTokenService.expiresInSeconds()).thenReturn(300);

        LoginResponse response = authService.login(request);

        assertEquals("fallback.jwt", response.getAccessToken());
        assertEquals("BRANCH_ADMIN", response.getRole());
        assertEquals(300, response.getExpiresIn());
        assertNull(response.getRefreshToken());
    }

    @Test
    void doesNotFallbackWhenKeycloakRejectsCredentials() {
        LoginRequest request = loginRequest("user@example.com", "Wrong#123");
        when(keycloakService.login("user@example.com", "Wrong#123"))
                .thenThrow(new RuntimeException("Keycloak tu choi yeu cau: invalid_grant"));

        assertThrows(RuntimeException.class, () -> authService.login(request));
        verify(fallbackTokenService, never()).issue(org.mockito.ArgumentMatchers.any());
    }

    private LoginRequest loginRequest(String email, String password) {
        LoginRequest request = new LoginRequest();
        request.setEmail(email);
        request.setPassword(password);
        return request;
    }

    private User activeUser(String email, String password, String role) {
        User user = new User();
        user.setUserId(1L);
        user.setEmail(email);
        user.setFullName("Test User");
        user.setRole(role);
        user.setStatus("ACTIVE");
        user.setPasswordHash(passwordEncoder.encode(password));
        return user;
    }
}
