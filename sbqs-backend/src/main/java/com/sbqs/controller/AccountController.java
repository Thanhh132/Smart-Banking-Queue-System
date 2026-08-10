package com.sbqs.controller;

import com.sbqs.dto.AccountProfileResponse;
import com.sbqs.dto.AccountChangeConfirmationResponse;
import com.sbqs.dto.ChangePasswordRequest;
import com.sbqs.dto.CompleteSocialProfileRequest;
import com.sbqs.dto.CustomerPaperlessProfileResponse;
import com.sbqs.dto.UpdateAccountProfileRequest;
import com.sbqs.dto.UpdateCustomerPaperlessProfileRequest;
import com.sbqs.service.AccountChangeService;
import com.sbqs.service.AccountService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/account")
@CrossOrigin("*")
public class AccountController {
    private final AccountService accountService;
    private final AccountChangeService accountChangeService;

    public AccountController(AccountService accountService, AccountChangeService accountChangeService) {
        this.accountService = accountService;
        this.accountChangeService = accountChangeService;
    }

    @GetMapping
    /** API dùng chung cho mọi role để xem thông tin tài khoản của chính mình. */
    public ResponseEntity<AccountProfileResponse> getProfile() {
        return ResponseEntity.ok(accountService.getProfile());
    }

    @GetMapping("/paperless-profile")
    /** Tra ve ho so giay to cua customer va cac truong con thieu theo dich vu duoc chon. */
    public ResponseEntity<CustomerPaperlessProfileResponse> getPaperlessProfile(
            @RequestParam(required = false) Long serviceId) {
        return ResponseEntity.ok(accountService.getPaperlessProfile(serviceId));
    }

    @PutMapping("/paperless-profile")
    /** Luu ho so giay to dung chung cho cac dich vu can dien form online. */
    public ResponseEntity<CustomerPaperlessProfileResponse> updatePaperlessProfile(
            @Valid @RequestBody UpdateCustomerPaperlessProfileRequest request) {
        return ResponseEntity.ok(accountService.updatePaperlessProfile(request));
    }

    @PutMapping("/social-profile")
    public ResponseEntity<AccountProfileResponse> completeSocialProfile(
            @Valid @RequestBody CompleteSocialProfileRequest request) {
        return ResponseEntity.ok(accountService.completeSocialProfile(request));
    }

    @PostMapping("/change-request")
    /** Nhận thông tin CUSTOMER muốn sửa và gửi link xác nhận, chưa cập nhật ngay. */
    public ResponseEntity<Void> requestProfileChange(
            @Valid @RequestBody UpdateAccountProfileRequest request) {
        accountChangeService.requestProfileChange(request);
        return ResponseEntity.accepted().build();
    }

    @PostMapping("/confirm-change")
    /** Endpoint công khai được gọi từ link email; token hợp lệ chính là bằng chứng xác nhận. */
    public ResponseEntity<AccountChangeConfirmationResponse> confirmProfileChange(
            @RequestParam String token) {
        return ResponseEntity.ok(accountChangeService.confirmProfileChange(token));
    }

    @PutMapping("/password")
    /** Đổi mật khẩu khi người dùng đã đăng nhập và nhập đúng mật khẩu hiện tại. */
    public ResponseEntity<Void> changePassword(@Valid @RequestBody ChangePasswordRequest request) {
        accountService.changePassword(request);
        return ResponseEntity.noContent().build();
    }
}
