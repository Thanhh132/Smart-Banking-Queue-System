package com.sbqs.service;

import com.sbqs.dto.CreateStaffRequest;
import com.sbqs.dto.UpdateUserRequest;
import com.sbqs.entity.Branch;
import com.sbqs.entity.User;
import com.sbqs.repository.BranchRepository;
import com.sbqs.repository.UserRepository;
import com.sbqs.util.PasswordPolicy;
import jakarta.transaction.Transactional;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Locale;

@Service
public class UserService {

        private final UserRepository userRepository;
        private final BranchRepository branchRepository;
        private final KeycloakService keycloakService;
        private final CurrentUserService currentUserService;
        private final PasswordEncoder passwordEncoder;

        public UserService(
                        UserRepository userRepository,
                        BranchRepository branchRepository,
                        KeycloakService keycloakService,
                        CurrentUserService currentUserService,
                        PasswordEncoder passwordEncoder) {

                this.userRepository = userRepository;
                this.branchRepository = branchRepository;
                this.keycloakService = keycloakService;
                this.currentUserService = currentUserService;
                this.passwordEncoder = passwordEncoder;
        }

        public List<User> getUsersByBranch(Long branchId) {

                requireBranchAccess(branchId);

                Branch branch = branchRepository.findById(branchId)
                                .orElseThrow(() -> new RuntimeException("Khong tim thay chi nhanh"));

                return userRepository.findByBranch(branch)
                                .stream()
                                .filter(user -> !"DELETED".equalsIgnoreCase(user.getStatus()))
                                .toList();
        }

        public List<User> getAllUsers() {
                User currentUser = currentUserService.requireUser();
                if ("SUPER_ADMIN".equals(currentUser.getRole())) {
                        return userRepository.findAll()
                                        .stream()
                                        .filter(user -> !"DELETED".equalsIgnoreCase(user.getStatus()))
                                        .toList();
                }

                return userRepository.findByBranch(currentUser.getBranch())
                                .stream()
                                .filter(user -> !"DELETED".equalsIgnoreCase(user.getStatus()))
                                .toList();
        }

        public List<User> getUsersByRole(String role) {
                User currentUser = currentUserService.requireUser();
                if ("SUPER_ADMIN".equals(currentUser.getRole())) {
                        return userRepository.findByRole(role)
                                        .stream()
                                        .filter(user -> !"DELETED".equalsIgnoreCase(user.getStatus()))
                                        .toList();
                }

                return userRepository.findByBranchAndRole(currentUser.getBranch(), role)
                                .stream()
                                .filter(user -> !"DELETED".equalsIgnoreCase(user.getStatus()))
                                .toList();
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

                if (request.getStatus() != null && !request.getStatus().isBlank()) {
                        user.setStatus(request.getStatus().toUpperCase(Locale.ROOT));
                }

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

                if (request.getStatus() != null && !request.getStatus().isBlank()) {
                        keycloakService.setUserEnabled(
                                        savedUser.getKeycloakUserId(),
                                        "ACTIVE".equalsIgnoreCase(savedUser.getStatus()));
                }

                return savedUser;
        }

        @Transactional
        /** Khóa mềm tài khoản thay vì xóa để bảo toàn lịch sử giao dịch và audit ngân hàng. */
        public void deleteUser(Long userId) {

                User user = userRepository.findById(userId)
                                .orElseThrow(() -> new RuntimeException("Khong tim thay nguoi dung"));

                requireUserManagementAccess(user);

                // Khong xoa vat ly de giu lich su giao dich; lan xoa thu hai an khoi danh sach quan tri.
                user.setStatus("INACTIVE".equalsIgnoreCase(user.getStatus()) ? "DELETED" : "INACTIVE");
                userRepository.save(user);
                keycloakService.setUserEnabled(user.getKeycloakUserId(), false);
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
