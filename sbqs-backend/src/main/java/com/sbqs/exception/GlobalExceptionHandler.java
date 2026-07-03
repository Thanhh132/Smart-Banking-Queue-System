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

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<Map<String, Object>> handleDataIntegrityException(
            DataIntegrityViolationException ex) {

        return badRequest(resolveDataIntegrityMessage(ex));
    }

    private ResponseEntity<Map<String, Object>> badRequest(String message) {
        Map<String, Object> error = new HashMap<>();
        error.put("timestamp", LocalDateTime.now());
        error.put("status", HttpStatus.BAD_REQUEST.value());
        error.put("error", "Bad Request");
        error.put("message", message == null || message.isBlank()
                ? "Không thể thực hiện thao tác này."
                : message);

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
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
            return "Không thể xóa vì tài khoản đang có dữ liệu liên quan như ca làm hoặc lịch sử phục vụ. Hãy khóa tài khoản thay vì xóa hẳn.";
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
