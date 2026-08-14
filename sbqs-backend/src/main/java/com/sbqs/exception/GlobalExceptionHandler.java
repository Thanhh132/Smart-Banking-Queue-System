package com.sbqs.exception;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.bind.MethodArgumentNotValidException;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@RestControllerAdvice
public class GlobalExceptionHandler {
    private static final Pattern CONSTRAINT_PATTERN = Pattern.compile("constraint \\\"([^\\\"]+)\\\"");
    private static final String ACTIVE_TICKET_CUSTOMER_INDEX = "ux_tickets_one_active_customer";
    private static final String TICKET_IDEMPOTENCY_INDEX = "ux_tickets_customer_idempotency";

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<Map<String, Object>> handleRuntimeException(RuntimeException ex) {
        return badRequest(ex.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidationException(MethodArgumentNotValidException ex) {
        String message = ex.getBindingResult().getFieldErrors().stream()
                .findFirst()
                .map(error -> error.getDefaultMessage())
                .orElse("Du lieu gui len khong hop le");
        return badRequest(message);
    }

    @ExceptionHandler(LoginRateLimitExceededException.class)
    public ResponseEntity<Map<String, Object>> handleLoginRateLimit(LoginRateLimitExceededException ex) {
        Map<String, Object> error = new HashMap<>();
        error.put("timestamp", LocalDateTime.now());
        error.put("status", HttpStatus.TOO_MANY_REQUESTS.value());
        error.put("error", "Too Many Requests");
        error.put("message", ex.getMessage());
        error.put("retryAfterSeconds", ex.getRetryAfterSeconds());
        return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).body(error);
    }

    @ExceptionHandler(TicketRateLimitExceededException.class)
    public ResponseEntity<Map<String, Object>> handleTicketRateLimit(TicketRateLimitExceededException ex) {
        ResponseEntity<Map<String, Object>> response = errorResponse(HttpStatus.TOO_MANY_REQUESTS, ex.getMessage());
        response.getBody().put("code", "TICKET_RATE_LIMITED");
        response.getBody().put("retryAfterSeconds", ex.getRetryAfterSeconds());
        return response;
    }

    @ExceptionHandler(QueueCapacityExceededException.class)
    public ResponseEntity<Map<String, Object>> handleQueueCapacity(QueueCapacityExceededException ex) {
        ResponseEntity<Map<String, Object>> response = errorResponse(HttpStatus.SERVICE_UNAVAILABLE, ex.getMessage());
        response.getBody().put("code", "QUEUE_CAPACITY_REACHED");
        response.getBody().put("retryAfterSeconds", ex.getRetryAfterSeconds());
        return response;
    }

    @ExceptionHandler(ActiveTicketExistsException.class)
    public ResponseEntity<Map<String, Object>> handleActiveTicketExists(ActiveTicketExistsException ex) {
        ResponseEntity<Map<String, Object>> response = errorResponse(HttpStatus.CONFLICT, ex.getMessage());
        response.getBody().put("code", "ACTIVE_TICKET_EXISTS");
        response.getBody().put("ticketId", ex.getTicketId());
        return response;
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<Map<String, Object>> handleDataIntegrityException(
            DataIntegrityViolationException ex) {

        String constraintName = extractConstraintName(rootMessage(ex));
        if (ACTIVE_TICKET_CUSTOMER_INDEX.equalsIgnoreCase(constraintName)) {
            ResponseEntity<Map<String, Object>> response = errorResponse(
                    HttpStatus.CONFLICT,
                    "Bạn đang có phiếu chưa hoàn thành. Hãy hoàn thành hoặc hủy phiếu hiện tại trước.");
            response.getBody().put("code", "ACTIVE_TICKET_EXISTS");
            return response;
        }
        if (TICKET_IDEMPOTENCY_INDEX.equalsIgnoreCase(constraintName)) {
            ResponseEntity<Map<String, Object>> response = errorResponse(
                    HttpStatus.CONFLICT,
                    "Yêu cầu lấy số này đã được xử lý trước đó.");
            response.getBody().put("code", "IDEMPOTENCY_KEY_CONFLICT");
            return response;
        }

        return badRequest(resolveDataIntegrityMessage(ex));
    }

    private ResponseEntity<Map<String, Object>> badRequest(String message) {
        return errorResponse(HttpStatus.BAD_REQUEST, message);
    }

    private ResponseEntity<Map<String, Object>> errorResponse(HttpStatus status, String message) {
        Map<String, Object> error = new HashMap<>();
        error.put("timestamp", LocalDateTime.now());
        error.put("status", status.value());
        error.put("error", status.getReasonPhrase());
        error.put("message", message == null || message.isBlank()
                ? "Không thể thực hiện thao tác này."
                : message);

        return ResponseEntity
                .status(status)
                .body(error);
    }

    private String resolveDataIntegrityMessage(DataIntegrityViolationException ex) {
        String detail = rootMessage(ex);
        String lower = detail.toLowerCase(Locale.ROOT);
        String constraintName = extractConstraintName(detail);

        String mapped = mapConstraintMessage(constraintName, lower);
        if (mapped != null) {
            return mapped;
        }

        if (lower.contains("duplicate key")) {
            return "Dữ liệu bị trùng. Vui lòng kiểm tra lại mã, email hoặc số điện thoại trước khi lưu.";
        }

        if (lower.contains("violates foreign key constraint")
                || lower.contains("update or delete")) {
            return "Không thể xóa vì dữ liệu này đang được sử dụng ở bảng khác. Constraint: "
                    + valueOrUnknown(constraintName)
                    + ".";
        }

        if (lower.contains("null value")) {
            return "Thiếu dữ liệu bắt buộc. Vui lòng kiểm tra các trường còn trống.";
        }

        return "Không thể thực hiện thao tác vì dữ liệu đang bị ràng buộc. Constraint: "
                + valueOrUnknown(constraintName)
                + ".";
    }

    private String mapConstraintMessage(String constraintName, String lower) {
        String value = constraintName == null ? "" : constraintName.toLowerCase(Locale.ROOT);

        if (value.contains("queue_machine_services") || value.contains("queue_machine_service")) {
            return "Không thể xóa vì dữ liệu đang có liên kết máy bốc số - dịch vụ. Hãy vào mục Gán dịch vụ và gỡ mapping trước.";
        }

        if (value.contains("counter_sessions")) {
            return "Không thể xóa vì quầy hoặc nhân viên đang có lịch sử ca làm. Hãy kết thúc ca đang hoạt động trước; nếu đã có lịch sử thì nên chuyển trạng thái ngừng hoạt động thay vì xóa.";
        }

        if (value.contains("service_histories")) {
            return "Không thể xóa vì dữ liệu đã phát sinh lịch sử giao dịch. Hãy giữ dữ liệu để phục vụ báo cáo hoặc chuyển sang trạng thái ngừng hoạt động.";
        }

        if (value.contains("tickets")) {
            return "Không thể xóa vì dữ liệu đang có phiếu giao dịch liên quan. Hãy hoàn tất hoặc hủy các phiếu trước, sau đó kiểm tra lịch sử giao dịch.";
        }

        if (value.contains("counters") || lower.contains("table \"counters\"")) {
            return "Không thể xóa vì máy bốc số/chi nhánh đang có quầy giao dịch liên quan. Hãy xóa hoặc ngừng hoạt động các quầy trước.";
        }

        if (value.contains("queue_machines") || lower.contains("table \"queue_machines\"")) {
            return "Không thể xóa vì chi nhánh/dịch vụ đang liên kết với máy bốc số. Hãy gỡ mapping, quầy và phiếu liên quan trước.";
        }

        if (value.contains("services") || lower.contains("table \"services\"")) {
            return "Không thể xóa vì dịch vụ đang được mapping hoặc đã phát sinh phiếu/lịch sử. Hãy gỡ mapping trước; nếu đã có lịch sử thì chuyển dịch vụ sang ngừng hoạt động.";
        }

        if (value.contains("users") || lower.contains("table \"users\"")) {
            return "Không thể xóa tài khoản vì vẫn còn dữ liệu đang tham chiếu đến tài khoản này.";
        }

        if (value.contains("branches") || lower.contains("table \"branches\"")) {
            return "Không thể xóa vì chi nhánh đang có dữ liệu liên quan. Hãy xóa/gỡ nhân viên, dịch vụ, máy bốc số, quầy, phiếu và lịch sử trước.";
        }

        if (value.contains("uk_services_branch_code")) {
            return "Mã dịch vụ đã tồn tại trong chi nhánh này.";
        }

        if (value.contains("uk_queue_machines_branch_code")) {
            return "Mã máy bốc số đã tồn tại trong chi nhánh này.";
        }

        if (value.contains("uk_counters_branch_code")) {
            return "Mã quầy đã tồn tại trong chi nhánh này.";
        }

        return null;
    }

    private String rootMessage(Throwable throwable) {
        Throwable current = throwable;
        while (current.getCause() != null) {
            current = current.getCause();
        }
        return current.getMessage() == null ? throwable.getMessage() : current.getMessage();
    }

    private String extractConstraintName(String message) {
        if (message == null) {
            return null;
        }

        Matcher matcher = CONSTRAINT_PATTERN.matcher(message);
        return matcher.find() ? matcher.group(1) : null;
    }

    private String valueOrUnknown(String value) {
        return value == null || value.isBlank() ? "không xác định" : value;
    }
    @ExceptionHandler(KeycloakUnavailableException.class)
    public ResponseEntity<Map<String, Object>> handleKeycloakUnavailable(KeycloakUnavailableException ex) {
        Map<String, Object> error = new HashMap<>();
        error.put("timestamp", LocalDateTime.now());
        error.put("status", HttpStatus.SERVICE_UNAVAILABLE.value());
        error.put("error", "Service Unavailable");
        error.put("message", "Dich vu dang nhap tam thoi gian doan. Vui long thu lai sau.");
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(error);
    }
}
