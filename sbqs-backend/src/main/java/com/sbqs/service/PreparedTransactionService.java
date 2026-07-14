package com.sbqs.service;

import com.sbqs.dto.TicketPaperlessFieldResponse;
import com.sbqs.dto.TicketStaffViewResponse;
import com.sbqs.entity.FormFieldDefinition;
import com.sbqs.entity.Services;
import com.sbqs.entity.Ticket;
import com.sbqs.entity.TransactionDraft;
import com.sbqs.entity.User;
import com.sbqs.repository.TransactionDraftRepository;
import com.sbqs.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Xử lý biểu mẫu giao dịch chuẩn bị trước gắn với ticket: kiểm tra dữ liệu,
 * lưu giao dịch nháp và dựng dữ liệu giấy tờ điện tử cho giao dịch viên.
 */
@Service
public class PreparedTransactionService {

    private final TransactionDraftRepository transactionDraftRepository;
    private final UserRepository userRepository;

    public PreparedTransactionService(
            TransactionDraftRepository transactionDraftRepository,
            UserRepository userRepository) {
        this.transactionDraftRepository = transactionDraftRepository;
        this.userRepository = userRepository;
    }

    /**
     * Chỉ nhận key có trong schema và chuẩn hóa từng kiểu dữ liệu. Việc kiểm tra lại
     * ở backend là bắt buộc vì form động phía trình duyệt có thể bị sửa thủ công.
     */
    public Map<String, Object> validateForm(Services service, Map<String, Object> submittedValues) {
        Map<String, Object> submitted = submittedValues == null ? Map.of() : submittedValues;
        List<FormFieldDefinition> schema = service.getFormSchema() == null ? List.of() : service.getFormSchema();
        Set<String> allowedKeys = schema.stream().map(FormFieldDefinition::key).collect(Collectors.toSet());
        if (!allowedKeys.containsAll(submitted.keySet())) {
            throw new RuntimeException("Bieu mau chua truong du lieu khong duoc phep");
        }

        Map<String, Object> result = new LinkedHashMap<>();
        for (FormFieldDefinition field : schema) {
            Object raw = submitted.get(field.key());
            String value = raw == null ? "" : String.valueOf(raw).trim();
            if (field.required() && value.isBlank()) {
                throw new RuntimeException("Vui long nhap: " + field.label());
            }
            if (value.length() > 500) {
                throw new RuntimeException("Du lieu qua dai tai truong: " + field.label());
            }
            if (!value.isBlank() && List.of("SELECT", "RADIO").contains(field.type())
                    && (field.options() == null || !field.options().contains(value))) {
                throw new RuntimeException("Gia tri khong hop le tai truong: " + field.label());
            }
            if (!value.isBlank() && "NUMBER".equals(field.type()) && !value.matches("^\\d{1,18}$")) {
                throw new RuntimeException("Gia tri so khong hop le tai truong: " + field.label());
            }
            result.put(field.key(), "CHECKBOX".equals(field.type()) ? Boolean.parseBoolean(value) : value);
        }
        return result;
    }

    /** Lưu cả schema lẫn giá trị tại thời điểm lấy số để hồ sơ cũ luôn tái hiện đúng. */
    @Transactional
    public void saveDraft(Ticket ticket, Services service, Map<String, Object> values) {
        TransactionDraft draft = new TransactionDraft();
        draft.setTicket(ticket);
        draft.setServiceId(service.getServiceId());
        draft.setServiceName(service.getServiceName());
        draft.setSchemaSnapshot(new ArrayList<>(service.getFormSchema()));
        draft.setValues(values);
        draft.setCreatedBy(ticket.getCustomerEmail());
        transactionDraftRepository.save(draft);
    }

    /** Chặn cấp số nếu hồ sơ khách hàng còn thiếu trường bắt buộc của dịch vụ. */
    public void requireCompleteProfile(User customer, Services service) {
        List<String> requiredFields = service.getRequiredCustomerFields();
        if (requiredFields == null || requiredFields.isEmpty()) return;

        boolean incomplete = requiredFields.stream()
                .anyMatch(field -> isBlank(getProfileValue(customer, field)));
        if (incomplete) {
            throw new RuntimeException("Vui long bo sung day du ho so giay to truoc khi lay so dich vu nay");
        }
    }

    /**
     * Ghép hồ sơ khách hàng với dữ liệu giao dịch đã khai thành view chỉ dành cho
     * nhân viên đang phục vụ phiếu; alias hồ sơ được loại để không hiển thị trùng.
     */
    @Transactional(readOnly = true)
    public TicketStaffViewResponse toStaffView(Ticket ticket) {
        Services service = ticket.getService();
        User customer = ticket.getCustomerEmail() == null
                ? null : userRepository.findByEmailIgnoreCase(ticket.getCustomerEmail()).orElse(null);
        List<TicketPaperlessFieldResponse> profileFields = defaultProfileFields(customer);
        TransactionDraft draft = transactionDraftRepository.findByTicketTicketId(ticket.getTicketId()).orElse(null);
        if (draft != null) {
            List<TicketPaperlessFieldResponse> transactionFields = draft.getSchemaSnapshot().stream()
                    .filter(field -> !isDefaultProfileAlias(field.key()))
                    .map(field -> new TicketPaperlessFieldResponse(
                            field.key(), field.label(), displayValue(field, draft.getValues().get(field.key()))))
                    .toList();
            List<TicketPaperlessFieldResponse> fields = new ArrayList<>(profileFields);
            fields.addAll(transactionFields);
            return response(ticket, service, fields);
        }

        List<String> requiredFields = service == null || service.getRequiredCustomerFields() == null
                ? List.of() : service.getRequiredCustomerFields();
        List<TicketPaperlessFieldResponse> fields = customer == null
                ? profileFields
                : requiredFields.stream()
                        .map(key -> new TicketPaperlessFieldResponse(
                                key, getProfileLabel(key), displayProfileValue(key, getProfileValue(customer, key))))
                        .toList();
        return response(ticket, service, fields);
    }

