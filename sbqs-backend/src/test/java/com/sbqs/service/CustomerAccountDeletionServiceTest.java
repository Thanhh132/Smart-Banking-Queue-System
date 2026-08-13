package com.sbqs.service;

import com.sbqs.entity.DigitalDelegation;
import com.sbqs.entity.CustomerProfile;
import com.sbqs.entity.Ticket;
import com.sbqs.entity.User;
import com.sbqs.repository.AccountChangeTokenRepository;
import com.sbqs.repository.CustomerProfileRepository;
import com.sbqs.repository.DigitalDelegationRepository;
import com.sbqs.repository.EmailVerificationTokenRepository;
import com.sbqs.repository.PasswordResetTokenRepository;
import com.sbqs.repository.TicketRepository;
import com.sbqs.repository.UserRepository;
import com.sbqs.repository.WebPushDeliveryRepository;
import com.sbqs.repository.WebPushSubscriptionRepository;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CustomerAccountDeletionServiceTest {

    @Test
    void anonymizesCustomerAndRemovesPrivateAccountData() {
        Fixture fixture = new Fixture();
        User customer = fixture.customer();
        DigitalDelegation delegation = new DigitalDelegation();
        delegation.setStatus("ACTIVE");
        when(fixture.currentUserService.requireUser()).thenReturn(customer);
        when(fixture.ticketRepository.findByCustomerUserIdAndStatusIn(7L, List.of("WAITING", "SERVING")))
                .thenReturn(List.of());
        when(fixture.delegationRepository.findByOwnerOrderByCreatedAtDesc(customer))
                .thenReturn(List.of(delegation));

        fixture.service().deleteCurrentCustomerAccount();

        verify(fixture.accountChangeTokenRepository).deleteByUser(customer);
        verify(fixture.emailVerificationTokenRepository).deleteByUser(customer);
        verify(fixture.passwordResetTokenRepository).deleteByUser(customer);
        verify(fixture.customerProfileRepository).deleteByUserUserId(7L);
        verify(fixture.webPushDeliveryRepository).deleteBySubscriptionUserUserId(7L);
        verify(fixture.webPushSubscriptionRepository).deleteByUserUserId(7L);
        verify(fixture.keycloakAdminService).deleteUser("kc-7", "customer@sbqs.test");
        assertEquals("DELETED", customer.getStatus());
        assertEquals("CANCELLED", delegation.getStatus());
        assertEquals("deleted-customer-7@invalid.sbqs.local", customer.getEmail());
        assertNull(customer.getPhone());
        assertNull(customer.getKeycloakUserId());
        assertNull(customer.getCustomerProfile());
    }

    @Test
    void rejectsDeletionWhileCustomerHasAnActiveTicket() {
        Fixture fixture = new Fixture();
        User customer = fixture.customer();
        when(fixture.currentUserService.requireUser()).thenReturn(customer);
        when(fixture.ticketRepository.findByCustomerUserIdAndStatusIn(7L, List.of("WAITING", "SERVING")))
                .thenReturn(List.of(new Ticket()));

        assertThrows(RuntimeException.class, () -> fixture.service().deleteCurrentCustomerAccount());

        verify(fixture.userRepository, never()).saveAndFlush(customer);
        verify(fixture.keycloakAdminService, never()).deleteUser("kc-7", "customer@sbqs.test");
    }

    private static class Fixture {
        final CurrentUserService currentUserService = mock(CurrentUserService.class);
        final UserRepository userRepository = mock(UserRepository.class);
        final TicketRepository ticketRepository = mock(TicketRepository.class);
        final CustomerProfileRepository customerProfileRepository = mock(CustomerProfileRepository.class);
        final AccountChangeTokenRepository accountChangeTokenRepository = mock(AccountChangeTokenRepository.class);
        final EmailVerificationTokenRepository emailVerificationTokenRepository = mock(EmailVerificationTokenRepository.class);
        final PasswordResetTokenRepository passwordResetTokenRepository = mock(PasswordResetTokenRepository.class);
        final WebPushDeliveryRepository webPushDeliveryRepository = mock(WebPushDeliveryRepository.class);
        final WebPushSubscriptionRepository webPushSubscriptionRepository = mock(WebPushSubscriptionRepository.class);
        final DigitalDelegationRepository delegationRepository = mock(DigitalDelegationRepository.class);
        final KeycloakAdminService keycloakAdminService = mock(KeycloakAdminService.class);

        CustomerAccountDeletionService service() {
            return new CustomerAccountDeletionService(
                    currentUserService, userRepository, ticketRepository, customerProfileRepository,
                    accountChangeTokenRepository, emailVerificationTokenRepository, passwordResetTokenRepository,
                    webPushDeliveryRepository, webPushSubscriptionRepository, delegationRepository, keycloakAdminService);
        }

        User customer() {
            User user = new User();
            user.setUserId(7L);
            user.setRole("CUSTOMER");
            user.setStatus("ACTIVE");
            user.setFullName("Customer");
            user.setEmail("customer@sbqs.test");
            user.setPhone("0900000000");
            user.setPasswordHash("hash");
            user.setKeycloakUserId("kc-7");
            CustomerProfile profile = new CustomerProfile();
            profile.setUser(user);
            user.setCustomerProfile(profile);
            return user;
        }
    }
}
