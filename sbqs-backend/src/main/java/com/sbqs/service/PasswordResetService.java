package com.sbqs.service;

import com.sbqs.config.PasswordResetProperties;
import com.sbqs.entity.PasswordResetToken;
import com.sbqs.entity.User;
import com.sbqs.repository.PasswordResetTokenRepository;
import com.sbqs.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.UUID;

@Service
public class PasswordResetService {
    private static final Logger log = LoggerFactory.getLogger(PasswordResetService.class);

    private final UserRepository userRepository;
    private final PasswordResetTokenRepository tokenRepository;
    private final KeycloakService keycloakService;
    private final PasswordResetProperties properties;
    private final ObjectProvider<JavaMailSender> mailSenderProvider;

    public PasswordResetService(
            UserRepository userRepository,
            PasswordResetTokenRepository tokenRepository,
            KeycloakService keycloakService,
            PasswordResetProperties properties,
            ObjectProvider<JavaMailSender> mailSenderProvider) {

        this.userRepository = userRepository;
        this.tokenRepository = tokenRepository;
        this.keycloakService = keycloakService;
        this.properties = properties;
        this.mailSenderProvider = mailSenderProvider;
    }

    @Transactional
    public void requestReset(String email) {
        if (email == null || email.isBlank()) {
            return;
        }

        User user = userRepository.findByEmailIgnoreCase(email.trim()).orElse(null);
        if (user == null || !"ACTIVE".equalsIgnoreCase(user.getStatus())) {
            return;
        }

        boolean requestedRecently = tokenRepository.findFirstByUserOrderByCreatedAtDesc(user)
                .map(token -> token.getCreatedAt().isAfter(LocalDateTime.now().minusMinutes(1)))
                .orElse(false);
        if (requestedRecently) {
            return;
        }

        tokenRepository.deleteByUser(user);

        String rawToken = UUID.randomUUID() + "." + UUID.randomUUID();
        PasswordResetToken token = new PasswordResetToken();
        token.setUser(user);
        token.setTokenHash(hash(rawToken));
        token.setExpiresAt(LocalDateTime.now().plusMinutes(properties.getExpiryMinutes()));
        tokenRepository.save(token);

        JavaMailSender mailSender = mailSenderProvider.getIfAvailable();
        if (mailSender == null) {
            log.error("Password reset email was not sent because SMTP is not configured");
            return;
        }

        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(properties.getFromEmail());
        message.setTo(user.getEmail());
        message.setSubject("SBQS - Dat lai mat khau");
        message.setText("Mo lien ket sau de dat lai mat khau. Lien ket het han sau "
                + properties.getExpiryMinutes() + " phut:\n\n"
                + properties.getFrontendUrl() + "?token=" + rawToken);
        try {
            mailSender.send(message);
        } catch (RuntimeException ex) {
            log.error("Password reset email could not be sent", ex);
        }
    }

    @Transactional
    public void resetPassword(String rawToken, String newPassword) {
        validatePassword(newPassword);

        PasswordResetToken token = tokenRepository.findByTokenHash(hash(rawToken))
                .orElseThrow(() -> new RuntimeException("Lien ket dat lai mat khau khong hop le"));

        if (token.getUsedAt() != null || token.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new RuntimeException("Lien ket dat lai mat khau da het han hoac da duoc su dung");
        }

        keycloakService.resetUserPassword(token.getUser().getKeycloakUserId(), newPassword);
        token.setUsedAt(LocalDateTime.now());
        tokenRepository.save(token);
    }

    private void validatePassword(String password) {
        if (password == null || password.length() < 8
                || password.chars().noneMatch(Character::isUpperCase)
                || password.chars().noneMatch(Character::isLowerCase)
                || password.chars().noneMatch(Character::isDigit)) {
            throw new RuntimeException("Mat khau phai co it nhat 8 ky tu, gom chu hoa, chu thuong va chu so");
        }
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
            throw new IllegalStateException("Khong tao duoc password reset token", ex);
        }
    }
}
