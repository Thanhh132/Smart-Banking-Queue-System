package com.sbqs.service;

import com.sbqs.dto.CreateStaffRequest;
import com.sbqs.entity.Branch;
import com.sbqs.entity.User;
import com.sbqs.repository.BranchRepository;
import com.sbqs.repository.UserRepository;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import com.sbqs.dto.UpdateUserRequest;

import java.util.List;

@Service
public class UserService {

        private final UserRepository userRepository;
        private final BranchRepository branchRepository;

        private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

        public List<User> getUsersByBranch(Long branchId) {

                Branch branch = branchRepository.findById(branchId)
                                .orElseThrow(() -> new RuntimeException("Không tìm thấy chi nhánh"));

                return userRepository.findByBranch(branch);
        }

        public UserService(
                        UserRepository userRepository,
                        BranchRepository branchRepository) {

                this.userRepository = userRepository;
                this.branchRepository = branchRepository;
        }

        public List<User> getAllUsers() {
                return userRepository.findAll();
        }

        public User createStaff(CreateStaffRequest request) {
                if (userRepository.existsByEmail(request.getEmail())) {
                        throw new RuntimeException("Email đã tồn tại");
                }

                if (userRepository.existsByPhone(request.getPhone())) {
                        throw new RuntimeException("Số điện thoại đã tồn tại");
                }

                Branch branch = branchRepository.findById(request.getBranchId())
                                .orElseThrow(() -> new RuntimeException("Không tìm thấy chi nhánh"));

                User user = new User();

                user.setFullName(request.getFullName());
                user.setEmail(request.getEmail());
                user.setPhone(request.getPhone());
                user.setRole("STAFF");
                user.setStatus("ACTIVE");
                user.setBranch(branch);
                user.setPasswordHash(passwordEncoder.encode(request.getPassword()));

                return userRepository.save(user);
        }

        public User updateUser(Long userId, UpdateUserRequest request) {

                User user = userRepository.findById(userId)
                                .orElseThrow(() -> new RuntimeException("Không tìm thấy người dùng"));

                if (userRepository.existsByEmailAndUserIdNot(request.getEmail(), userId)) {
                        throw new RuntimeException("Email đã tồn tại");
                }

                if (userRepository.existsByPhoneAndUserIdNot(request.getPhone(), userId)) {
                        throw new RuntimeException("Số điện thoại đã tồn tại");
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
                                .orElseThrow(() -> new RuntimeException("Không tìm thấy người dùng"));

                userRepository.delete(user);
        }

}