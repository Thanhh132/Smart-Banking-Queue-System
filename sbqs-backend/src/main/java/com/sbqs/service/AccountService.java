package com.sbqs.service;

import com.sbqs.dto.AccountProfileResponse;
import com.sbqs.dto.ChangePasswordRequest;
import com.sbqs.dto.CompleteSocialProfileRequest;
import com.sbqs.dto.CustomerPaperlessProfileResponse;
import com.sbqs.dto.CustomerProfileFieldResponse;
import com.sbqs.dto.UpdateCustomerPaperlessProfileRequest;
import com.sbqs.entity.Services;
import com.sbqs.entity.CustomerProfile;
import com.sbqs.entity.User;
import com.sbqs.repository.ServiceRepository;
import com.sbqs.repository.UserRepository;
import com.sbqs.util.PasswordPolicy;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
public class AccountService {
    private final CurrentUserService currentUserService;
    private final UserRepository userRepository;
    private final KeycloakService keycloakService;
    private final KeycloakAdminService keycloakAdminService;
    private final PasswordEncoder passwordEncoder;
    private final ServiceRepository serviceRepository;
    private final CustomerProfileService customerProfileService;

    private static final Map<String, CustomerProfileFieldResponse> PAPERLESS_FIELDS = Map.ofEntries(
            Map.entry("FULL_NAME", new CustomerProfileFieldResponse(
                    "FULL_NAME", "Họ và tên", "text", "Ví dụ: NGUYEN VAN A", true)),
            Map.entry("DATE_OF_BIRTH", new CustomerProfileFieldResponse(
                    "DATE_OF_BIRTH", "Ngày sinh", "date", "Định dạng dd/mm/yyyy", true)),
            Map.entry("GENDER", new CustomerProfileFieldResponse(
                    "GENDER", "Giới tính", "text", "Nam / Nữ / Khác", true)),
            Map.entry("NATIONALITY", new CustomerProfileFieldResponse(
                    "NATIONALITY", "Quốc tịch", "text", "Mặc định: Việt Nam", true)),
            Map.entry("IDENTITY_NUMBER", new CustomerProfileFieldResponse(
                    "IDENTITY_NUMBER", "Số CCCD/CMND", "text", "Gồm 12 số", true)),
            Map.entry("IDENTITY_ISSUE_DATE", new CustomerProfileFieldResponse(
                    "IDENTITY_ISSUE_DATE", "Ngày cấp CCCD", "date", "Định dạng dd/mm/yyyy", true)),
            Map.entry("IDENTITY_ISSUE_PLACE", new CustomerProfileFieldResponse(
                    "IDENTITY_ISSUE_PLACE", "Nơi cấp CCCD", "text", "Cơ quan cấp giấy tờ", true)),
            Map.entry("PASSPORT_NUMBER", new CustomerProfileFieldResponse(
                    "PASSPORT_NUMBER", "Số hộ chiếu", "text", "Dành cho khách hàng nước ngoài", true)),
            Map.entry("VISA_NUMBER", new CustomerProfileFieldResponse(
                    "VISA_NUMBER", "Số thị thực", "text", "Dành cho khách hàng nước ngoài nếu có", true)),
            Map.entry("MOBILE_PHONE", new CustomerProfileFieldResponse(
                    "MOBILE_PHONE", "Số điện thoại di động", "text", "Số dùng nhận OTP/SMS Banking", true)),
            Map.entry("EMAIL_ADDRESS", new CustomerProfileFieldResponse(
                    "EMAIL_ADDRESS", "Địa chỉ email", "text", "Email nhận thông báo hoặc sao kê", true)),
            Map.entry("PERMANENT_ADDRESS", new CustomerProfileFieldResponse(
                    "PERMANENT_ADDRESS", "Địa chỉ thường trú", "textarea", "Địa chỉ ghi trên CCCD", true)),
            Map.entry("CONTACT_ADDRESS", new CustomerProfileFieldResponse(
                    "CONTACT_ADDRESS", "Địa chỉ cư trú hiện tại", "textarea", "Nơi đang ở thực tế để liên hệ/giao thẻ", true)),
            Map.entry("OCCUPATION", new CustomerProfileFieldResponse(
                    "OCCUPATION", "Nghề nghiệp", "text", "Nghề nghiệp hiện tại", true)),
            Map.entry("EMPLOYMENT_STATUS", new CustomerProfileFieldResponse(
                    "EMPLOYMENT_STATUS", "Tình trạng việc làm", "text", "Nhân viên hợp đồng / Chủ doanh nghiệp / Tự do / Học sinh - Sinh viên", true)),
            Map.entry("EMPLOYER_NAME", new CustomerProfileFieldResponse(
                    "EMPLOYER_NAME", "Tên công ty/Cơ quan", "text", "Tên công ty hoặc cơ quan đang làm việc", true)),
            Map.entry("WORK_PHONE", new CustomerProfileFieldResponse(
                    "WORK_PHONE", "Số điện thoại nơi làm việc", "text", "Số điện thoại công ty/cơ quan", true)),
            Map.entry("JOB_TITLE", new CustomerProfileFieldResponse(
                    "JOB_TITLE", "Chức vụ/Vị trí", "text", "Nhân viên, Trưởng phòng, Giám đốc...", true)),
            Map.entry("MONTHLY_INCOME", new CustomerProfileFieldResponse(
                    "MONTHLY_INCOME", "Thu nhập trung bình hàng tháng", "number", "Nhập số tiền thu nhập mỗi tháng", true)),
            Map.entry("SALARY_PAYMENT_METHOD", new CustomerProfileFieldResponse(
                    "SALARY_PAYMENT_METHOD", "Hình thức nhận lương", "text", "Chuyển khoản qua ngân hàng nào hoặc tiền mặt", true)));

