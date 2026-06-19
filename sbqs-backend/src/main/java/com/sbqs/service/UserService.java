package com.sbqs.service;

import com.sbqs.dto.CreateStaffRequest;
import com.sbqs.dto.UpdateUserRequest;
import com.sbqs.entity.Branch;
import com.sbqs.entity.User;
import com.sbqs.repository.BranchRepository;
import com.sbqs.repository.UserRepository;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class UserService {

        private final UserRepository userRepository;
        private final BranchRepository branchRepository;
        private final KeycloakService keycloakService;

        public UserService(
                        UserRepository userRepository,
                        BranchRepository branchRepository,
                        KeycloakService keycloakService) {

                this.userRepository = userRepository;
                this.branchRepository = branchRepository;
                this.keycloakService = keycloakService;
        }

        public List<User> getUsersByBranch(Long branchId) {

                Branch branch = branchRepository.findById(branchId)
                                .orElseThrow(() -> new RuntimeException("Khong tim thay chi nhanh"));

                return userRepository.findByBranch(branch);
        }

        public List<User> getAllUsers() {
                return userRepository.findAll();
        }

        public List<User> getUsersByRole(String role) {
                return userRepository.findByRole(role);
        }

        public User createStaff(CreateStaffRequest request) {
                request.setBranchId(resolveCurrentBranchAdminBranchId());
                return createBranchUser(request, "STAFF");
        }

        public User createAdminBranch(CreateStaffRequest request) {
                return createBranchUser(request, "BRANCH_ADMIN");
        }

        private User createBranchUser(CreateStaffRequest request, String role) {
                if (userRepository.existsByEmail(request.getEmail())) {
                        throw new RuntimeException("Email da ton tai");
                }

                if (userRepository.existsByPhone(request.getPhone())) {
                        throw new RuntimeException("So dien thoai da ton tai");
                }

                Branch branch = branchRepository.findById(request.getBranchId())
                                .orElseThrow(() -> new RuntimeException("Khong tim thay chi nhanh"));

                String keycloakUserId = keycloakService.createUser(
                                request.getFullName(),
                                request.getEmail(),
                                request.getPassword(),
                                role);

                User user = new User();

                user.setFullName(request.getFullName());
                user.setEmail(request.getEmail());
                user.setPhone(request.getPhone());
                user.setRole(role);
                user.setStatus("ACTIVE");
                user.setBranch(branch);
                user.setPasswordHash("KEYCLOAK_MANAGED");
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

                User currentUser = userRepository.findByEmail(email)
                                .orElseThrow(() -> new RuntimeException("Khong tim thay tai khoan admin chi nhanh"));

                if (currentUser.getBranch() == null) {
                        throw new RuntimeException("Admin chi nhanh chua duoc gan chi nhanh");
                }

                return currentUser.getBranch().getBranchId();
        }

        public User updateUser(Long userId, UpdateUserRequest request) {

                User user = userRepository.findById(userId)
                                .orElseThrow(() -> new RuntimeException("Khong tim thay nguoi dung"));

                if (userRepository.existsByEmailAndUserIdNot(request.getEmail(), userId)) {
                        throw new RuntimeException("Email da ton tai");
                }

                if (userRepository.existsByPhoneAndUserIdNot(request.getPhone(), userId)) {
                        throw new RuntimeException("So dien thoai da ton tai");
                }

                user.setFullName(request.getFullName());
                user.setEmail(request.getEmail());
                user.setPhone(request.getPhone());

                if (request.getStatus() != null && !request.getStatus().isBlank()) {
                        user.setStatus(request.getStatus());
                }

                return userRepository.save(user);
        }

        public void deleteUser(Long userId) {

                User user = userRepository.findById(userId)
                                .orElseThrow(() -> new RuntimeException("Khong tim thay nguoi dung"));

                keycloakService.deleteUser(user.getKeycloakUserId());
                userRepository.delete(user);
        }
}
