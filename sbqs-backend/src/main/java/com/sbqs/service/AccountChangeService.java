package com.sbqs.service;

import com.sbqs.config.AccountChangeProperties;
import com.sbqs.dto.AccountChangeConfirmationResponse;
import com.sbqs.dto.UpdateAccountProfileRequest;
import com.sbqs.entity.AccountChangeToken;
import com.sbqs.entity.User;
import com.sbqs.repository.AccountChangeTokenRepository;
import com.sbqs.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.Locale;
import java.util.UUID;

/**
 * Quản lý yêu cầu thay đổi hồ sơ tài khoản cần xác nhận qua email.
 * Token thật không được lưu trong cơ sở dữ liệu; hệ thống chỉ lưu giá trị băm SHA-256.
 */
@Service
public class AccountChangeService {
    private final CurrentUserService currentUserService;
    private final UserRepository userRepository;
    private final KeycloakAdminService keycloakService;
    private final AccountChangeTokenRepository changeTokenRepository;
    private final AuthenticationMailService mailService;
    private final AccountChangeProperties changeProperties;

    public AccountChangeService(
            CurrentUserService currentUserService,
            UserRepository userRepository,
            KeycloakAdminService keycloakService,
            AccountChangeTokenRepository changeTokenRepository,
            AuthenticationMailService mailService,
            AccountChangeProperties changeProperties) {
        this.currentUserService = currentUserService;
        this.userRepository = userRepository;
        this.keycloakService = keycloakService;
        this.changeTokenRepository = changeTokenRepository;
        this.mailService = mailService;
        this.changeProperties = changeProperties;
    }

    @Transactional
    /** Tạo yêu cầu thay đổi và gửi liên kết xác nhận về email hiện tại. */
    public void requestProfileChange(UpdateAccountProfileRequest request) {
        User user = currentUserService.requireUser();
        if (!"CUSTOMER".equals(user.getRole())) {
            throw new RuntimeException("Thong tin nhan su do cong ty quan ly va khong the tu chinh sua");
        }

        String fullName = request.fullName().trim();
        String email = request.email().trim().toLowerCase(Locale.ROOT);
        String phone = request.phone().trim();
        ensureUniqueContact(user, email, phone);
        if (fullName.equals(user.getFullName())
                && email.equalsIgnoreCase(user.getEmail())
                && phone.equals(user.getPhone())) {
            throw new RuntimeException("Thong tin moi khong co thay doi");
        }

        changeTokenRepository.deleteByUser(user);
        String rawToken = newToken();
        AccountChangeToken change = new AccountChangeToken();
        change.setUser(user);
        change.setPendingFullName(fullName);
        change.setPendingEmail(email);
        change.setPendingPhone(phone);
        change.setCurrentEmailTokenHash(hash(rawToken));
        change.setExpiresAt(LocalDateTime.now().plusMinutes(changeProperties.getExpiryMinutes()));
        changeTokenRepository.save(change);

        sendConfirmationMail(
                user.getEmail(), rawToken, "Xac nhan thay doi thong tin tai khoan",
                "Bam vao lien ket de cho phep thay doi thong tin tai khoan SBQS.");
    }

