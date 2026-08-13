package com.sbqs.service;

import com.sbqs.dto.TicketPaperlessFieldResponse;
import com.sbqs.dto.TicketStaffViewResponse;
import com.sbqs.entity.FormFieldDefinition;
import com.sbqs.entity.Services;
import com.sbqs.entity.Ticket;
import com.sbqs.entity.TransactionDraft;
import com.sbqs.entity.User;
import com.sbqs.repository.TransactionDraftRepository;
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
    private final CustomerProfileService customerProfileService;

    public PreparedTransactionService(
            TransactionDraftRepository transactionDraftRepository,
            CustomerProfileService customerProfileService) {
        this.transactionDraftRepository = transactionDraftRepository;
        this.customerProfileService = customerProfileService;
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
    public void saveDraft(Ticket ticket, Services service, User customer, Map<String, Object> values) {
        TransactionDraft draft = new TransactionDraft();
        draft.setTicket(ticket);
        draft.setServiceId(service.getServiceId());
        draft.setServiceName(service.getServiceName());
        draft.setSchemaSnapshot(new ArrayList<>(service.getFormSchema()));
        draft.setProfileSnapshot(customerProfileService.snapshot(
                customer, CustomerProfilePolicy.includeDefaults(service.getRequiredCustomerFields())));
        draft.setValues(values);
        draft.setCreatedBy(customer.getEmail());
        transactionDraftRepository.save(draft);
    }

    /** Chặn cấp số nếu hồ sơ khách hàng còn thiếu trường bắt buộc của dịch vụ. */
    public void requireCompleteProfile(User customer, Services service) {
        List<String> requiredFields = service.getRequiredCustomerFields();
        if (requiredFields == null || requiredFields.isEmpty()) return;

        Map<String, String> profileValues = customerProfileService.values(customer);
        boolean incomplete = requiredFields.stream()
                .filter(profileValues::containsKey)
                .anyMatch(field -> isBlank(profileValues.get(field)));
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
        User customer = ticket.getCustomer();
        User resolvedCustomer = customer;
        TransactionDraft draft = transactionDraftRepository.findByTicketTicketId(ticket.getTicketId()).orElse(null);
        if (draft != null) {
            List<TicketPaperlessFieldResponse> profileFields = snapshotProfileFields(draft, resolvedCustomer);
            List<TicketPaperlessFieldResponse> transactionFields = draft.getSchemaSnapshot().stream()
                    .filter(field -> !isDefaultProfileAlias(field.key()))
                    .map(field -> new TicketPaperlessFieldResponse(
                            field.key(), field.label(), displayValue(field, draft.getValues().get(field.key()))))
                    .toList();
            List<TicketPaperlessFieldResponse> fields = new ArrayList<>(profileFields);
            fields.addAll(transactionFields);
            return response(ticket, service, resolvedCustomer, fields);
        }

        List<String> requiredFields = service == null || service.getRequiredCustomerFields() == null
                ? List.of() : service.getRequiredCustomerFields();
        List<TicketPaperlessFieldResponse> fields = resolvedCustomer == null
                ? List.of()
                : requiredFields.stream()
                        .map(key -> new TicketPaperlessFieldResponse(
                                key, getProfileLabel(key), displayProfileValue(key, customerProfileService.value(resolvedCustomer, key))))
                        .toList();
        return response(ticket, service, resolvedCustomer, fields);
    }

    private List<TicketPaperlessFieldResponse> snapshotProfileFields(TransactionDraft draft, User legacyCustomer) {
        Map<String, Object> snapshot = draft.getProfileSnapshot();
        if (snapshot != null && !snapshot.isEmpty()) {
            return snapshot.entrySet().stream()
                    .map(entry -> new TicketPaperlessFieldResponse(
                            entry.getKey(), getProfileLabel(entry.getKey()),
                            displayProfileValue(entry.getKey(), entry.getValue() == null ? null : String.valueOf(entry.getValue()))))
                    .toList();
        }
        // Compatibility fallback: old drafts have no profile snapshot.
        if (legacyCustomer == null) return List.of();
        return CustomerProfilePolicy.DEFAULT_REQUIRED_FIELDS.stream()
                .map(key -> new TicketPaperlessFieldResponse(
                        key, getProfileLabel(key), displayProfileValue(key, customerProfileService.value(legacyCustomer, key))))
                .toList();
    }

    private boolean isDefaultProfileAlias(String fieldKey) {
        if (fieldKey == null) return false;
        return Set.of("fullname", "accountholder", "phone", "address")
                .contains(fieldKey.toLowerCase());
    }

    private TicketStaffViewResponse response(
            Ticket ticket, Services service, User customer, List<TicketPaperlessFieldResponse> fields) {
        return new TicketStaffViewResponse(
                ticket.getTicketId(), ticket.getTicketNumber(), ticket.getStatus(),
                customer == null ? null : customer.getEmail(), ticket.getServingStartedAt(),
                customer == null ? null : new TicketStaffViewResponse.CustomerSummary(
                        customer.getUserId(), customer.getFullName(), customer.getEmail(), customer.getPhone()),
                service == null ? null : new TicketStaffViewResponse.ServiceSummary(
                        service.getServiceId(), service.getServiceCode(),
                        service.getServiceName(), service.getServiceType()),
                fields, !fields.isEmpty());
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
