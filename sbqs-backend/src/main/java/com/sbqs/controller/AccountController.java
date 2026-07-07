package com.sbqs.controller;

import com.sbqs.dto.AccountProfileResponse;
import com.sbqs.dto.AccountChangeConfirmationResponse;
import com.sbqs.dto.ChangePasswordRequest;
import com.sbqs.dto.UpdateAccountProfileRequest;
import com.sbqs.service.AccountService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/account")
@CrossOrigin("*")
public class AccountController {
    private final AccountService accountService;

    public AccountController(AccountService accountService) {
        this.accountService = accountService;
    }

    @GetMapping
    /** API dùng chung cho mọi role để xem thông tin tài khoản của chính mình. */
    public ResponseEntity<AccountProfileResponse> getProfile() {
        return ResponseEntity.ok(accountService.getProfile());
    }

    @PostMapping("/change-request")
    /** Nhận thông tin CUSTOMER muốn sửa và gửi link xác nhận, chưa cập nhật ngay. */
    public ResponseEntity<Void> requestProfileChange(
            @Valid @RequestBody UpdateAccountProfileRequest request) {
        accountService.requestProfileChange(request);
        return ResponseEntity.accepted().build();
    }

    @PostMapping("/confirm-change")
    /** Endpoint công khai được gọi từ link email; token hợp lệ chính là bằng chứng xác nhận. */
    public ResponseEntity<AccountChangeConfirmationResponse> confirmProfileChange(
            @RequestParam String token) {
        return ResponseEntity.ok(accountService.confirmProfileChange(token));
    }

    @PutMapping("/password")
    /** Đổi mật khẩu khi người dùng đã đăng nhập và nhập đúng mật khẩu hiện tại. */
    public ResponseEntity<Void> changePassword(@Valid @RequestBody ChangePasswordRequest request) {
        accountService.changePassword(request);
        return ResponseEntity.noContent().build();
    }
}
