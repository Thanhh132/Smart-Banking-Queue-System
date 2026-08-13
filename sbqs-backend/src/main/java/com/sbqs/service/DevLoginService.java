package com.sbqs.service;

import com.sbqs.dto.DevLoginAccountResponse;
import com.sbqs.dto.LoginResponse;
import com.sbqs.entity.User;
import com.sbqs.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@Service
public class DevLoginService {
    private static final Set<String> TESTABLE_ROLES = Set.of(
            "SUPER_ADMIN", "BRANCH_ADMIN", "STAFF", "CUSTOMER");
    private static final Logger log = LoggerFactory.getLogger(DevLoginService.class);
    private final UserRepository userRepository;
    private final FallbackTokenService tokenService;

    public DevLoginService(UserRepository userRepository, FallbackTokenService tokenService) {
        this.userRepository = userRepository;
        this.tokenService = tokenService;
    }

    @Transactional(readOnly = true)
    public List<DevLoginAccountResponse> accounts() {
        return userRepository.findByStatusNotIgnoreCase("DELETED").stream()
                .filter(this::isTestableAccount)
                .sorted(Comparator
                        .comparing(User::getRole, Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER))
                        .thenComparing(User::getFullName, Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER)))
                .map(DevLoginAccountResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public LoginResponse login(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Tai khoan khong con ton tai"));
        if (!isTestableAccount(user)) {
            throw new RuntimeException("Tai khoan khong kha dung cho Dev Login");
        }
        log.warn("DEV_QUICK_LOGIN issued for userId={} role={}", user.getUserId(), user.getRole());
        return new LoginResponse(
                tokenService.issueDevelopment(user),
                null,
                "Bearer",
                tokenService.expiresInSeconds(),
                user.getRole(),
                user.getFullName(),
                user.getEmail(),
                user.getBranch() == null ? null : user.getBranch().getBranchId(),
                "DEV_QUICK_LOGIN",
                !"CUSTOMER".equals(user.getRole()) || CustomerProfilePolicy.isComplete(user));
    }

    private boolean isTestableAccount(User user) {
        return "ACTIVE".equalsIgnoreCase(user.getStatus())
                && user.getEmail() != null
                && !user.getEmail().isBlank()
                && user.getRole() != null
                && TESTABLE_ROLES.contains(user.getRole().toUpperCase(Locale.ROOT));
    }
}