    @Transactional
    /** Xác nhận quyền sở hữu email trước khi đồng bộ dữ liệu sang Keycloak và database. */
    public AccountChangeConfirmationResponse confirmProfileChange(String rawToken) {
        String tokenHash = hash(rawToken);
        AccountChangeToken change = changeTokenRepository.findByCurrentEmailTokenHash(tokenHash)
                .orElseGet(() -> changeTokenRepository.findByNewEmailTokenHash(tokenHash)
                        .orElseThrow(() -> new RuntimeException("Lien ket xac nhan khong hop le")));

        if (change.getAppliedAt() != null) {
            return new AccountChangeConfirmationResponse(
                    "APPLIED", "Thong tin tai khoan da duoc cap nhat truoc do.",
                    change.getNewEmailTokenHash() != null);
        }
        if (change.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new RuntimeException("Lien ket xac nhan da het han. Vui long tao yeu cau moi");
        }

        boolean currentEmailToken = tokenHash.equals(change.getCurrentEmailTokenHash());
        if (currentEmailToken) {
            change.setCurrentEmailConfirmedAt(LocalDateTime.now());
            boolean changingEmail = !change.getPendingEmail().equalsIgnoreCase(change.getUser().getEmail());
            if (changingEmail) {
                sendNewEmailConfirmationIfNeeded(change);
                return new AccountChangeConfirmationResponse(
                        "PENDING_NEW_EMAIL",
                        "Email hien tai da xac nhan. Hay kiem tra email moi de hoan tat.", false);
            }
        } else {
            if (change.getCurrentEmailConfirmedAt() == null) {
                throw new RuntimeException("Can xac nhan tai email hien tai truoc");
            }
            change.setNewEmailConfirmedAt(LocalDateTime.now());
        }

        return applyProfileChange(change);
    }

    private void sendNewEmailConfirmationIfNeeded(AccountChangeToken change) {
        if (change.getNewEmailTokenHash() != null) {
            return;
        }
        String rawToken = newToken();
        change.setNewEmailTokenHash(hash(rawToken));
        change.setExpiresAt(LocalDateTime.now().plusMinutes(changeProperties.getExpiryMinutes()));
        changeTokenRepository.save(change);
        sendConfirmationMail(
                change.getPendingEmail(), rawToken, "Xac minh email moi",
                "Bam vao lien ket de xac minh email moi cho tai khoan SBQS.");
    }

    private AccountChangeConfirmationResponse applyProfileChange(AccountChangeToken change) {
        User user = change.getUser();
        boolean emailChanged = !change.getPendingEmail().equalsIgnoreCase(user.getEmail());
        ensureUniqueContact(user, change.getPendingEmail(), change.getPendingPhone());

        keycloakService.updateUserProfile(
                user.getKeycloakUserId(), change.getPendingFullName(), change.getPendingEmail(), user.getRole());
        user.setFullName(change.getPendingFullName());
        user.setEmail(change.getPendingEmail());
        user.setPhone(change.getPendingPhone());
        userRepository.save(user);
        change.setAppliedAt(LocalDateTime.now());
        changeTokenRepository.save(change);
        return new AccountChangeConfirmationResponse(
                "APPLIED", "Thong tin tai khoan da duoc cap nhat.", emailChanged);
    }

    private void ensureUniqueContact(User user, String email, String phone) {
        if (userRepository.existsByPhoneAndUserIdNot(phone, user.getUserId())) {
            throw new RuntimeException("So dien thoai da duoc tai khoan khac su dung");
        }
        if (userRepository.existsByEmailIgnoreCaseAndUserIdNot(email, user.getUserId())) {
            throw new RuntimeException("Email da duoc tai khoan khac su dung");
        }
    }

    private void sendConfirmationMail(String email, String rawToken, String subject, String instruction) {
        String url = changeProperties.getFrontendUrl() + "?token=" + rawToken;
        String html = """
                <h2>%s</h2>
                <p>%s</p>
                <p><a href="%s">Xac nhan thay doi</a></p>
                <p>Lien ket het han sau %d phut. Neu ban khong yeu cau, hay bo qua email nay.</p>
                """.formatted(subject, instruction, url, changeProperties.getExpiryMinutes());
        mailService.sendHtml(
                changeProperties.getFromEmail(), email, "SBQS - " + subject, html, "Account change");
    }

    private String newToken() {
        return UUID.randomUUID() + "." + UUID.randomUUID();
    }

    private String hash(String value) {
        if (value == null || value.isBlank()) {
            return "";
        }
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (Exception ex) {
            throw new IllegalStateException("Khong tao duoc token xac nhan tai khoan", ex);
        }
    }
}
