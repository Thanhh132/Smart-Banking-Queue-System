package com.sbqs.service;

import com.sbqs.config.AccountChangeProperties;
import com.sbqs.dto.AccountChangeConfirmationResponse;
import com.sbqs.dto.AccountProfileResponse;
import com.sbqs.dto.ChangePasswordRequest;
import com.sbqs.dto.CustomerPaperlessProfileResponse;
import com.sbqs.dto.CustomerProfileFieldResponse;
import com.sbqs.dto.UpdateAccountProfileRequest;
import com.sbqs.dto.UpdateCustomerPaperlessProfileRequest;
import com.sbqs.entity.AccountChangeToken;
import com.sbqs.entity.Services;
import com.sbqs.entity.User;
import com.sbqs.repository.AccountChangeTokenRepository;
import com.sbqs.repository.ServiceRepository;
import com.sbqs.repository.UserRepository;
import com.sbqs.util.PasswordPolicy;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

@Service
public class AccountService {
    private final CurrentUserService currentUserService;
    private final UserRepository userRepository;
    private final KeycloakService keycloakService;
    private final PasswordEncoder passwordEncoder;
    private final AccountChangeTokenRepository changeTokenRepository;
    private final ServiceRepository serviceRepository;
    private final AuthenticationMailService mailService;
    private final AccountChangeProperties changeProperties;

    private static final Map<String, CustomerProfileFieldResponse> PAPERLESS_FIELDS = Map.ofEntries(
            Map.entry("DATE_OF_BIRTH", new CustomerProfileFieldResponse(
                    "DATE_OF_BIRTH", "Ngày sinh", "date", "Định dạng dd/mm/yyyy", true)),
            Map.entry("IDENTITY_NUMBER", new CustomerProfileFieldResponse(
                    "IDENTITY_NUMBER", "Số CCCD/Hộ chiếu", "text", "Nhập số giấy tờ tùy thân", true)),
            Map.entry("IDENTITY_ISSUE_DATE", new CustomerProfileFieldResponse(
                    "IDENTITY_ISSUE_DATE", "Ngày cấp CCCD/Hộ chiếu", "date", "Định dạng dd/mm/yyyy", true)),
            Map.entry("IDENTITY_ISSUE_PLACE", new CustomerProfileFieldResponse(
                    "IDENTITY_ISSUE_PLACE", "Nơi cấp CCCD/Hộ chiếu", "text", "Cơ quan cấp giấy tờ", true)),
            Map.entry("PERMANENT_ADDRESS", new CustomerProfileFieldResponse(
                    "PERMANENT_ADDRESS", "Địa chỉ thường trú", "textarea", "Số nhà, đường, phường/xã, quận/huyện, tỉnh/thành", true)),
            Map.entry("CONTACT_ADDRESS", new CustomerProfileFieldResponse(
                    "CONTACT_ADDRESS", "Địa chỉ liên hệ", "textarea", "Tự điền theo địa chỉ thường trú nếu để trống", true)),
            Map.entry("OCCUPATION", new CustomerProfileFieldResponse(
                    "OCCUPATION", "Nghề nghiệp", "text", "Nghề nghiệp hiện tại", true)),
            Map.entry("EMPLOYER_NAME", new CustomerProfileFieldResponse(
                    "EMPLOYER_NAME", "Đơn vị công tác", "text", "Tên công ty hoặc đơn vị công tác", true)),
            Map.entry("MONTHLY_INCOME", new CustomerProfileFieldResponse(
                    "MONTHLY_INCOME", "Thu nhập hằng tháng", "number", "Thu nhập dự kiến mỗi tháng", true)),
            Map.entry("ACCOUNT_NUMBER", new CustomerProfileFieldResponse(
                    "ACCOUNT_NUMBER", "Số tài khoản liên kết", "text", "Số tài khoản ngân hàng dùng để liên kết thẻ", true)),
            Map.entry("CARD_DELIVERY_ADDRESS", new CustomerProfileFieldResponse(
                    "CARD_DELIVERY_ADDRESS", "Địa chỉ nhận thẻ", "textarea", "Tự điền theo địa chỉ liên hệ nếu để trống", true)));

