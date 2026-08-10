package com.sbqs.service;

import com.sbqs.dto.CreateStaffRequest;
import com.sbqs.dto.UpdateUserRequest;
import com.sbqs.entity.Branch;
import com.sbqs.entity.Counter;
import com.sbqs.entity.CounterSession;
import com.sbqs.entity.User;
import com.sbqs.repository.BranchRepository;
import com.sbqs.repository.CounterRepository;
import com.sbqs.repository.CounterSessionRepository;
import com.sbqs.repository.DigitalDelegationRepository;
import com.sbqs.repository.UserRepository;
import com.sbqs.util.PasswordPolicy;
import jakarta.transaction.Transactional;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;

@Service
public class UserService {

        private final UserRepository userRepository;
        private final BranchRepository branchRepository;
        private final KeycloakAdminService keycloakService;
        private final CurrentUserService currentUserService;
        private final PasswordEncoder passwordEncoder;
        private final CounterSessionRepository counterSessionRepository;
        private final CounterRepository counterRepository;
        private final DigitalDelegationRepository delegationRepository;

        public UserService(
                        UserRepository userRepository,
                        BranchRepository branchRepository,
                        KeycloakAdminService keycloakService,
                        CurrentUserService currentUserService,
                        PasswordEncoder passwordEncoder,
                        CounterSessionRepository counterSessionRepository,
                        CounterRepository counterRepository,
                        DigitalDelegationRepository delegationRepository) {

                this.userRepository = userRepository;
                this.branchRepository = branchRepository;
                this.keycloakService = keycloakService;
                this.currentUserService = currentUserService;
                this.passwordEncoder = passwordEncoder;
                this.counterSessionRepository = counterSessionRepository;
                this.counterRepository = counterRepository;
                this.delegationRepository = delegationRepository;
        }

        public List<User> getUsersByBranch(Long branchId) {

                requireBranchAccess(branchId);

                Branch branch = branchRepository.findById(branchId)
                                .orElseThrow(() -> new RuntimeException("Khong tim thay chi nhanh"));

                return userRepository.findByBranchAndStatusNotIgnoreCase(branch, "DELETED");
        }

        public List<User> getAllUsers() {
                User currentUser = currentUserService.requireUser();
                if ("SUPER_ADMIN".equals(currentUser.getRole())) {
                        return userRepository.findByStatusNotIgnoreCase("DELETED");
                }

                return userRepository.findByBranchAndStatusNotIgnoreCase(currentUser.getBranch(), "DELETED");
        }

        public List<User> getUsersByRole(String role) {
                User currentUser = currentUserService.requireUser();
                boolean managedRole = "STAFF".equalsIgnoreCase(role)
                                || "BRANCH_ADMIN".equalsIgnoreCase(role);
                if ("SUPER_ADMIN".equals(currentUser.getRole())) {
                        return managedRole
                                        ? userRepository.findByRole(role)
                                        : userRepository.findByRoleAndStatusNotIgnoreCase(role, "DELETED");
                }

                return managedRole
                                ? userRepository.findByBranchAndRole(currentUser.getBranch(), role)
                                : userRepository.findByBranchAndRoleAndStatusNotIgnoreCase(
                                                currentUser.getBranch(), role, "DELETED");
        }

        public User createStaff(CreateStaffRequest request) {
                request.setBranchId(resolveCurrentBranchAdminBranchId());
                return createBranchUser(request, "STAFF");
        }

        public User createAdminBranch(CreateStaffRequest request) {
                return createBranchUser(request, "BRANCH_ADMIN");
        }

        /** Tạo nhân sự đồng thời ở Keycloak và database, kèm BCrypt hash để hỗ trợ fallback. */
        private User createBranchUser(CreateStaffRequest request, String role) {
                String email = normalizeEmail(request.getEmail());
                if (userRepository.existsByEmailIgnoreCase(email)) {
                        throw new RuntimeException("Email đã tồn tại. Vui lòng sử dụng email khác");
                }

                if (userRepository.existsByPhone(request.getPhone())) {
                        throw new RuntimeException("Số điện thoại đã tồn tại. Vui lòng sử dụng số khác");
                }

                if (request.getConfirmPassword() == null
                                || !request.getConfirmPassword().equals(request.getPassword())) {
                        throw new RuntimeException("Mat khau xac nhan khong khop");
                }

                PasswordPolicy.validate(request.getPassword());

                Branch branch = branchRepository.findById(request.getBranchId())
                                .orElseThrow(() -> new RuntimeException("Khong tim thay chi nhanh"));

                String keycloakUserId = keycloakService.createUser(
                                request.getFullName(),
                                email,
                                request.getPassword(),
                                role);

                User user = new User();

                user.setFullName(request.getFullName());
                user.setEmail(email);
                user.setPhone(request.getPhone());
                user.setRole(role);
                user.setStatus("ACTIVE");
                user.setBranch(branch);
                user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
                user.setKeycloakUserId(keycloakUserId);

                return userRepository.save(user);
        }

