package com.sbqs.service;

import com.sbqs.dto.RegisterRequest;
import com.sbqs.entity.User;
import com.sbqs.repository.UserRepository;
import com.sbqs.util.PasswordPolicy;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;

/**
 * Xử lý riêng nghiệp vụ đăng ký khách hàng mới và gửi email kích hoạt.
 * Tài khoản chỉ được mở khóa sau khi khách hàng xác minh email.
 */
@Service
public class CustomerRegistrationService {
    private final UserRepository userRepository;
    private final KeycloakAdminService keycloakAdminService;
    private final PasswordEncoder passwordEncoder;
    private final EmailVerificationService emailVerificationService;

    public CustomerRegistrationService(
            UserRepository userRepository,
            KeycloakAdminService keycloakAdminService,
            PasswordEncoder passwordEncoder,
            EmailVerificationService emailVerificationService) {
        this.userRepository = userRepository;
        this.keycloakAdminService = keycloakAdminService;
        this.passwordEncoder = passwordEncoder;
        this.emailVerificationService = emailVerificationService;
    }

    @Transactional
    /** Tạo tài khoản PENDING đồng thời trên Keycloak và cơ sở dữ liệu nghiệp vụ. */
    public User register(RegisterRequest request) {
        String email = request.getEmail().trim().toLowerCase(Locale.ROOT);
        if (userRepository.existsByEmailIgnoreCase(email)) {
            throw new RuntimeException("Email đã tồn tại. Vui lòng sử dụng email khác");
        }
        if (userRepository.existsByPhone(request.getPhone())) {
            throw new RuntimeException("Số điện thoại đã tồn tại. Vui lòng sử dụng số khác");
        }
        if (!request.getPassword().equals(request.getConfirmPassword())) {
            throw new RuntimeException("Mat khau xac nhan khong khop");
        }
        PasswordPolicy.validate(request.getPassword());

        String keycloakUserId = keycloakAdminService.createUser(
                request.getFullName(), email, request.getPassword(), "CUSTOMER", false, false);

        User user = new User();
        user.setFullName(request.getFullName());
        user.setEmail(email);
        user.setPhone(request.getPhone());
        user.setRole("CUSTOMER");
        user.setStatus("PENDING");
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        user.setKeycloakUserId(keycloakUserId);

        User savedUser = userRepository.save(user);
        emailVerificationService.sendVerification(savedUser);
        return savedUser;
    }
}
