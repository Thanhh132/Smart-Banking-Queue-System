package com.sbqs.service;

import com.sbqs.entity.DigitalDelegation;
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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class CustomerAccountDeletionService {
    private final CurrentUserService currentUserService;
    private final UserRepository userRepository;
    private final TicketRepository ticketRepository;
    private final CustomerProfileRepository customerProfileRepository;
    private final AccountChangeTokenRepository accountChangeTokenRepository;
    private final EmailVerificationTokenRepository emailVerificationTokenRepository;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final WebPushDeliveryRepository webPushDeliveryRepository;
    private final WebPushSubscriptionRepository webPushSubscriptionRepository;
    private final DigitalDelegationRepository delegationRepository;
    private final KeycloakAdminService keycloakAdminService;

    public CustomerAccountDeletionService(
            CurrentUserService currentUserService,
            UserRepository userRepository,
            TicketRepository ticketRepository,
            CustomerProfileRepository customerProfileRepository,
            AccountChangeTokenRepository accountChangeTokenRepository,
            EmailVerificationTokenRepository emailVerificationTokenRepository,
            PasswordResetTokenRepository passwordResetTokenRepository,
            WebPushDeliveryRepository webPushDeliveryRepository,
            WebPushSubscriptionRepository webPushSubscriptionRepository,
            DigitalDelegationRepository delegationRepository,
            KeycloakAdminService keycloakAdminService) {
        this.currentUserService = currentUserService;
        this.userRepository = userRepository;
        this.ticketRepository = ticketRepository;
        this.customerProfileRepository = customerProfileRepository;
        this.accountChangeTokenRepository = accountChangeTokenRepository;
        this.emailVerificationTokenRepository = emailVerificationTokenRepository;
        this.passwordResetTokenRepository = passwordResetTokenRepository;
        this.webPushDeliveryRepository = webPushDeliveryRepository;
        this.webPushSubscriptionRepository = webPushSubscriptionRepository;
        this.delegationRepository = delegationRepository;
        this.keycloakAdminService = keycloakAdminService;
    }

    @Transactional
    public void deleteCurrentCustomerAccount() {
        User user = currentUserService.requireUser();
        if (!"CUSTOMER".equals(user.getRole())) {
            throw new RuntimeException("Chi khach hang moi co the tu xoa tai khoan");
        }
        if (!ticketRepository.findByCustomerUserIdAndStatusIn(
                user.getUserId(), List.of("WAITING", "SERVING")).isEmpty()) {
            throw new RuntimeException("Vui long huy hoac hoan tat phieu dang hoat dong truoc khi xoa tai khoan");
        }

        String keycloakUserId = user.getKeycloakUserId();
        String email = user.getEmail();
        accountChangeTokenRepository.deleteByUser(user);
        emailVerificationTokenRepository.deleteByUser(user);
        passwordResetTokenRepository.deleteByUser(user);
        // Keep the managed User graph consistent with the profile row being removed.
        // Otherwise saveAndFlush(user) sees the deleted profile as a transient child.
        user.setCustomerProfile(null);
        customerProfileRepository.deleteByUserUserId(user.getUserId());
        webPushDeliveryRepository.deleteBySubscriptionUserUserId(user.getUserId());
        webPushSubscriptionRepository.deleteByUserUserId(user.getUserId());

        List<DigitalDelegation> delegations = delegationRepository.findByOwnerOrderByCreatedAtDesc(user);
        delegations.stream()
                .filter(item -> List.of("ACTIVE", "VERIFIED").contains(item.getStatus()))
                .forEach(item -> item.setStatus("CANCELLED"));
        delegationRepository.saveAll(delegations);

        user.setStatus("DELETED");
        user.setFullName("Khach hang da xoa tai khoan");
        user.setEmail("deleted-customer-" + user.getUserId() + "@invalid.sbqs.local");
        user.setPhone(null);
        user.setPasswordHash(null);
        user.setKeycloakUserId(null);
        user.setIdentityProvider(null);
        userRepository.saveAndFlush(user);
        keycloakAdminService.deleteUser(keycloakUserId, email);
    }
}