        private Long resolveCurrentBranchAdminBranchId() {
                Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

                if (authentication == null || !(authentication.getPrincipal() instanceof Jwt jwt)) {
                        throw new RuntimeException("Khong xac dinh duoc tai khoan dang dang nhap");
                }

                String email = jwt.getClaimAsString("email");
                if (email == null || email.isBlank()) {
                        email = jwt.getClaimAsString("preferred_username");
                }

                User currentUser = userRepository.findByEmailIgnoreCase(email)
                                .orElseThrow(() -> new RuntimeException("Khong tim thay tai khoan admin chi nhanh"));

                if (currentUser.getBranch() == null) {
                        throw new RuntimeException("Admin chi nhanh chua duoc gan chi nhanh");
                }

                return currentUser.getBranch().getBranchId();
        }

        @Transactional
        /** Admin cập nhật nhân sự trong phạm vi được phép rồi đồng bộ hồ sơ/trạng thái sang Keycloak. */
        public User updateUser(Long userId, UpdateUserRequest request) {

                User user = userRepository.findById(userId)
                                .orElseThrow(() -> new RuntimeException("Khong tim thay nguoi dung"));

                requireUserManagementAccess(user);

                String email = normalizeEmail(request.getEmail());
                if (userRepository.existsByEmailIgnoreCaseAndUserIdNot(email, userId)) {
                        throw new RuntimeException("Email đã tồn tại. Vui lòng sử dụng email khác");
                }

                if (userRepository.existsByPhoneAndUserIdNot(request.getPhone(), userId)) {
                        throw new RuntimeException("Số điện thoại đã tồn tại. Vui lòng sử dụng số khác");
                }

                user.setFullName(request.getFullName());
                user.setEmail(email);
                user.setPhone(request.getPhone());

                if (request.getBranchId() != null) {
                        Branch branch = branchRepository.findById(request.getBranchId())
                                        .orElseThrow(() -> new RuntimeException("Khong tim thay chi nhanh"));
                        requireBranchAccess(branch.getBranchId());
                        user.setBranch(branch);
                }

                User savedUser = userRepository.save(user);
                keycloakService.updateUserProfile(
                                savedUser.getKeycloakUserId(),
                                savedUser.getFullName(),
                                savedUser.getEmail(),
                                savedUser.getRole());

                return savedUser;
        }

        @Transactional
        @CacheEvict(cacheNames = "queueMonitor", allEntries = true)
        /** Xóa vĩnh viễn nhân sự khỏi database và Keycloak; lịch sử dùng dữ liệu snapshot. */
        public void deleteUser(Long userId) {

                User user = userRepository.findById(userId)
                                .orElseThrow(() -> new RuntimeException("Khong tim thay nguoi dung"));

                requireUserManagementAccess(user);
                if (!List.of("STAFF", "BRANCH_ADMIN").contains(user.getRole())) {
                        throw new RuntimeException("Chi duoc xoa tai khoan nhan vien hoac quan tri chi nhanh");
                }

                closeIdleCounterSession(user);
                delegationRepository.clearVerifier(user);
                userRepository.delete(user);
                userRepository.flush();
                keycloakService.deleteUser(user.getKeycloakUserId(), user.getEmail());
        }

        private void closeIdleCounterSession(User user) {
                CounterSession session = counterSessionRepository
                                .findFirstByStaffIdAndStatusOrderByStartedAtDesc(user.getUserId(), "ACTIVE")
                                .orElse(null);
                if (session == null) {
                        return;
                }

                Counter counter = counterRepository.findById(session.getCounterId()).orElse(null);
                if (counter != null && counter.getCurrentTicket() != null) {
                        throw new RuntimeException(
                                        "Nhan vien dang phuc vu mot phieu. Hay hoan tat phieu truoc khi xoa tai khoan");
                }

                session.setEndedAt(LocalDateTime.now());
                session.setStatus("COMPLETED");
                counterSessionRepository.save(session);

                if (counter != null) {
                        counter.setStatus("INACTIVE");
                        counterRepository.save(counter);
                }
        }

        private void requireBranchAccess(Long branchId) {
                User currentUser = currentUserService.requireUser();
                if (!"SUPER_ADMIN".equals(currentUser.getRole())) {
                        currentUserService.requireBranch(branchId);
                }
        }

        /** Áp dụng ma trận: SUPER_ADMIN quản lý cấp dưới; BRANCH_ADMIN chỉ quản lý STAFF cùng chi nhánh. */
        private void requireUserManagementAccess(User targetUser) {
                User currentUser = currentUserService.requireUser();

                if ("SUPER_ADMIN".equals(currentUser.getRole())) {
                        if ("SUPER_ADMIN".equals(targetUser.getRole())) {
                                throw new RuntimeException("Khong duoc sua hoac xoa tai khoan super admin qua API nay");
                        }
                        return;
                }

                if (!"BRANCH_ADMIN".equals(currentUser.getRole())
                                || !"STAFF".equals(targetUser.getRole())
                                || currentUser.getBranch() == null
                                || targetUser.getBranch() == null
                                || !currentUser.getBranch().getBranchId()
                                                .equals(targetUser.getBranch().getBranchId())) {
                        throw new RuntimeException("Ban khong co quyen quan ly tai khoan nay");
                }
        }

        private String normalizeEmail(String email) {
                return email == null ? null : email.trim().toLowerCase(Locale.ROOT);
        }
}
