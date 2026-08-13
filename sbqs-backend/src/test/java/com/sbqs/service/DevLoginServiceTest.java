package com.sbqs.service;

import com.sbqs.dto.DevLoginAccountResponse;
import com.sbqs.dto.LoginResponse;
import com.sbqs.entity.User;
import com.sbqs.repository.UserRepository;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class DevLoginServiceTest {
    @Test
    void accountListContainsOnlyMinimalFieldsForActiveTestableRoles() {
        UserRepository users = mock(UserRepository.class);
        User customer = user(2L, "Customer", "customer@example.com", "CUSTOMER", "ACTIVE");
        User admin = user(1L, "Admin", "admin@example.com", "SUPER_ADMIN", "ACTIVE");
        User inactive = user(3L, "Inactive", "inactive@example.com", "STAFF", "INACTIVE");
        User unsupported = user(4L, "Auditor", "auditor@example.com", "AUDITOR", "ACTIVE");
        when(users.findByStatusNotIgnoreCase("DELETED")).thenReturn(List.of(admin, customer, inactive, unsupported));

        List<DevLoginAccountResponse> accounts = new DevLoginService(users, mock(FallbackTokenService.class)).accounts();

        assertEquals(2, accounts.size());
        assertEquals("CUSTOMER", accounts.getFirst().role());
        assertArrayEquals(
                new String[]{"userId", "displayName", "role", "branchName"},
                java.util.Arrays.stream(DevLoginAccountResponse.class.getRecordComponents())
                        .map(java.lang.reflect.RecordComponent::getName)
                        .toArray(String[]::new));
        verify(users).findByStatusNotIgnoreCase("DELETED");
    }

    @Test
    void quickLoginUsesSelectedUserIdAndRejectsUnavailableAccount() {
        UserRepository users = mock(UserRepository.class);
        FallbackTokenService tokens = mock(FallbackTokenService.class);
        DevLoginService service = new DevLoginService(users, tokens);
        User active = user(7L, "Staff", "staff@example.com", "STAFF", "ACTIVE");
        when(users.findById(7L)).thenReturn(Optional.of(active));
        when(tokens.issueDevelopment(active)).thenReturn("dev.jwt");
        when(tokens.expiresInSeconds()).thenReturn(300);

        LoginResponse response = service.login(7L);

        assertEquals("dev.jwt", response.getAccessToken());
        assertEquals("DEV_QUICK_LOGIN", response.getAuthenticationSource());
        verify(tokens).issueDevelopment(active);

        User locked = user(8L, "Locked", "locked@example.com", "CUSTOMER", "INACTIVE");
        when(users.findById(8L)).thenReturn(Optional.of(locked));
        assertThrows(RuntimeException.class, () -> service.login(8L));
        verify(tokens, never()).issueDevelopment(locked);

        when(users.findById(999L)).thenReturn(Optional.empty());
        assertThrows(RuntimeException.class, () -> service.login(999L));
        verify(tokens, never()).issueDevelopment(argThat(user -> user != active));
    }

    private User user(Long id, String name, String email, String role, String status) {
        User user = new User();
        user.setUserId(id);
        user.setFullName(name);
        user.setEmail(email);
        user.setRole(role);
        user.setStatus(status);
        return user;
    }
}
