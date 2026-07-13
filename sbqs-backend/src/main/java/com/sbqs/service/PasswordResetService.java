package com.sbqs.service;

import com.sbqs.config.PasswordResetProperties;
import com.sbqs.entity.PasswordResetToken;
import com.sbqs.entity.User;
import com.sbqs.repository.PasswordResetTokenRepository;
import com.sbqs.repository.UserRepository;
import com.sbqs.util.PasswordPolicy;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.crypto.password.PasswordEncoder;

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
    private final KeycloakAdminService keycloakService;
    private final PasswordResetProperties properties;
    private final ObjectProvider<JavaMailSender> mailSenderProvider;
    private final PasswordEncoder passwordEncoder;

    public PasswordResetService(
            UserRepository userRepository,
            PasswordResetTokenRepository tokenRepository,
            KeycloakAdminService keycloakService,
            PasswordResetProperties properties,
            ObjectProvider<JavaMailSender> mailSenderProvider,
            PasswordEncoder passwordEncoder) {

        this.userRepository = userRepository;
        this.tokenRepository = tokenRepository;
        this.keycloakService = keycloakService;
        this.properties = properties;
        this.mailSenderProvider = mailSenderProvider;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    /** Tạo token reset một lần cho tài khoản ACTIVE và áp dụng cooldown để chống spam mail. */
    public void requestReset(String email) {
        if (email == null || email.isBlank()) {
            return;
        }

        User user = userRepository.findByEmailIgnoreCase(email.trim()).orElse(null);
        if (user == null || !"ACTIVE".equalsIgnoreCase(user.getStatus())) {
            return;
        }

        boolean requestedRecently = tokenRepository.findFirstByUserOrderByCreatedAtDesc(user)
                .map(token -> token.getCreatedAt().isAfter(
                        LocalDateTime.now().minusMinutes(properties.getCooldownMinutes())))
                .orElse(false);
        if (requestedRecently) {
            log.info(
                    "Password reset request skipped because email={} requested within {} minutes",
                    user.getEmail(),
                    properties.getCooldownMinutes());
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

        String resetUrl = properties.getFrontendUrl() + "?token=" + rawToken;

        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, "UTF-8");
            helper.setFrom(properties.getFromEmail());
            helper.setTo(user.getEmail());
            helper.setSubject("SBQS - Đặt lại mật khẩu");
            helper.setText(buildPasswordResetEmail(user, resetUrl), true);
            mailSender.send(message);
        } catch (MessagingException | RuntimeException ex) {
            log.error("Password reset email could not be sent", ex);
        }
    }

    private String buildPasswordResetEmail(User user, String resetUrl) {
        String escapedName = escapeHtml(user.getFullName() == null ? "khách hàng" : user.getFullName());
        String escapedResetUrl = escapeHtml(resetUrl);

        return """
                <!doctype html>
                <html>
                <body style="margin:0;padding:0;background:#f4f7fb;font-family:Arial,Helvetica,sans-serif;color:#0f172a;">
                  <table role="presentation" width="100%%" cellspacing="0" cellpadding="0" style="background:#f4f7fb;padding:28px 0;">
                    <tr>
                      <td align="center">
                        <table role="presentation" width="100%%" cellspacing="0" cellpadding="0" style="max-width:560px;background:#ffffff;border:1px solid #d8e2ef;border-radius:10px;overflow:hidden;">
                          <tr>
                            <td style="padding:24px 28px;background:#005baa;color:#ffffff;">
                              <div style="font-size:22px;font-weight:800;letter-spacing:.3px;">SBQS</div>
                              <div style="font-size:13px;opacity:.9;margin-top:4px;">Smart Banking Queue System</div>
                            </td>
                          </tr>
                          <tr>
                            <td style="padding:28px;">
                              <h1 style="margin:0 0 12px;font-size:24px;line-height:1.25;color:#0f172a;">Đặt lại mật khẩu</h1>
                              <p style="margin:0 0 16px;font-size:15px;line-height:1.6;color:#475569;">Xin chào %s,</p>
                              <p style="margin:0 0 22px;font-size:15px;line-height:1.6;color:#475569;">
                                SBQS nhận được yêu cầu đặt lại mật khẩu cho tài khoản của bạn. Bấm nút bên dưới để tạo mật khẩu mới.
                              </p>
                              <p style="margin:0 0 24px;">
                                <a href="%s" style="display:inline-block;background:#005baa;color:#ffffff;text-decoration:none;padding:13px 20px;border-radius:8px;font-size:15px;font-weight:800;">
                                  Đặt lại mật khẩu
                                </a>
                              </p>
                              <p style="margin:0 0 18px;font-size:14px;line-height:1.6;color:#64748b;">
                                Liên kết này hết hạn sau %d phút. Nếu bạn không yêu cầu đặt lại mật khẩu, hãy bỏ qua email này.
                              </p>
                            </td>
                          </tr>
                        </table>
                      </td>
                    </tr>
                  </table>
                </body>
                </html>
                """.formatted(
                escapedName,
                escapedResetUrl,
                properties.getExpiryMinutes());
    }

    private String escapeHtml(String value) {
        return value
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;");
    }

    @Transactional
    /** Kiểm tra hạn/token đã dùng rồi đồng bộ mật khẩu mới sang Keycloak và BCrypt fallback. */
    public void resetPassword(String rawToken, String newPassword) {
        validatePassword(newPassword);

        PasswordResetToken token = tokenRepository.findByTokenHash(hash(rawToken))
                .orElseThrow(() -> new RuntimeException("Lien ket dat lai mat khau khong hop le"));

        if (token.getUsedAt() != null || token.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new RuntimeException("Lien ket dat lai mat khau da het han hoac da duoc su dung");
        }

        keycloakService.resetUserPassword(token.getUser().getKeycloakUserId(), newPassword);
        token.getUser().setPasswordHash(passwordEncoder.encode(newPassword));
        userRepository.save(token.getUser());
        token.setUsedAt(LocalDateTime.now());
        tokenRepository.save(token);
    }

    public void resetPassword(String rawToken, String newPassword, String confirmPassword) {
        if (confirmPassword == null || !confirmPassword.equals(newPassword)) {
            throw new RuntimeException("Mat khau xac nhan khong khop");
        }
        resetPassword(rawToken, newPassword);
    }

    private void validatePassword(String password) {
        PasswordPolicy.validate(password);
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