    public AccountService(
            CurrentUserService currentUserService,
            UserRepository userRepository,
            KeycloakService keycloakService,
            PasswordEncoder passwordEncoder,
            AccountChangeTokenRepository changeTokenRepository,
            ServiceRepository serviceRepository,
            AuthenticationMailService mailService,
            AccountChangeProperties changeProperties) {
        this.currentUserService = currentUserService;
        this.userRepository = userRepository;
        this.keycloakService = keycloakService;
        this.passwordEncoder = passwordEncoder;
        this.changeTokenRepository = changeTokenRepository;
        this.serviceRepository = serviceRepository;
        this.mailService = mailService;
        this.changeProperties = changeProperties;
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
        Map<String, String> values = toPaperlessValues(user);
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
        }

        applyPaperlessValues(user, values);

        List<String> requiredFieldKeys = getRequiredFieldKeys(request.serviceId());
        List<String> missingFields = requiredFieldKeys.stream()
                .filter(key -> isBlank(getPaperlessValue(user, key)))
                .toList();
        if (!missingFields.isEmpty()) {
            throw new RuntimeException("Vui long bo sung day du thong tin bat buoc truoc khi lay so");
        }

        userRepository.save(user);
        return getPaperlessProfile(request.serviceId());
    }

    @Transactional
    /**
     * Tạo yêu cầu thay đổi hồ sơ cho CUSTOMER.
     * Thông tin mới chỉ được lưu tạm trong token; chưa cập nhật users hoặc Keycloak
     * cho đến khi chủ tài khoản bấm link gửi về email hiện tại.
     */
    public void requestProfileChange(UpdateAccountProfileRequest request) {
        User user = currentUserService.requireUser();
        if (!"CUSTOMER".equals(user.getRole())) {
            throw new RuntimeException("Thong tin nhan su do cong ty quan ly va khong the tu chinh sua");
        }

        String fullName = request.fullName().trim();
        String email = request.email().trim().toLowerCase(Locale.ROOT);
        String phone = request.phone().trim();
        if (userRepository.existsByPhoneAndUserIdNot(phone, user.getUserId())) {
            throw new RuntimeException("So dien thoai da ton tai. Vui long su dung so khac");
        }
        if (userRepository.existsByEmailIgnoreCaseAndUserIdNot(email, user.getUserId())) {
            throw new RuntimeException("Email da ton tai. Vui long su dung email khac");
        }
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
    /**
     * Xử lý link xác nhận thay đổi hồ sơ.
     * Nếu email không đổi, xác nhận email hiện tại là đủ để áp dụng thay đổi.
     * Nếu email thay đổi, bước này yêu cầu xác nhận email hiện tại trước rồi mới gửi
     * link thứ hai sang email mới để chứng minh người dùng sở hữu cả hai địa chỉ.
     */
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
                if (change.getNewEmailTokenHash() == null) {
                    String newEmailRawToken = newToken();
                    change.setNewEmailTokenHash(hash(newEmailRawToken));
                    change.setExpiresAt(LocalDateTime.now().plusMinutes(changeProperties.getExpiryMinutes()));
                    changeTokenRepository.save(change);
                    sendConfirmationMail(
                            change.getPendingEmail(), newEmailRawToken, "Xac minh email moi",
                            "Bam vao lien ket de xac minh email moi cho tai khoan SBQS.");
                }
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

    /**
     * Áp dụng thông tin đã được xác nhận vào Keycloak và database nội bộ.
     * Các điều kiện trùng email/số điện thoại được kiểm tra lại tại thời điểm áp dụng
     * vì dữ liệu có thể đã thay đổi trong lúc người dùng chưa bấm link.
     */
    private AccountChangeConfirmationResponse applyProfileChange(AccountChangeToken change) {
        User user = change.getUser();
        boolean emailChanged = !change.getPendingEmail().equalsIgnoreCase(user.getEmail());
        if (userRepository.existsByPhoneAndUserIdNot(change.getPendingPhone(), user.getUserId())) {
            throw new RuntimeException("So dien thoai da duoc tai khoan khac su dung");
        }
        if (userRepository.existsByEmailIgnoreCaseAndUserIdNot(change.getPendingEmail(), user.getUserId())) {
            throw new RuntimeException("Email da duoc tai khoan khac su dung");
        }

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
        keycloakService.resetUserPassword(user.getKeycloakUserId(), request.newPassword());
    }

    /** Gửi email HTML bất đồng bộ, giúp API không phải chờ SMTP/MailHog phản hồi. */
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

    /** Tạo token ngẫu nhiên dùng một lần; database chỉ lưu SHA-256 hash của token. */
    private String newToken() {
        return UUID.randomUUID() + "." + UUID.randomUUID();
    }

    /** Băm token trước khi tra cứu/lưu để token thật không bị lộ nếu database bị đọc. */
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
            return List.of();
        }

        Services service = serviceRepository.findById(serviceId)
                .orElseThrow(() -> new RuntimeException("Khong tim thay dich vu"));
        return service.getRequiredCustomerFields() == null
                ? List.of()
                : service.getRequiredCustomerFields().stream()
                        .filter(PAPERLESS_FIELDS::containsKey)
                        .distinct()
                        .toList();
    }

    private Map<String, String> toPaperlessValues(User user) {
        Map<String, String> values = new LinkedHashMap<>();
        PAPERLESS_FIELDS.keySet().forEach(key -> values.put(key, getPaperlessValue(user, key)));
        return values;
    }

    private String getPaperlessValue(User user, String key) {
        return switch (key) {
            case "DATE_OF_BIRTH" -> user.getDateOfBirth();
            case "IDENTITY_NUMBER" -> user.getIdentityNumber();
            case "IDENTITY_ISSUE_DATE" -> user.getIdentityIssueDate();
            case "IDENTITY_ISSUE_PLACE" -> user.getIdentityIssuePlace();
            case "PERMANENT_ADDRESS" -> user.getPermanentAddress();
            case "CONTACT_ADDRESS" -> user.getContactAddress();
            case "OCCUPATION" -> user.getOccupation();
            case "EMPLOYER_NAME" -> user.getEmployerName();
            case "MONTHLY_INCOME" -> user.getMonthlyIncome();
            case "ACCOUNT_NUMBER" -> user.getAccountNumber();
            case "CARD_DELIVERY_ADDRESS" -> user.getCardDeliveryAddress();
            default -> "";
        };
    }

    private void applyPaperlessValues(User user, Map<String, String> values) {
        values.forEach((key, value) -> {
            String normalized = normalizeProfileValue(value);
            switch (key) {
                case "DATE_OF_BIRTH" -> user.setDateOfBirth(normalized);
                case "IDENTITY_NUMBER" -> user.setIdentityNumber(normalized);
                case "IDENTITY_ISSUE_DATE" -> user.setIdentityIssueDate(normalized);
                case "IDENTITY_ISSUE_PLACE" -> user.setIdentityIssuePlace(normalized);
                case "PERMANENT_ADDRESS" -> user.setPermanentAddress(normalized);
                case "CONTACT_ADDRESS" -> user.setContactAddress(normalized);
                case "OCCUPATION" -> user.setOccupation(normalized);
                case "EMPLOYER_NAME" -> user.setEmployerName(normalized);
                case "MONTHLY_INCOME" -> user.setMonthlyIncome(normalized);
                case "ACCOUNT_NUMBER" -> user.setAccountNumber(normalized);
                case "CARD_DELIVERY_ADDRESS" -> user.setCardDeliveryAddress(normalized);
                default -> throw new RuntimeException("Truong ho so khong hop le: " + key);
            }
        });
    }

    private String normalizeProfileValue(String value) {
        return value == null ? null : value.trim();
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
