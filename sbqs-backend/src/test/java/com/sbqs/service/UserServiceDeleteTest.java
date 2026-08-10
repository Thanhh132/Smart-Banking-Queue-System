package com.sbqs.service;

import com.sbqs.entity.Branch;
import com.sbqs.entity.Counter;
import com.sbqs.entity.CounterSession;
import com.sbqs.entity.Ticket;
import com.sbqs.entity.User;
import com.sbqs.repository.BranchRepository;
import com.sbqs.repository.CounterRepository;
import com.sbqs.repository.CounterSessionRepository;
import com.sbqs.repository.DigitalDelegationRepository;
import com.sbqs.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class UserServiceDeleteTest {

    @Test
    void includesLegacySoftDeletedStaffSoTheyCanBePermanentlyRemoved() {
        Dependencies dependencies = new Dependencies();
        UserService service = dependencies.createService();
        User currentAdmin = branchAdmin(10L);

        when(dependencies.currentUserService.requireUser()).thenReturn(currentAdmin);

        service.getUsersByRole("STAFF");

        verify(dependencies.userRepository).findByBranchAndRole(currentAdmin.getBranch(), "STAFF");
        verify(dependencies.userRepository, never())
                .findByBranchAndRoleAndStatusNotIgnoreCase(currentAdmin.getBranch(), "STAFF", "DELETED");
    }

    @Test
    void deletesStaffFromDatabaseAndKeycloak() {
        Dependencies dependencies = new Dependencies();
        UserService service = dependencies.createService();
        User currentAdmin = branchAdmin(10L);
        User staff = staff(21L, 10L);

        when(dependencies.currentUserService.requireUser()).thenReturn(currentAdmin);
        when(dependencies.userRepository.findById(21L)).thenReturn(Optional.of(staff));
        when(dependencies.counterSessionRepository
                .findFirstByStaffIdAndStatusOrderByStartedAtDesc(21L, "ACTIVE"))
                .thenReturn(Optional.empty());

        service.deleteUser(21L);

        verify(dependencies.delegationRepository).clearVerifier(staff);
        verify(dependencies.userRepository).delete(staff);
        verify(dependencies.userRepository).flush();
        verify(dependencies.keycloakAdminService).deleteUser("kc-staff-21", "staff21@sbqs.test");
        verify(dependencies.keycloakAdminService, never()).setUserEnabled("kc-staff-21", false);
    }

    @Test
    void rejectsDeletionWhileStaffIsServingTicket() {
        Dependencies dependencies = new Dependencies();
        UserService service = dependencies.createService();
        User currentAdmin = branchAdmin(10L);
        User staff = staff(21L, 10L);
        CounterSession session = new CounterSession();
        session.setCounterId(31L);
        session.setStaffId(21L);
        session.setStatus("ACTIVE");
        Counter counter = new Counter();
        counter.setCounterId(31L);
        counter.setCurrentTicket(new Ticket());

        when(dependencies.currentUserService.requireUser()).thenReturn(currentAdmin);
        when(dependencies.userRepository.findById(21L)).thenReturn(Optional.of(staff));
        when(dependencies.counterSessionRepository
                .findFirstByStaffIdAndStatusOrderByStartedAtDesc(21L, "ACTIVE"))
                .thenReturn(Optional.of(session));
        when(dependencies.counterRepository.findById(31L)).thenReturn(Optional.of(counter));

        assertThrows(RuntimeException.class, () -> service.deleteUser(21L));

        verify(dependencies.userRepository, never()).delete(staff);
        verify(dependencies.keycloakAdminService, never()).deleteUser("kc-staff-21", "staff21@sbqs.test");
    }

    private static User branchAdmin(Long branchId) {
        User user = new User();
        user.setUserId(1L);
        user.setRole("BRANCH_ADMIN");
        user.setBranch(branch(branchId));
        return user;
    }

    private static User staff(Long userId, Long branchId) {
        User user = new User();
        user.setUserId(userId);
        user.setRole("STAFF");
        user.setEmail("staff" + userId + "@sbqs.test");
        user.setKeycloakUserId("kc-staff-" + userId);
        user.setBranch(branch(branchId));
        return user;
    }

    private static Branch branch(Long branchId) {
        Branch branch = new Branch();
        branch.setBranchId(branchId);
        return branch;
    }

    private static class Dependencies {
        private final UserRepository userRepository = mock(UserRepository.class);
        private final BranchRepository branchRepository = mock(BranchRepository.class);
        private final KeycloakAdminService keycloakAdminService = mock(KeycloakAdminService.class);
        private final CurrentUserService currentUserService = mock(CurrentUserService.class);
        private final PasswordEncoder passwordEncoder = mock(PasswordEncoder.class);
        private final CounterSessionRepository counterSessionRepository = mock(CounterSessionRepository.class);
        private final CounterRepository counterRepository = mock(CounterRepository.class);
        private final DigitalDelegationRepository delegationRepository =
                mock(DigitalDelegationRepository.class);

        private UserService createService() {
            return new UserService(
                    userRepository,
                    branchRepository,
                    keycloakAdminService,
                    currentUserService,
                    passwordEncoder,
                    counterSessionRepository,
                    counterRepository,
                    delegationRepository);
        }
    }
}
