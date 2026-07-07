package com.sbqs.service;

import com.sbqs.exception.LoginRateLimitExceededException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.Locale;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class LoginRateLimitService {
    private final ConcurrentHashMap<String, AttemptState> attempts = new ConcurrentHashMap<>();
    private final int maxFailures;
    private final Duration window;
    private final Duration blockDuration;

    public LoginRateLimitService(
            @Value("${sbqs.auth.rate-limit.max-failures:5}") int maxFailures,
            @Value("${sbqs.auth.rate-limit.window-minutes:15}") long windowMinutes,
            @Value("${sbqs.auth.rate-limit.block-minutes:15}") long blockMinutes) {
        this.maxFailures = maxFailures;
        this.window = Duration.ofMinutes(windowMinutes);
        this.blockDuration = Duration.ofMinutes(blockMinutes);
    }

    /** Kiểm tra đồng thời khóa theo email và IP trước khi cho phép thử đăng nhập. */
    public void checkAllowed(String email, String ipAddress) {
        checkKey(emailKey(email));
        checkKey(ipKey(ipAddress));
    }

    /** Ghi nhận thất bại; đủ số lần trong cửa sổ thời gian sẽ khóa tạm email và IP. */
    public void recordFailure(String email, String ipAddress) {
        recordFailure(emailKey(email));
        recordFailure(ipKey(ipAddress));
    }

    /** Xóa bộ đếm sau khi đăng nhập thành công để không khóa nhầm người dùng hợp lệ. */
    public void recordSuccess(String email, String ipAddress) {
        attempts.remove(emailKey(email));
        attempts.remove(ipKey(ipAddress));
    }

    private void checkKey(String key) {
        AttemptState state = attempts.get(key);
        if (state != null && state.blockedUntil() != null && state.blockedUntil().isAfter(Instant.now())) {
            throw new LoginRateLimitExceededException(
                    "Qua nhieu lan dang nhap that bai. Vui long thu lai sau.");
        }
    }

    private void recordFailure(String key) {
        attempts.compute(key, (ignored, current) -> {
            Instant now = Instant.now();
            int failures = current == null || current.windowStartedAt().plus(window).isBefore(now)
                    ? 1 : current.failures() + 1;
            Instant startedAt = current == null || current.windowStartedAt().plus(window).isBefore(now)
                    ? now : current.windowStartedAt();
            Instant blockedUntil = failures >= maxFailures ? now.plus(blockDuration) : null;
            return new AttemptState(failures, startedAt, blockedUntil);
        });
    }

    private String emailKey(String email) {
        return "email:" + (email == null ? "" : email.trim().toLowerCase(Locale.ROOT));
    }

    private String ipKey(String ipAddress) {
        return "ip:" + (ipAddress == null || ipAddress.isBlank() ? "unknown" : ipAddress);
    }

    private record AttemptState(int failures, Instant windowStartedAt, Instant blockedUntil) {
    }
}
