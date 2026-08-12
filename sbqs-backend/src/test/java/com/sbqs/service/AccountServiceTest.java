package com.sbqs.service;

import com.sbqs.config.AccountChangeProperties;
import com.sbqs.dto.ChangePasswordRequest;
import com.sbqs.dto.UpdateAccountProfileRequest;
import com.sbqs.entity.User;
import com.sbqs.repository.AccountChangeTokenRepository;
import com.sbqs.repository.ServiceRepository;
import com.sbqs.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

class AccountServiceTest {

    @Test
    void changesPasswordInKeycloakAndFallbackHashAfterCurrentPasswordIsVerified() {
        CurrentUserService currentUserService = mock(CurrentUserService.class);
        UserRepository userRepository = mock(UserRepository.class);
        KeycloakService keycloakService = mock(KeycloakService.class);
        KeycloakAdminService keycloakAdminService = mock(KeycloakAdminService.class);
        PasswordEncoder passwordEncoder = mock(PasswordEncoder.class);
        ServiceRepository serviceRepository = mock(ServiceRepository.class);
        CustomerProfileService customerProfileService = mock(CustomerProfileService.class);
        User user = user();
        when(currentUserService.requireUser()).thenReturn(user);
        when(passwordEncoder.encode("NewPassword2@")).thenReturn("new-hash");

        AccountService service = new AccountService(
                currentUserService, userRepository, keycloakService, keycloakAdminService, passwordEncoder,
                serviceRepository, customerProfileService);
        service.changePassword(new ChangePasswordRequest("CurrentPassword1!", "NewPassword2@"));

        verify(keycloakService).login(user.getEmail(), "CurrentPassword1!");
        verify(keycloakAdminService).resetUserPassword(user.getKeycloakUserId(), "NewPassword2@");
        verify(userRepository).save(user);
        assertEquals("new-hash", user.getPasswordHash());
    }

    @Test
    void rejectsSelfServiceProfileChangesForCompanyManagedRoles() {
        CurrentUserService currentUserService = mock(CurrentUserService.class);
        UserRepository userRepository = mock(UserRepository.class);
        KeycloakAdminService keycloakService = mock(KeycloakAdminService.class);
        AccountChangeTokenRepository changeTokenRepository = mock(AccountChangeTokenRepository.class);
        AuthenticationMailService mailService = mock(AuthenticationMailService.class);
        User user = user();
        when(currentUserService.requireUser()).thenReturn(user);

        AccountChangeService service = new AccountChangeService(
                currentUserService, userRepository, keycloakService, changeTokenRepository,
                mailService, new AccountChangeProperties());

        assertThrows(RuntimeException.class, () -> service.requestProfileChange(
                new UpdateAccountProfileRequest("Nguyen Van B", "new@example.com", "0909999999")));
        verifyNoInteractions(changeTokenRepository, mailService);
    }

    private User user() {
        User user = new User();
        user.setUserId(7L);
        user.setFullName("Nguyen Van A");
        user.setEmail("user@example.com");
        user.setPhone("0900000000");
        user.setRole("STAFF");
        user.setStatus("ACTIVE");
        user.setKeycloakUserId("kc-7");
        return user;
    }
}
