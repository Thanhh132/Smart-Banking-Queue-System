package com.sbqs.service;

import com.sbqs.dto.CreateStaffRequest;
import com.sbqs.dto.bulkimport.ImportError;
import com.sbqs.dto.bulkimport.ImportResult;
import com.sbqs.dto.bulkimport.ServiceImportRow;
import com.sbqs.dto.bulkimport.StaffImportRow;
import com.sbqs.entity.Branch;
import com.sbqs.entity.Services;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Service
public class BulkImportService {
    private static final long MAX_FILE_SIZE = 5 * 1024 * 1024;

    private final ExcelImportParser parser;
    private final UserService userService;
    private final ServicesService servicesService;
    private final CurrentUserService currentUserService;

    public BulkImportService(
            ExcelImportParser parser,
            UserService userService,
            ServicesService servicesService,
            CurrentUserService currentUserService) {

        this.parser = parser;
        this.userService = userService;
        this.servicesService = servicesService;
        this.currentUserService = currentUserService;
    }

    /** Import nhân viên theo từng dòng; dòng lỗi được thu thập riêng thay vì hủy toàn bộ file. */
    public ImportResult importStaff(MultipartFile file) {
        validateFile(file);
        List<StaffImportRow> rows = readStaff(file);
        List<ImportError> errors = new ArrayList<>();
        int successCount = 0;

        for (StaffImportRow row : rows) {
            try {
                validateStaffRow(row);
                CreateStaffRequest request = new CreateStaffRequest();
                request.setFullName(row.fullName());
                request.setEmail(row.email().toLowerCase(Locale.ROOT));
                request.setPhone(row.phone());
                request.setPassword(row.password());
                request.setConfirmPassword(row.password());
                userService.createStaff(request);
                successCount++;
            } catch (RuntimeException ex) {
                errors.add(error(row.rowNumber(), row.email(), ex));
            }
        }

        return result(rows.size(), successCount, errors);
    }

    /** Import danh mục dịch vụ trong phạm vi chi nhánh của admin và trả báo cáo dòng lỗi. */
    public ImportResult importServices(MultipartFile file) {
        validateFile(file);
        List<ServiceImportRow> rows = readServices(file);
        List<ImportError> errors = new ArrayList<>();
        Branch branch = currentUserService.requireUser().getBranch();
        int successCount = 0;

        if (branch == null) {
            throw new RuntimeException("Tài khoản chưa được gán chi nhánh");
        }

        for (ServiceImportRow row : rows) {
            try {
                Services service = toService(row, branch);
                servicesService.createService(service);
                successCount++;
            } catch (RuntimeException ex) {
                errors.add(error(row.rowNumber(), row.serviceCode(), ex));
            }
        }

        return result(rows.size(), successCount, errors);
    }

    public byte[] staffTemplate() {
        return parser.createStaffTemplate();
    }

    public byte[] serviceTemplate() {
        return parser.createServiceTemplate();
    }

    private Services toService(ServiceImportRow row, Branch branch) {
        requireText(row.serviceCode(), "Mã dịch vụ");
        requireText(row.serviceName(), "Tên dịch vụ");
        requireText(row.serviceType(), "Loại dịch vụ");

        int estimatedTime;
        try {
            estimatedTime = Integer.parseInt(row.estimatedTime());
        } catch (NumberFormatException ex) {
            throw new RuntimeException("Thời gian xử lý phải là số nguyên dương");
        }
        if (estimatedTime <= 0) {
            throw new RuntimeException("Thời gian xử lý phải lớn hơn 0");
        }

        String status = row.status().isBlank() ? "ACTIVE" : row.status().toUpperCase(Locale.ROOT);
        if (!List.of("ACTIVE", "INACTIVE").contains(status)) {
            throw new RuntimeException("Trạng thái chỉ nhận ACTIVE hoặc INACTIVE");
        }

        Services service = new Services();
        service.setServiceCode(row.serviceCode().toUpperCase(Locale.ROOT));
        service.setServiceName(row.serviceName());
        service.setServiceType(row.serviceType().toUpperCase(Locale.ROOT));
        service.setDescription(row.description());
        service.setEstimatedTime(estimatedTime);
        service.setStatus(status);
        service.setBranch(branch);
        return service;
    }

    private void validateStaffRow(StaffImportRow row) {
        requireText(row.fullName(), "Họ tên");
        requireText(row.email(), "Email");
        requireText(row.phone(), "Số điện thoại");
        requireText(row.password(), "Mật khẩu");
        if (!row.email().matches("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$")) {
            throw new RuntimeException("Email không đúng định dạng");
        }
        if (!row.phone().matches("^[0-9]{10,11}$")) {
            throw new RuntimeException("Số điện thoại phải có 10-11 chữ số");
        }
    }

    /** Chặn file rỗng/sai định dạng trước khi parser đọc nội dung Excel. */
    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new RuntimeException("Vui lòng chọn file Excel cần nhập");
        }
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new RuntimeException("File Excel không được lớn hơn 5 MB");
        }
        String name = file.getOriginalFilename();
        if (name == null || !name.toLowerCase(Locale.ROOT).endsWith(".xlsx")) {
            throw new RuntimeException("Chỉ hỗ trợ file Excel định dạng .xlsx");
        }
    }

    private List<StaffImportRow> readStaff(MultipartFile file) {
        try {
            return parser.parseStaff(file.getInputStream());
        } catch (IOException ex) {
            throw new RuntimeException("Không đọc được file Excel", ex);
        }
    }

    private List<ServiceImportRow> readServices(MultipartFile file) {
        try {
            return parser.parseServices(file.getInputStream());
        } catch (IOException ex) {
            throw new RuntimeException("Không đọc được file Excel", ex);
        }
    }

    private ImportResult result(int total, int success, List<ImportError> errors) {
        return new ImportResult(total, success, errors.size(), List.copyOf(errors));
    }

    private ImportError error(int row, String identifier, RuntimeException ex) {
        String message = ex.getMessage() == null || ex.getMessage().isBlank()
                ? "Không thể nhập dòng dữ liệu này"
                : friendlyMessage(ex.getMessage());
        return new ImportError(row, identifier == null ? "" : identifier, message);
    }

    private String friendlyMessage(String message) {
        String normalized = message.toLowerCase(Locale.ROOT);
        if (normalized.contains("so dien thoai da ton tai")) {
            return "Số điện thoại đã tồn tại. Vui lòng sử dụng số khác";
        }
        if (normalized.contains("email da ton tai")) {
            return "Email đã tồn tại. Vui lòng sử dụng email khác";
        }
        return message;
    }

    private void requireText(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new RuntimeException(fieldName + " không được để trống");
        }
    }
}