    public AccountService(
            CurrentUserService currentUserService,
            UserRepository userRepository,
            KeycloakService keycloakService,
            KeycloakAdminService keycloakAdminService,
            PasswordEncoder passwordEncoder,
            ServiceRepository serviceRepository,
            CustomerProfileService customerProfileService) {
        this.currentUserService = currentUserService;
        this.userRepository = userRepository;
        this.keycloakService = keycloakService;
        this.keycloakAdminService = keycloakAdminService;
        this.passwordEncoder = passwordEncoder;
        this.serviceRepository = serviceRepository;
        this.customerProfileService = customerProfileService;
    }

    @Transactional(readOnly = true)
    /**
     * Lấy hồ sơ của chính người đang đăng nhập từ JWT, không nhận userId từ frontend
     * để tránh người dùng xem nhầm hoặc cố tình xem tài khoản khác.
     */
    public AccountProfileResponse getProfile() {
        return AccountProfileResponse.from(currentUserService.requireUser());
    }

    @Transactional(readOnly = true)
    /**
     * Tra ve ho so nghiep vu ngan hang cua CUSTOMER va danh sach truong con thieu theo dich vu.
     * Frontend dung API nay de quyet dinh co can hien form bo sung truoc khi cap so hay khong.
     */
    public CustomerPaperlessProfileResponse getPaperlessProfile(Long serviceId) {
        User user = requireCustomer();
        List<String> requiredFieldKeys = getRequiredFieldKeys(serviceId);
        Map<String, String> values = customerProfileService.values(user);
        List<CustomerProfileFieldResponse> requiredFields = requiredFieldKeys.stream()
                .map(PAPERLESS_FIELDS::get)
                .filter(field -> field != null)
                .toList();
        List<String> missingFields = requiredFields.stream()
                .map(CustomerProfileFieldResponse::key)
                .filter(key -> isBlank(values.get(key)))
                .toList();

        return new CustomerPaperlessProfileResponse(
                values,
                requiredFields,
                missingFields,
                missingFields.isEmpty());
    }

    @Transactional
    /**
     * Luu cac thong tin chi can khai bao mot lan de lan sau tu dong dien vao giay to online.
     * Neu co serviceId, chi cac truong service yeu cau bat buoc moi duoc validate day du.
     */
    public CustomerPaperlessProfileResponse updatePaperlessProfile(UpdateCustomerPaperlessProfileRequest request) {
        User user = requireCustomer();
        Map<String, String> values = request.values() == null ? Map.of() : request.values();

        for (String key : values.keySet()) {
            if (!PAPERLESS_FIELDS.containsKey(key)) {
                throw new RuntimeException("Truong ho so khong hop le: " + key);
            }
            if (CustomerProfilePolicy.isAccountManaged(key)) {
                throw new RuntimeException("Họ tên và số điện thoại chỉ được thay đổi tại Thông tin tài khoản");
            }
        }

        CustomerProfile profile = customerProfileService.requireForUpdate(user);
        applyPaperlessValues(user, profile, values);

        List<String> requiredFieldKeys = getRequiredFieldKeys(request.serviceId());
        List<String> missingFields = requiredFieldKeys.stream()
                .filter(key -> isBlank(customerProfileService.value(user, key)))
                .toList();
        if (!missingFields.isEmpty()) {
            throw new RuntimeException("Vui long bo sung day du thong tin bat buoc truoc khi lay so");
        }

        customerProfileService.save(profile);
        userRepository.save(user);
        return getPaperlessProfile(request.serviceId());
    }

