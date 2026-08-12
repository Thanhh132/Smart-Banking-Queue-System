package com.sbqs.service;

import com.sbqs.entity.CustomerProfile;
import com.sbqs.entity.User;
import com.sbqs.repository.CustomerProfileRepository;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class CustomerProfileServiceTest {
    @Test
    void readsProfileAsSourceOfTruthAndKeepsTransactionFieldsOut() {
        CustomerProfileRepository repository = mock(CustomerProfileRepository.class);
        User user = customer();
        user.setPermanentAddress("legacy address");
        user.setAccountNumber("legacy account");
        CustomerProfile profile = new CustomerProfile();
        profile.setUser(user);
        profile.setPermanentAddress("new profile address");
        when(repository.findByUserUserId(7L)).thenReturn(Optional.of(profile));

        CustomerProfileService service = new CustomerProfileService(repository);
        Map<String, String> values = service.values(user);

        assertEquals("new profile address", values.get("PERMANENT_ADDRESS"));
        assertFalse(values.containsKey("ACCOUNT_NUMBER"));
        assertFalse(values.containsKey("CARD_DELIVERY_ADDRESS"));
    }

    @Test
    void snapshotsOnlyRequiredProfileFields() {
        CustomerProfileRepository repository = mock(CustomerProfileRepository.class);
        User user = customer();
        CustomerProfile profile = new CustomerProfile();
        profile.setUser(user);
        profile.setPermanentAddress("A");
        profile.setIdentityNumber("012345678901");
        when(repository.findByUserUserId(7L)).thenReturn(Optional.of(profile));

        Map<String, Object> snapshot = new CustomerProfileService(repository).snapshot(
                user, List.of("FULL_NAME", "PERMANENT_ADDRESS"));

        assertEquals(Map.of("FULL_NAME", "Customer A", "PERMANENT_ADDRESS", "A"), snapshot);
    }

    private User customer() {
        User user = new User();
        user.setUserId(7L);
        user.setRole("CUSTOMER");
        user.setFullName("Customer A");
        user.setEmail("a@example.com");
        user.setPhone("0900000000");
        return user;
    }
}
