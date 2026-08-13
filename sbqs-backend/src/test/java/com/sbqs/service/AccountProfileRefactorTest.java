package com.sbqs.service;

import com.sbqs.dto.CompleteSocialProfileRequest;
import com.sbqs.dto.UpdateCustomerPaperlessProfileRequest;
import com.sbqs.entity.CustomerProfile;
import com.sbqs.entity.User;
import com.sbqs.repository.ServiceRepository;
import com.sbqs.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

class AccountProfileRefactorTest {
    @Test
    void accountReadKeepsAccountDataFromUser() {
        Fixture fixture = fixture();
        assertEquals("customer@example.com", fixture.service.getProfile().email());
        assertEquals("Customer", fixture.service.getProfile().fullName());
    }

    @Test
    void paperlessUpdateWritesCustomerProfileNotLegacyUserColumns() {
        Fixture fixture = fixture();
        when(fixture.profiles.values(fixture.user)).thenReturn(Map.of(
                "FULL_NAME", "Customer", "MOBILE_PHONE", "0900000000",
                "PERMANENT_ADDRESS", "New A", "CONTACT_ADDRESS", "New B"));
        when(fixture.profiles.value(fixture.user, "FULL_NAME")).thenReturn("Customer");
        when(fixture.profiles.value(fixture.user, "MOBILE_PHONE")).thenReturn("0900000000");
        when(fixture.profiles.value(fixture.user, "PERMANENT_ADDRESS")).thenReturn("New A");
        when(fixture.profiles.value(fixture.user, "CONTACT_ADDRESS")).thenReturn("New B");

        fixture.service.updatePaperlessProfile(new UpdateCustomerPaperlessProfileRequest(
                null, Map.of("PERMANENT_ADDRESS", "New A", "CONTACT_ADDRESS", "New B")));

        verify(fixture.profiles).apply(fixture.profile, "PERMANENT_ADDRESS", "New A");
        verify(fixture.profiles).apply(fixture.profile, "CONTACT_ADDRESS", "New B");
        verify(fixture.profiles).save(fixture.profile);
    }

    @Test
    void socialCompletionSplitsAccountAndProfileWrites() {
        Fixture fixture = fixture();
        fixture.service.completeSocialProfile(new CompleteSocialProfileRequest(
                "New Name", "0911111111", "Permanent", "Contact"));

        assertEquals("New Name", fixture.user.getFullName());
        assertEquals("0911111111", fixture.user.getPhone());
        assertEquals("Permanent", fixture.profile.getPermanentAddress());
        assertEquals("Contact", fixture.profile.getContactAddress());
        verify(fixture.profiles).save(fixture.profile);
    }

    private Fixture fixture() {
        CurrentUserService current = mock(CurrentUserService.class);
        UserRepository users = mock(UserRepository.class);
        KeycloakService keycloak = mock(KeycloakService.class);
        KeycloakAdminService keycloakAdmin = mock(KeycloakAdminService.class);
        CustomerProfileService profiles = mock(CustomerProfileService.class);
        User user = new User();
        user.setUserId(5L);
        user.setRole("CUSTOMER");
        user.setFullName("Customer");
        user.setEmail("customer@example.com");
        user.setPhone("0900000000");
        user.setKeycloakUserId("kc-5");
        CustomerProfile profile = new CustomerProfile();
        profile.setUser(user);
        when(current.requireUser()).thenReturn(user);
        when(users.save(user)).thenReturn(user);
        when(profiles.requireForUpdate(user)).thenReturn(profile);
        AccountService service = new AccountService(
                current, users, keycloak, keycloakAdmin, mock(PasswordEncoder.class),
                mock(ServiceRepository.class), profiles);
        return new Fixture(service, user, profile, profiles);
    }

    private record Fixture(AccountService service, User user, CustomerProfile profile,
                           CustomerProfileService profiles) { }
}