    @Transactional
    public AccountProfileResponse completeSocialProfile(CompleteSocialProfileRequest request) {
        User user = requireCustomer();
        if (CustomerProfilePolicy.isComplete(user)) {
            throw new RuntimeException("Ho so khach hang da hoan tat");
        }

        user.setFullName(request.fullName().trim());
        user.setPhone(request.phone().trim());
        CustomerProfile profile = customerProfileService.requireForUpdate(user);
        profile.setPermanentAddress(request.permanentAddress().trim());
        profile.setContactAddress(request.contactAddress().trim());
        customerProfileService.save(profile);
        user = userRepository.save(user);
        keycloakAdminService.updateUserProfile(
                user.getKeycloakUserId(), user.getFullName(), user.getEmail(), user.getRole());
        return AccountProfileResponse.from(user);
    }

    @Transactional
    /**
     * Đổi mật khẩu cho mọi role.
     * Mật khẩu hiện tại được kiểm tra trực tiếp với Keycloak; sau đó mật khẩu mới được
     * đồng bộ vào Keycloak và BCrypt hash nội bộ để đăng nhập fallback vẫn hoạt động.
     */
    public void changePassword(ChangePasswordRequest request) {
        User user = currentUserService.requireUser();
        if (request.currentPassword().equals(request.newPassword())) {
            throw new RuntimeException("Mat khau moi phai khac mat khau hien tai");
        }
        PasswordPolicy.validate(request.newPassword());

        // Verify against the primary identity source before changing either copy.
        keycloakService.login(user.getEmail(), request.currentPassword());
        user.setPasswordHash(passwordEncoder.encode(request.newPassword()));
        userRepository.save(user);
        keycloakAdminService.resetUserPassword(user.getKeycloakUserId(), request.newPassword());
    }

    /** Chi CUSTOMER moi duoc tu cap nhat ho so giay to cua chinh minh. */
    private User requireCustomer() {
        User user = currentUserService.requireUser();
        if (!"CUSTOMER".equals(user.getRole())) {
            throw new RuntimeException("Chi khach hang moi duoc cap nhat ho so giay to");
        }
        return user;
    }

    /** Lay danh sach truong ma mot dich vu yeu cau de chuan bi giay to online. */
    private List<String> getRequiredFieldKeys(Long serviceId) {
        if (serviceId == null) {
            return CustomerProfilePolicy.DEFAULT_REQUIRED_FIELDS;
        }

        Services service = serviceRepository.findById(serviceId)
                .orElseThrow(() -> new RuntimeException("Khong tim thay dich vu"));
        return CustomerProfilePolicy.includeDefaults(service.getRequiredCustomerFields()).stream()
                        .filter(PAPERLESS_FIELDS::containsKey)
                        .distinct()
                        .toList();
    }

    private void applyPaperlessValues(User user, CustomerProfile profile, Map<String, String> values) {
        values.forEach((key, value) -> {
            String normalized = normalizeProfileValue(value);
            switch (key) {
                case "FULL_NAME", "MOBILE_PHONE" ->
                        throw new RuntimeException("Họ tên và số điện thoại chỉ được thay đổi tại Thông tin tài khoản");
                case "EMAIL_ADDRESS" -> applyPaperlessEmail(user, normalized);
                default -> customerProfileService.apply(profile, key, normalized);
            }
        });
    }

    private void applyPaperlessEmail(User user, String email) {
        String normalizedEmail = email == null ? null : email.toLowerCase(Locale.ROOT);
        if (!isBlank(normalizedEmail)
                && userRepository.existsByEmailIgnoreCaseAndUserIdNot(normalizedEmail, user.getUserId())) {
            throw new RuntimeException("Email đã được tài khoản khác sử dụng");
        }
        user.setEmail(normalizedEmail);
    }

    private String normalizeProfileValue(String value) {
        return value == null ? null : value.trim();
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
