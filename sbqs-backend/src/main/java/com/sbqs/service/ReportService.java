package com.sbqs.service;

import com.sbqs.dto.report.HistoryReportRow;
import com.sbqs.dto.report.ReportDocument;
import com.sbqs.dto.report.ReportFormat;
import com.sbqs.dto.report.ServiceReportRow;
import com.sbqs.dto.report.TicketReportRow;
import com.sbqs.dto.report.UserReportRow;
import com.sbqs.entity.History;
import com.sbqs.entity.User;
import com.sbqs.mapper.ReportQueryMapper;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

@Service
public class ReportService {
    private static final DateTimeFormatter DATE_TIME_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private final JasperReportService jasperReportService;
    private final CurrentUserService currentUserService;
    private final ReportQueryMapper reportQueryMapper;
    private final HistoryService historyService;

    public ReportService(
            JasperReportService jasperReportService,
            CurrentUserService currentUserService,
            ReportQueryMapper reportQueryMapper,
            HistoryService historyService) {

        this.jasperReportService = jasperReportService;
        this.currentUserService = currentUserService;
        this.reportQueryMapper = reportQueryMapper;
        this.historyService = historyService;
    }

    /** Xuất báo cáo người dùng theo phạm vi hệ thống hoặc đúng chi nhánh của admin hiện tại. */
    public ReportDocument exportUsers(ReportFormat format) {
        User currentUser = currentUserService.requireUser();
        requireAdminReportAccess(currentUser);

        List<UserReportRow> rows = reportQueryMapper.findUsersForReport(reportBranchId(currentUser))
                .stream()
                .map(user -> new UserReportRow(
                        value(user.getFullName()),
                        value(user.getEmail()),
                        value(user.getPhone()),
                        roleLabel(user.getRole()),
                        user.getBranchName() == null ? "Toàn hệ thống" : value(user.getBranchName()),
                        statusLabel(user.getStatus()),
                        formatDate(user.getCreatedAt())))
                .toList();

        return jasperReportService.export(
                "users-report",
                "sbqs-users-report",
                parameters("BÁO CÁO NGƯỜI DÙNG", scopeLabel(currentUser), rows.size()),
                rows,
                format);
    }

    public ReportDocument exportServices(ReportFormat format) {
        User currentUser = currentUserService.requireUser();
        requireAdminReportAccess(currentUser);

        List<ServiceReportRow> rows = reportQueryMapper.findServicesForReport(reportBranchId(currentUser))
                .stream()
                .map(service -> new ServiceReportRow(
                        value(service.getServiceCode()),
                        value(service.getServiceName()),
                        value(service.getServiceType()),
                        service.getEstimatedTime(),
                        value(service.getBranchName()),
                        statusLabel(service.getStatus())))
                .toList();

        return jasperReportService.export(
                "services-report",
                "sbqs-services-report",
                parameters("BÁO CÁO DỊCH VỤ", scopeLabel(currentUser), rows.size()),
                rows,
                format);
    }

    public ReportDocument exportTickets(ReportFormat format) {
        User currentUser = currentUserService.requireUser();
        requireAdminReportAccess(currentUser);

        List<TicketReportRow> rows = reportQueryMapper.findTicketsForReport(reportBranchId(currentUser))
                .stream()
                .map(ticket -> new TicketReportRow(
                        ticket.getTicketNumber(),
                        value(ticket.getCustomerEmail()),
                        value(ticket.getServiceName()),
                        value(ticket.getQueueMachineName()),
                        value(ticket.getBranchName()),
                        ticketStatusLabel(ticket.getStatus()),
                        formatDate(ticket.getCreatedAt())))
                .toList();

        return jasperReportService.export(
                "tickets-report",
                "sbqs-tickets-report",
                parameters("BÁO CÁO PHIẾU", scopeLabel(currentUser), rows.size()),
                rows,
                format);
    }

    /** Xuất lịch sử giao dịch đã snapshot, phù hợp đối soát ngay cả khi dữ liệu gốc đổi tên. */
    public ReportDocument exportHistory(ReportFormat format) {
        User currentUser = currentUserService.requireUser();
        List<HistoryReportRow> rows = historyService.findScopedHistory(currentUser)
                .stream()
                .sorted(Comparator.comparing(
                        History::getCompletedAt,
                        Comparator.nullsLast(Comparator.reverseOrder())))
                .map(history -> new HistoryReportRow(
                        history.getTicketNumber(),
                        value(history.getCustomerEmail()),
                        value(history.getStaffName()),
                        value(history.getServiceName()),
                        value(history.getCounterName()),
                        value(history.getBranchName()),
                        ticketStatusLabel(history.getStatus()),
                        formatDate(history.getStartedAt()),
                        formatDate(history.getCompletedAt())))
                .toList();

        return jasperReportService.export(
                "history-report",
                "sbqs-history-report",
                parameters("BÁO CÁO LỊCH SỬ GIAO DỊCH", scopeLabel(currentUser), rows.size()),
                rows,
                format);
    }

    private Map<String, Object> parameters(String title, String scope, int totalRecords) {
        return Map.of(
                "REPORT_TITLE", title,
                "REPORT_SCOPE", scope,
                "GENERATED_AT", DATE_TIME_FORMAT.format(LocalDateTime.now()),
                "TOTAL_RECORDS", totalRecords);
    }

    private String scopeLabel(User user) {
        return switch (user.getRole()) {
            case "SUPER_ADMIN" -> "Phạm vi: Toàn hệ thống";
            case "BRANCH_ADMIN" -> "Chi nhánh: " + user.getBranch().getBranchName();
            case "STAFF" -> "Nhân viên: " + user.getFullName();
            case "CUSTOMER" -> "Khách hàng: " + user.getEmail();
            default -> "Phạm vi: Tài khoản hiện tại";
        };
    }

    private boolean isSuperAdmin(User user) {
        return "SUPER_ADMIN".equals(user.getRole());
    }

    private Long reportBranchId(User user) {
        return isSuperAdmin(user) ? null : user.getBranch().getBranchId();
    }

    private String formatDate(LocalDateTime value) {
        return value == null ? "-" : DATE_TIME_FORMAT.format(value);
    }

    private String value(String value) {
        return value == null || value.isBlank() ? "-" : value;
    }

    private String roleLabel(String role) {
        return switch (value(role)) {
            case "SUPER_ADMIN" -> "Super Admin";
            case "BRANCH_ADMIN" -> "Quản trị chi nhánh";
            case "STAFF" -> "Nhân viên";
            case "CUSTOMER" -> "Khách hàng";
            default -> value(role);
        };
    }

    private String statusLabel(String status) {
        return "ACTIVE".equals(status) ? "Hoạt động" : "Không hoạt động";
    }

    private String ticketStatusLabel(String status) {
        return switch (value(status)) {
            case "WAITING" -> "Đang chờ";
            case "SERVING" -> "Đang phục vụ";
            case "COMPLETED" -> "Hoàn thành";
            case "CANCELLED" -> "Đã hủy";
            case "MISSED" -> "Khách không đến";
            default -> value(status);
        };
    }

    /** Chỉ role quản trị được xuất báo cáo tổng hợp có dữ liệu của nhiều người dùng. */
    private void requireAdminReportAccess(User user) {
        if (!isSuperAdmin(user) && !"BRANCH_ADMIN".equals(user.getRole())) {
            throw new RuntimeException("Bạn không có quyền xuất báo cáo này");
        }
    }
}
