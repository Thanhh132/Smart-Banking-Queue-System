package com.sbqs.service;

import com.sbqs.config.EmailVerificationProperties;
import com.sbqs.entity.EmailVerificationToken;
import com.sbqs.entity.User;
import com.sbqs.repository.EmailVerificationTokenRepository;
import com.sbqs.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.UUID;

@Service
public class EmailVerificationService {
    private final EmailVerificationTokenRepository tokenRepository;
    private final UserRepository userRepository;
    private final KeycloakService keycloakService;
    private final EmailVerificationProperties properties;
    private final AuthenticationMailService mailService;

    public EmailVerificationService(
            EmailVerificationTokenRepository tokenRepository,
            UserRepository userRepository,
            KeycloakService keycloakService,
            EmailVerificationProperties properties,
            AuthenticationMailService mailService) {
        this.tokenRepository = tokenRepository;
        this.userRepository = userRepository;
        this.keycloakService = keycloakService;
        this.properties = properties;
        this.mailService = mailService;
    }

    @Transactional
    /** Tạo token kích hoạt tài khoản mới và gửi link xác minh qua email. */
    public void sendVerification(User user) {
        tokenRepository.deleteByUser(user);
        String rawToken = UUID.randomUUID() + "." + UUID.randomUUID();
        EmailVerificationToken token = new EmailVerificationToken();
        token.setUser(user);
        token.setTokenHash(hash(rawToken));
        token.setExpiresAt(LocalDateTime.now().plusMinutes(properties.getExpiryMinutes()));
        tokenRepository.save(token);

        String verificationUrl = properties.getFrontendUrl() + "?token=" + rawToken;
        String html = """
                    <h2>Xac minh tai khoan SBQS</h2>
                    <p>Xin chao %s,</p>
                    <p>Vui long bam vao lien ket sau de kich hoat tai khoan:</p>
                    <p><a href="%s">Xac minh email</a></p>
                    <p>Lien ket het han sau %d phut.</p>
                    """.formatted(escapeHtml(user.getFullName()), verificationUrl, properties.getExpiryMinutes());
        mailService.sendHtml(
                properties.getFromEmail(),
                user.getEmail(),
                "SBQS - Xac minh dia chi email",
                html,
                "Email verification");
    }

    @Transactional
    /** Gửi lại link cho tài khoản PENDING, có cooldown để tránh spam email. */
    public void requestVerification(String email) {
        if (email == null || email.isBlank()) {
            return;
        }
        User user = userRepository.findByEmailIgnoreCase(email.trim()).orElse(null);
        if (user == null || !"PENDING".equalsIgnoreCase(user.getStatus())) {
            return;
        }
        boolean requestedRecently = tokenRepository.findFirstByUserOrderByCreatedAtDesc(user)
                .filter(token -> token.getUsedAt() == null)
                .filter(token -> token.getExpiresAt().isAfter(LocalDateTime.now()))
                .map(token -> token.getCreatedAt().isAfter(
                        LocalDateTime.now().minusMinutes(properties.getResendCooldownMinutes())))
                .orElse(false);
        if (!requestedRecently) {
            sendVerification(user);
        }
    }

    @Transactional
    /** Xác minh token rồi kích hoạt đồng thời tài khoản Keycloak và tài khoản nội bộ. */
    public void verify(String rawToken) {
        EmailVerificationToken token = tokenRepository.findByTokenHash(hash(rawToken))
                .orElseThrow(() -> new RuntimeException("Lien ket xac minh email khong hop le"));
        User user = token.getUser();
        if ("ACTIVE".equalsIgnoreCase(user.getStatus())) {
            return;
        }

        if (token.getUsedAt() != null || token.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new RuntimeException("Lien ket xac minh email da het han hoac da duoc su dung");
        }

        keycloakService.verifyUserEmail(user.getKeycloakUserId());
        user.setStatus("ACTIVE");
        userRepository.save(user);
        token.setUsedAt(LocalDateTime.now());
        tokenRepository.save(token);
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
            throw new IllegalStateException("Khong tao duoc email verification token", ex);
        }
    }

    private String escapeHtml(String value) {
        return value == null ? "khach hang" : value
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;");
    }
}
