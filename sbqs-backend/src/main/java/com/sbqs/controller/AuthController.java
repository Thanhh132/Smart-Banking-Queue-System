package com.sbqs.controller;

import com.sbqs.dto.LoginRequest;
import com.sbqs.dto.RefreshTokenRequest;
import com.sbqs.dto.RegisterRequest;
import com.sbqs.dto.ForgotPasswordRequest;
import com.sbqs.dto.ResetPasswordRequest;
import com.sbqs.entity.User;
import com.sbqs.service.AuthService;
import com.sbqs.service.AuthenticationAuditService;
import com.sbqs.service.CustomerRegistrationService;
import com.sbqs.service.EmailVerificationService;
import com.sbqs.service.LoginRateLimitService;
import com.sbqs.service.PasswordResetService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;
import com.sbqs.dto.LoginResponse;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Locale;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin("*")
public class AuthController {

    private final AuthService authService;
    private final CustomerRegistrationService registrationService;
    private final PasswordResetService passwordResetService;
    private final LoginRateLimitService loginRateLimitService;
    private final AuthenticationAuditService authenticationAuditService;
    private final EmailVerificationService emailVerificationService;

    public AuthController(
            AuthService authService,
            CustomerRegistrationService registrationService,
            PasswordResetService passwordResetService,
            LoginRateLimitService loginRateLimitService,
            AuthenticationAuditService authenticationAuditService,
            EmailVerificationService emailVerificationService) {

        this.authService = authService;
        this.registrationService = registrationService;
        this.passwordResetService = passwordResetService;
        this.loginRateLimitService = loginRateLimitService;
        this.authenticationAuditService = authenticationAuditService;
        this.emailVerificationService = emailVerificationService;
    }

    @PostMapping("/register")
    public ResponseEntity<User> register(
            @Valid @RequestBody RegisterRequest request) {

        return ResponseEntity.ok(
                registrationService.register(request));
    }

    /**
     * Điều phối toàn bộ lớp bảo vệ đăng nhập: rate limit theo email/IP, gọi cơ chế
     * xác thực chính hoặc fallback và ghi audit cho cả trường hợp thành công lẫn thất bại.
     */
    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(
            @Valid @RequestBody LoginRequest request,
            HttpServletRequest httpRequest) {

        String email = request.getEmail().trim().toLowerCase(Locale.ROOT);
        String ipAddress = resolveClientIp(httpRequest);
        String userAgent = httpRequest.getHeader("User-Agent");
        try {
            loginRateLimitService.checkAllowed(email, ipAddress);
            LoginResponse response = authService.login(request);
            loginRateLimitService.recordSuccess(email, ipAddress);
            authenticationAuditService.record(
                    email, true, response.getAuthenticationSource(), null, ipAddress, userAgent);
            return ResponseEntity.ok(response);
        } catch (com.sbqs.exception.LoginRateLimitExceededException ex) {
            authenticationAuditService.record(
                    email, false, null, "RATE_LIMITED", ipAddress, userAgent);
            throw ex;
        } catch (RuntimeException ex) {
            loginRateLimitService.recordFailure(email, ipAddress);
            authenticationAuditService.record(
                    email, false, null, "AUTHENTICATION_FAILED", ipAddress, userAgent);
            throw ex;
        }
    }

    @PostMapping("/refresh")
    public ResponseEntity<LoginResponse> refresh(
            @RequestBody RefreshTokenRequest request) {

        return ResponseEntity.ok(
                authService.refresh(request.getRefreshToken()));
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(
            @RequestBody RefreshTokenRequest request) {

        authService.logout(request.getRefreshToken());
        return ResponseEntity.noContent().build();
    }

    /** Luôn trả ACCEPTED để không làm lộ email nào đang tồn tại trong hệ thống. */
    @PostMapping("/forgot-password")
    public ResponseEntity<Void> forgotPassword(
            @RequestBody ForgotPasswordRequest request) {

        passwordResetService.requestReset(request.getEmail());
        return ResponseEntity.accepted().build();
    }

    @PostMapping("/reset-password")
    public ResponseEntity<Void> resetPassword(
            @RequestBody ResetPasswordRequest request) {

        passwordResetService.resetPassword(
                request.getToken(),
                request.getNewPassword(),
                request.getConfirmPassword());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/verify-email")
    public ResponseEntity<Void> verifyEmail(@RequestParam String token) {
        emailVerificationService.verify(token);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/resend-verification")
    public ResponseEntity<Void> resendVerification(@RequestBody ForgotPasswordRequest request) {
        emailVerificationService.requestVerification(request.getEmail());
        return ResponseEntity.accepted().build();
    }

    private String resolveClientIp(HttpServletRequest request) {
        // Do not trust X-Forwarded-For until a trusted reverse proxy is configured.
        return request.getRemoteAddr();
    }
}
