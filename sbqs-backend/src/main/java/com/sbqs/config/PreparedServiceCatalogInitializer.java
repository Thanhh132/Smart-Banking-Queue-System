package com.sbqs.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sbqs.entity.FormFieldDefinition;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.context.event.EventListener;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.List;

@Component
public class PreparedServiceCatalogInitializer {
    private static final String CATALOG_VERSION = "prepared-service-catalog-v2";
    private static final String MANUAL_MAPPING_VERSION = "prepared-service-manual-mapping-v1";
    private static final String CASH_FORM_VERSION = "prepared-service-cash-form-v4";
    private static final String DEFAULT_PROFILE_VERSION = "prepared-service-default-profile-v1";
    private static final String DEFAULT_PROFILE_FIELDS =
            "FULL_NAME,MOBILE_PHONE,PERMANENT_ADDRESS,CONTACT_ADDRESS";
    private final JdbcTemplate jdbc;
    private final TransactionTemplate transactionTemplate;
    private final ObjectMapper objectMapper;
    private final CacheManager cacheManager;

    public PreparedServiceCatalogInitializer(JdbcTemplate jdbc, TransactionTemplate transactionTemplate,
                                             ObjectMapper objectMapper, CacheManager cacheManager) {
        this.jdbc = jdbc;
        this.transactionTemplate = transactionTemplate;
        this.objectMapper = objectMapper;
        this.cacheManager = cacheManager;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void synchronizeCatalog() {
        transactionTemplate.executeWithoutResult(status -> {
            jdbc.execute("create table if not exists system_settings (setting_key varchar(100) primary key, setting_value varchar(500) not null)");
            Integer migrated = jdbc.queryForObject(
                    "select count(*) from system_settings where setting_key = ?", Integer.class, CATALOG_VERSION);
            if (migrated == null || migrated == 0) {
                jdbc.update("delete from queue_machine_services");
                jdbc.update("update tickets set status = 'CANCELLED' where status in ('WAITING', 'SERVING')");
                jdbc.update("update appointments set status = 'CANCELLED' where status = 'PENDING'");
                jdbc.update("update tickets set service_id = null");
                jdbc.update("update appointments set service_id = null");
                jdbc.update("delete from services");
                jdbc.update("insert into system_settings(setting_key, setting_value) values (?, 'completed')", CATALOG_VERSION);
            }
            Integer manualMappingApplied = jdbc.queryForObject(
                    "select count(*) from system_settings where setting_key = ?", Integer.class, MANUAL_MAPPING_VERSION);
            if (manualMappingApplied == null || manualMappingApplied == 0) {
                jdbc.update("delete from queue_machine_services");
                jdbc.update("insert into system_settings(setting_key, setting_value) values (?, 'completed')", MANUAL_MAPPING_VERSION);
            }
            jdbc.update("update tickets set status = 'CANCELLED' where service_id is null and status in ('WAITING', 'SERVING')");

            List<Long> branchIds = jdbc.queryForList("select branch_id from branches", Long.class);
            for (Long branchId : branchIds) {
                for (CatalogItem item : catalog()) {
                    ensureService(branchId, item);
                }
            }
            synchronizeCashForms();
            synchronizeDefaultProfileFields();
        });
        Cache servicesCache = cacheManager.getCache("services");
        if (servicesCache != null) servicesCache.clear();
    }

    private void ensureService(Long branchId, CatalogItem item) {
        List<Long> ids = jdbc.queryForList(
                "select service_id from services where branch_id = ? and service_code = ?", Long.class, branchId, item.code());
        if (ids.isEmpty()) {
            String schema;
            try {
                schema = objectMapper.writeValueAsString(item.fields());
            } catch (Exception exception) {
                throw new IllegalStateException("Khong the khoi tao bieu mau dich vu", exception);
            }
            jdbc.queryForObject("""
                    insert into services(service_code, service_name, branch_id, service_type, description,
                                         estimated_time, status, required_customer_fields, form_schema)
                    values (?, ?, ?, ?, ?, ?, 'ACTIVE', ?, ?) returning service_id
                    """, Long.class, item.code(), item.name(), branchId, item.type(), item.description(), item.minutes(),
                    DEFAULT_PROFILE_FIELDS, schema);
        }
    }

    private void synchronizeDefaultProfileFields() {
        Integer synchronizedCount = jdbc.queryForObject(
                "select count(*) from system_settings where setting_key = ?", Integer.class, DEFAULT_PROFILE_VERSION);
        if (synchronizedCount != null && synchronizedCount > 0) return;

        jdbc.update("""
                update services
                set required_customer_fields = case
                    when required_customer_fields is null or trim(required_customer_fields) = '' then ?
                    else concat(?, ',', required_customer_fields)
                end
                """, DEFAULT_PROFILE_FIELDS, DEFAULT_PROFILE_FIELDS);
        jdbc.update("insert into system_settings(setting_key, setting_value) values (?, 'completed')", DEFAULT_PROFILE_VERSION);
    }

    private void synchronizeCashForms() {
        Integer synchronizedCount = jdbc.queryForObject(
                "select count(*) from system_settings where setting_key = ?", Integer.class, CASH_FORM_VERSION);
        if (synchronizedCount != null && synchronizedCount > 0) return;

        catalog().stream()
                .filter(item -> List.of("CASH_DEPOSIT", "CASH_WITHDRAW").contains(item.code()))
                .forEach(item -> {
                    try {
                        String schema = objectMapper.writeValueAsString(item.fields());
                        jdbc.update("""
                                update services
                                set form_schema = ?
                                where service_code = ?
                                """, schema, item.code());
                    } catch (Exception exception) {
                        throw new IllegalStateException("Khong the cap nhat bieu mau tien mat", exception);
                    }
                });
        jdbc.update("insert into system_settings(setting_key, setting_value) values (?, 'completed')", CASH_FORM_VERSION);
    }

    private List<CatalogItem> catalog() {
        return List.of(
                new CatalogItem("DEBIT_CARD_NEW", "Phát hành thẻ ghi nợ mới", "CARD", 15,
                        "Đăng ký thẻ ghi nợ liên kết với tài khoản thanh toán.", List.of(
                        field("accountNumber", "Số tài khoản liên kết", "TEXT", true, "Tài khoản", "Thông tin thẻ"),
                        field("cardholderName", "Họ tên in trên thẻ (IN HOA, KHÔNG DẤU)", "TEXT", true, "NGUYEN VAN A", "Thông tin thẻ"),
                        options("cardNetwork", "Loại thẻ", "RADIO", true, "Thông tin thẻ", "Napas nội địa", "Visa/Mastercard"),
                        options("cardTier", "Hạng thẻ", "RADIO", true, "Thông tin thẻ", "Hạng chuẩn", "Hạng sang"),
                        field("onlinePayment", "Đăng ký thanh toán trực tuyến", "CHECKBOX", false, "", "Tiện ích"),
                        options("deliveryMethod", "Nhận thẻ vật lý", "RADIO", true, "Nhận thẻ", "Tại chi nhánh", "Nhận tại nhà"))),
                new CatalogItem("DEBIT_CARD_REISSUE", "Cấp lại thẻ ghi nợ", "CARD", 12,
                        "Cấp lại thẻ bị mất, hỏng hoặc hết hạn.", List.of(
                        field("accountNumber", "Số tài khoản liên kết", "TEXT", true, "Nhập số tài khoản", "Thông tin tài khoản"),
                        field("currentCardNumber", "Bốn số cuối của thẻ cũ", "TEXT", false, "Ví dụ: 1234", "Thông tin thẻ cũ"),
                        field("cardholderName", "Họ tên in trên thẻ", "TEXT", true, "NGUYEN VAN A", "Thông tin thẻ mới"),
                        options("reissueReason", "Lý do cấp lại", "SELECT", true, "Yêu cầu cấp lại", "Thẻ bị mất", "Thẻ bị hỏng", "Thẻ hết hạn", "Đổi loại thẻ"),
                        options("deliveryMethod", "Nơi nhận thẻ mới", "RADIO", true, "Nhận thẻ", "Tại chi nhánh", "Nhận tại nhà"))),
                new CatalogItem("ACCOUNT_OPEN", "Mở tài khoản thanh toán", "ACCOUNT", 20,
                        "Đăng ký tài khoản thanh toán mới tại ngân hàng.", List.of(
                        field("fullName", "Họ và tên", "TEXT", true, "Nguyễn Văn A", "Định danh"),
                        field("dateOfBirth", "Ngày sinh", "DATE", true, "", "Định danh"),
                        field("identityNumber", "Số CCCD", "TEXT", true, "12 chữ số", "Định danh"),
                        field("address", "Địa chỉ", "TEXTAREA", true, "Địa chỉ thường trú", "Liên hệ"),
                        field("phone", "Số điện thoại", "TEXT", true, "09xxxxxxxx", "Liên hệ"),
                        options("residency", "Trạng thái cư trú", "RADIO", true, "Tuân thủ", "Người cư trú", "Không cư trú"),
                        field("pep", "Khách hàng thuộc diện PEP", "CHECKBOX", false, "", "Tuân thủ"),
                        field("fatca", "Có nghĩa vụ khai báo FATCA", "CHECKBOX", false, "", "Tuân thủ"))),
                new CatalogItem("DIGITAL_BANKING", "Đăng ký ngân hàng số", "ACCOUNT", 10,
                        "Đăng ký sử dụng ứng dụng ngân hàng và phương thức xác thực.", List.of(
                        field("accountNumber", "Số tài khoản sử dụng", "TEXT", true, "Nhập số tài khoản", "Tài khoản"),
                        field("phone", "Số điện thoại đăng ký", "TEXT", true, "09xxxxxxxx", "Thông tin liên hệ"),
                        field("email", "Địa chỉ thư điện tử", "TEXT", true, "ten@vidu.com", "Thông tin liên hệ"),
                        options("otpMethod", "Cách nhận mã xác thực", "RADIO", true, "Xác thực giao dịch", "Tin nhắn SMS", "Ứng dụng Smart OTP"))),
                new CatalogItem("CASH_DEPOSIT", "Nộp tiền mặt tại quầy", "CASH", 10,
                        "Nộp tiền mặt vào tài khoản thanh toán.", List.of(
                        field("accountNumber", "Số tài khoản nhận tiền", "TEXT", true, "Nhập số tài khoản", "Thông tin giao dịch"),
                        field("accountHolder", "Họ và tên khách hàng", "TEXT", true, "Tự động lấy từ hồ sơ", "Thông tin giao dịch"),
                        field("amount", "Số tiền muốn nộp", "NUMBER", true, "Nhập số tiền VNĐ", "Thông tin giao dịch"),
                        field("transactionContent", "Nội dung nộp tiền", "TEXT", false, "Ví dụ: Nộp tiền vào tài khoản", "Thông tin bổ sung"))),
                new CatalogItem("CASH_WITHDRAW", "Rút tiền mặt tại quầy", "CASH", 10,
                        "Rút tiền mặt từ tài khoản thanh toán.", List.of(
                        field("accountNumber", "Số tài khoản rút tiền", "TEXT", true, "Nhập số tài khoản", "Thông tin giao dịch"),
                        field("accountHolder", "Họ và tên khách hàng", "TEXT", true, "Tự động lấy từ hồ sơ", "Thông tin giao dịch"),
                        field("amount", "Số tiền muốn rút", "NUMBER", true, "Nhập số tiền VNĐ", "Thông tin giao dịch"),
                        options("purpose", "Mục đích sử dụng tiền", "SELECT", false, "Thông tin bổ sung", "Chi tiêu cá nhân", "Kinh doanh", "Thanh toán", "Khác"))),
                new CatalogItem("SAVINGS", "Mở sổ tiết kiệm", "SAVINGS", 15,
                        "Đăng ký khoản tiền gửi, kỳ hạn và cách xử lý khi đáo hạn.", List.of(
                        field("debitAccount", "Số tài khoản trích tiền", "TEXT", true, "Số tài khoản", "Khoản gửi"),
                        field("depositAmount", "Số tiền gửi", "NUMBER", true, "Số tiền VNĐ", "Khoản gửi"),
                        options("term", "Kỳ hạn gửi", "SELECT", true, "Khoản gửi", "1 tháng", "3 tháng", "6 tháng", "12 tháng", "24 tháng"),
                        options("ownership", "Hình thức sở hữu", "RADIO", true, "Sở hữu", "Cá nhân", "Đồng sở hữu"),
                        options("maturityInstruction", "Chỉ thị khi đáo hạn", "RADIO", true, "Đáo hạn", "Tái tục cả gốc và lãi", "Tái tục gốc, trả lãi về tài khoản", "Tất toán toàn bộ"))),
                new CatalogItem("INTERNATIONAL_TRANSFER", "Chuyển tiền quốc tế", "TRANSFER", 25,
                        "Chuẩn bị thông tin Swift / Western Union và bên chịu phí.", List.of(
                        field("debitAccount", "Số tài khoản trích", "TEXT", true, "Số tài khoản", "Nguồn tiền"),
                        field("beneficiaryName", "Tên người nhận", "TEXT", true, "Tên theo hộ chiếu", "Người nhận"),
                        field("beneficiaryAddress", "Địa chỉ người nhận", "TEXTAREA", true, "Địa chỉ nước ngoài", "Người nhận"),
                        field("beneficiaryAccount", "Số tài khoản nhận", "TEXT", true, "IBAN / Account number", "Người nhận"),
                        field("swiftCode", "Mã SWIFT ngân hàng nhận", "TEXT", true, "8 hoặc 11 ký tự", "Ngân hàng nhận"),
                        field("amount", "Số tiền", "NUMBER", true, "Số tiền", "Chuyển tiền"),
                        options("currency", "Loại ngoại tệ", "SELECT", true, "Chuyển tiền", "USD", "EUR", "GBP", "JPY", "AUD"),
                        options("purpose", "Mục đích chuyển tiền", "SELECT", true, "Tuân thủ", "Du học", "Định cư", "Trợ cấp thân nhân", "Khám bệnh"),
                        options("feeMethod", "Phương thức chịu phí", "RADIO", true, "Chi phí", "Người chuyển chịu phí", "Người nhận chịu phí", "Chia đôi phí"))),
                new CatalogItem("CREDIT_CARD", "Phát hành thẻ tín dụng", "CARD", 25,
                        "Đăng ký hạn mức và cách thanh toán thẻ tín dụng.", List.of(
                        field("employer", "Công ty đang làm việc", "TEXT", true, "Tên công ty", "Nghề nghiệp"),
                        field("monthlyIncome", "Thu nhập hàng tháng", "NUMBER", true, "Số tiền VNĐ", "Tài chính"),
                        field("proposedLimit", "Hạn mức đề xuất", "NUMBER", true, "Số tiền VNĐ", "Tài chính"),
                        options("incomeProof", "Chứng minh thu nhập", "RADIO", true, "Tài chính", "Lương chuyển khoản", "Thế chấp sổ tiết kiệm"),
                        field("cardInsurance", "Đăng ký bảo hiểm thẻ", "CHECKBOX", false, "", "Tiện ích"),
                        options("autoDebit", "Tỷ lệ trích nợ tự động", "RADIO", true, "Thanh toán", "100% dư nợ", "5% tối thiểu"),
                        field("deliveryAddress", "Địa chỉ nhận thẻ", "TEXTAREA", true, "Địa chỉ nhận", "Nhận thẻ"))),
                new CatalogItem("IDENTITY_UPDATE", "Thay đổi giấy tờ định danh", "KYC", 15,
                        "Cập nhật CCCD mới và đồng bộ thông tin tài khoản.", List.of(
                        field("oldIdentityNumber", "Số định danh cũ", "TEXT", true, "CMND/CCCD cũ", "Định danh cũ"),
                        field("newIdentityNumber", "Số định danh mới", "TEXT", true, "CCCD gắn chip", "Định danh mới"),
                        field("newIssueDate", "Ngày cấp mới", "DATE", true, "", "Định danh mới"),
                        field("newIssuePlace", "Nơi cấp mới", "TEXT", true, "Nơi cấp", "Định danh mới"),
                        field("syncSavings", "Đồng bộ sang các sổ tiết kiệm cũ", "CHECKBOX", false, "", "Yêu cầu cập nhật"),
                        field("changeOtpPhone", "Thay đổi số điện thoại nhận OTP", "CHECKBOX", false, "", "Yêu cầu cập nhật"),
                        field("newOtpPhone", "Số điện thoại nhận OTP mới", "TEXT", false, "09xxxxxxxx", "Yêu cầu cập nhật"))),
                new CatalogItem("SIGNATURE_UPDATE", "Cập nhật chữ ký mẫu", "KYC", 10,
                        "Đăng ký thay đổi chữ ký sử dụng trong giao dịch ngân hàng.", List.of(
                        field("accountNumber", "Số tài khoản", "TEXT", true, "Nhập số tài khoản", "Thông tin tài khoản"),
                        field("identityNumber", "Số CCCD hiện tại", "TEXT", true, "Nhập 12 chữ số", "Xác minh khách hàng"),
                        options("updateScope", "Phạm vi áp dụng chữ ký mới", "RADIO", true, "Phạm vi cập nhật", "Tài khoản thanh toán", "Tất cả sản phẩm đang sử dụng")))
        );
    }

    private FormFieldDefinition field(String key, String label, String type, boolean required, String placeholder, String section) {
        return new FormFieldDefinition(key, label, type, required, placeholder, section, List.of());
    }

    private FormFieldDefinition options(String key, String label, String type, boolean required, String section, String... options) {
        return new FormFieldDefinition(key, label, type, required, "", section, List.of(options));
    }

    private record CatalogItem(String code, String name, String type, int minutes, String description,
                               List<FormFieldDefinition> fields) { }
}