    private List<TicketPaperlessFieldResponse> defaultProfileFields(User customer) {
        if (customer == null) return List.of();
        return CustomerProfilePolicy.DEFAULT_REQUIRED_FIELDS.stream()
                .map(key -> new TicketPaperlessFieldResponse(
                        key, getProfileLabel(key), displayProfileValue(key, getProfileValue(customer, key))))
                .toList();
    }

    private boolean isDefaultProfileAlias(String fieldKey) {
        if (fieldKey == null) return false;
        return Set.of("fullname", "accountholder", "phone", "address")
                .contains(fieldKey.toLowerCase());
    }

    private TicketStaffViewResponse response(
            Ticket ticket, Services service, List<TicketPaperlessFieldResponse> fields) {
        return new TicketStaffViewResponse(
                ticket.getTicketId(), ticket.getTicketNumber(), ticket.getStatus(),
                ticket.getCustomerEmail(), ticket.getServingStartedAt(),
                service == null ? null : new TicketStaffViewResponse.ServiceSummary(
                        service.getServiceId(), service.getServiceCode(),
                        service.getServiceName(), service.getServiceType()),
                fields, !fields.isEmpty());
    }

    private String getProfileValue(User user, String field) {
        return switch (field) {
            case "FULL_NAME" -> user.getFullName();
            case "DATE_OF_BIRTH" -> user.getDateOfBirth();
            case "GENDER" -> user.getGender();
            case "NATIONALITY" -> user.getNationality();
            case "IDENTITY_NUMBER" -> user.getIdentityNumber();
            case "IDENTITY_ISSUE_DATE" -> user.getIdentityIssueDate();
            case "IDENTITY_ISSUE_PLACE" -> user.getIdentityIssuePlace();
            case "PASSPORT_NUMBER" -> user.getPassportNumber();
            case "VISA_NUMBER" -> user.getVisaNumber();
            case "MOBILE_PHONE" -> user.getPhone();
            case "EMAIL_ADDRESS" -> user.getEmail();
            case "PERMANENT_ADDRESS" -> user.getPermanentAddress();
            case "CONTACT_ADDRESS" -> user.getContactAddress();
            case "OCCUPATION" -> user.getOccupation();
            case "EMPLOYMENT_STATUS" -> user.getEmploymentStatus();
            case "EMPLOYER_NAME" -> user.getEmployerName();
            case "WORK_PHONE" -> user.getWorkPhone();
            case "JOB_TITLE" -> user.getJobTitle();
            case "MONTHLY_INCOME" -> user.getMonthlyIncome();
            case "SALARY_PAYMENT_METHOD" -> user.getSalaryPaymentMethod();
            case "ACCOUNT_NUMBER" -> user.getAccountNumber();
            case "CARD_DELIVERY_ADDRESS" -> user.getCardDeliveryAddress();
            default -> "";
        };
    }

    private String getProfileLabel(String field) {
        return switch (field) {
            case "FULL_NAME" -> "Họ và tên";
            case "DATE_OF_BIRTH" -> "Ngày sinh";
            case "GENDER" -> "Giới tính";
            case "NATIONALITY" -> "Quốc tịch";
            case "IDENTITY_NUMBER" -> "Số CCCD/CMND";
            case "IDENTITY_ISSUE_DATE" -> "Ngày cấp CCCD";
            case "IDENTITY_ISSUE_PLACE" -> "Nơi cấp CCCD";
            case "PASSPORT_NUMBER" -> "Số hộ chiếu";
            case "VISA_NUMBER" -> "Số thị thực";
            case "MOBILE_PHONE" -> "Số điện thoại di động";
            case "EMAIL_ADDRESS" -> "Địa chỉ email";
            case "PERMANENT_ADDRESS" -> "Địa chỉ thường trú";
            case "CONTACT_ADDRESS" -> "Địa chỉ cư trú hiện tại";
            case "OCCUPATION" -> "Nghề nghiệp";
            case "EMPLOYMENT_STATUS" -> "Tình trạng việc làm";
            case "EMPLOYER_NAME" -> "Tên công ty/Cơ quan";
            case "WORK_PHONE" -> "Số điện thoại nơi làm việc";
            case "JOB_TITLE" -> "Chức vụ/Vị trí";
            case "MONTHLY_INCOME" -> "Thu nhập trung bình hàng tháng";
            case "SALARY_PAYMENT_METHOD" -> "Hình thức nhận lương";
            case "ACCOUNT_NUMBER" -> "Số tài khoản liên kết";
            case "CARD_DELIVERY_ADDRESS" -> "Địa chỉ nhận thẻ";
            default -> field;
        };
    }

    private String displayValue(FormFieldDefinition field, Object value) {
        if (value == null || String.valueOf(value).isBlank()) return "-";
        if (value instanceof Boolean flag) return flag ? "Có" : "Không";
        String text = String.valueOf(value);
        return "NUMBER".equals(field.type()) && text.matches("^\\d{1,18}$")
                ? formatNumber(text)
                : text;
    }

    private String displayProfileValue(String key, String value) {
        if (isBlank(value)) return "-";
        return "MONTHLY_INCOME".equals(key) && value.matches("^\\d{1,18}$")
                ? formatNumber(value)
                : value;
    }

    private String formatNumber(String value) {
        return value.replaceAll("\\B(?=(\\d{3})+(?!\\d))", ",");
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
