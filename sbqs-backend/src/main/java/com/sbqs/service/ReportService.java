package com.sbqs.service;

import com.sbqs.dto.report.HistoryReportRow;
import com.sbqs.dto.report.ReportDocument;
import com.sbqs.dto.report.ReportFormat;
import com.sbqs.dto.report.ServiceReportRow;
import com.sbqs.dto.report.TicketReportRow;
import com.sbqs.dto.report.UserReportRow;
import com.sbqs.entity.History;
import com.sbqs.entity.Services;
import com.sbqs.entity.Ticket;
import com.sbqs.entity.User;
import com.sbqs.repository.ServiceRepository;
import com.sbqs.repository.TicketRepository;
import com.sbqs.repository.UserRepository;
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
    private final UserRepository userRepository;
    private final ServiceRepository serviceRepository;
    private final TicketRepository ticketRepository;
    private final HistoryService historyService;

    public ReportService(
            JasperReportService jasperReportService,
            CurrentUserService currentUserService,
            UserRepository userRepository,
            ServiceRepository serviceRepository,
            TicketRepository ticketRepository,
            HistoryService historyService) {

        this.jasperReportService = jasperReportService;
        this.currentUserService = currentUserService;
        this.userRepository = userRepository;
        this.serviceRepository = serviceRepository;
        this.ticketRepository = ticketRepository;
        this.historyService = historyService;
    }

    public ReportDocument exportUsers(ReportFormat format) {
        User currentUser = currentUserService.requireUser();
        requireAdminReportAccess(currentUser);

        List<User> users = isSuperAdmin(currentUser)
                ? userRepository.findAll()
                : userRepository.findByBranch(currentUser.getBranch());
        List<UserReportRow> rows = users.stream()
                .sorted(Comparator.comparing(User::getCreatedAt, Comparator.nullsLast(Comparator.reverseOrder())))
                .map(user -> new UserReportRow(
                        value(user.getFullName()),
                        value(user.getEmail()),
                        value(user.getPhone()),
                        roleLabel(user.getRole()),
                        user.getBranch() == null ? "Toàn hệ thống" : value(user.getBranch().getBranchName()),
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

        List<Services> services = isSuperAdmin(currentUser)
                ? serviceRepository.findAll()
                : serviceRepository.findByBranch(currentUser.getBranch());
        List<ServiceReportRow> rows = services.stream()
                .sorted(Comparator.comparing(Services::getServiceCode))
                .map(service -> new ServiceReportRow(
                        value(service.getServiceCode()),
                        value(service.getServiceName()),
                        value(service.getServiceType()),
                        service.getEstimatedTime(),
                        value(service.getBranch().getBranchName()),
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

        List<Ticket> tickets = isSuperAdmin(currentUser)
                ? ticketRepository.findAll()
                : ticketRepository.findByBranch(currentUser.getBranch());
        List<TicketReportRow> rows = tickets.stream()
                .sorted(Comparator.comparing(Ticket::getCreatedAt, Comparator.nullsLast(Comparator.reverseOrder())))
                .map(ticket -> new TicketReportRow(
                        ticket.getTicketNumber(),
                        value(ticket.getCustomerEmail()),
                        ticket.getService() == null ? "-" : value(ticket.getService().getServiceName()),
                        ticket.getQueueMachine() == null ? "-" : value(ticket.getQueueMachine().getMachineName()),
                        ticket.getBranch() == null ? "-" : value(ticket.getBranch().getBranchName()),
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

    public ReportDocument exportHistory(ReportFormat format) {
        User currentUser = currentUserService.requireUser();
        List<HistoryReportRow> rows = historyService.findScopedHistory(currentUser)
                .stream()
                .sorted(Comparator.comparing(History::getCompletedAt, Comparator.nullsLast(Comparator.reverseOrder())))
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
        return "ACTIVE".equals(status) ? "Hoạt động" : "Tạm khóa";
    }

    private String ticketStatusLabel(String status) {
        return switch (value(status)) {
            case "WAITING" -> "Đang chờ";
            case "SERVING" -> "Đang phục vụ";
            case "COMPLETED" -> "Hoàn thành";
            case "CANCELLED" -> "Đã hủy";
            default -> value(status);
        };
    }

    private void requireAdminReportAccess(User user) {
        if (!isSuperAdmin(user) && !"BRANCH_ADMIN".equals(user.getRole())) {
            throw new RuntimeException("Bạn không có quyền xuất báo cáo này");
        }
    }
}
