package com.sbqs.service;

import com.sbqs.entity.User;
import com.sbqs.repository.UserRepository;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;

@Service
public class CurrentUserService {
    private final UserRepository userRepository;

    public CurrentUserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    /** Đọc email từ JWT đã xác minh rồi ánh xạ sang user nghiệp vụ trong PostgreSQL. */
    public User requireUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof Jwt jwt)) {
            throw new RuntimeException("Khong xac dinh duoc tai khoan dang dang nhap");
        }

        String email = jwt.getClaimAsString("email");
        if (email == null || email.isBlank()) {
            email = jwt.getClaimAsString("preferred_username");
        }

        return userRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new RuntimeException("Khong tim thay tai khoan dang dang nhap"));
    }

    public Long requireBranchId() {
        User user = requireUser();
        if (user.getBranch() == null) {
            throw new RuntimeException("Tai khoan chua duoc gan chi nhanh");
        }
        return user.getBranch().getBranchId();
    }

    /** Chặn truy cập chéo chi nhánh đối với role chỉ được vận hành một chi nhánh. */
    public void requireBranch(Long branchId) {
        if (branchId == null || !requireBranchId().equals(branchId)) {
            throw new RuntimeException("Ban khong co quyen truy cap du lieu cua chi nhanh nay");
        }
    }
}
