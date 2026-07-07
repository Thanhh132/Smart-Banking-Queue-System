package com.sbqs.service;

import com.sbqs.entity.AuthenticationAudit;
import com.sbqs.repository.AuthenticationAuditRepository;
import com.sbqs.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.Locale;

@Service
public class AuthenticationAuditService {
    private static final Logger log = LoggerFactory.getLogger(AuthenticationAuditService.class);
    private final AuthenticationAuditRepository auditRepository;
    private final UserRepository userRepository;

    public AuthenticationAuditService(
            AuthenticationAuditRepository auditRepository,
            UserRepository userRepository) {
        this.auditRepository = auditRepository;
        this.userRepository = userRepository;
    }

    /** Lưu dấu vết đăng nhập thành công/thất bại nhưng không bao giờ ghi mật khẩu hoặc token. */
    public void record(String email, boolean successful, String source,
                       String failureReason, String ipAddress, String userAgent) {
        try {
            AuthenticationAudit audit = new AuthenticationAudit();
            audit.setEmail(email == null ? "unknown" : email.trim().toLowerCase(Locale.ROOT));
            userRepository.findByEmailIgnoreCase(email == null ? "" : email)
                    .ifPresent(user -> audit.setUserId(user.getUserId()));
            audit.setSuccessful(successful);
            audit.setAuthenticationSource(source);
            audit.setFailureReason(failureReason);
            audit.setIpAddress(limit(ipAddress, 255));
            audit.setUserAgent(limit(userAgent, 512));
            auditRepository.save(audit);
        } catch (RuntimeException ex) {
            // An audit storage outage must be visible in logs but must not create
            // a false login failure after authentication already succeeded.
            log.error("Could not persist authentication audit email={} success={}", email, successful, ex);
        }
    }

    private String limit(String value, int maxLength) {
        if (value == null || value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, maxLength);
    }
}
