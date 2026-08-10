package com.sbqs.service;

import com.sbqs.dto.HistoryResponse;
import com.sbqs.entity.History;
import com.sbqs.entity.Counter;
import com.sbqs.entity.Ticket;
import com.sbqs.entity.User;
import com.sbqs.repository.HistoryRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;

@Service("sbqsHistoryService")
public class HistoryService {

    private final HistoryRepository historyRepository;
    private final CurrentUserService currentUserService;

    public HistoryService(
            HistoryRepository historyRepository,
            CurrentUserService currentUserService) {

        this.historyRepository = historyRepository;
        this.currentUserService = currentUserService;
    }

    public List<HistoryResponse> getHistoryByBranch(Long branchId) {
        User currentUser = currentUserService.requireUser();

        if ("SUPER_ADMIN".equals(currentUser.getRole())) {
            return historyRepository.findTop200ByBranchIdOrderByCompletedAtDesc(branchId)
                    .stream()
                    .map(this::convertToResponse)
                    .toList();
        }

        currentUserService.requireBranch(branchId);
        return historyRepository.findTop200ByBranchIdOrderByCompletedAtDesc(branchId)
                .stream()
                .map(this::convertToResponse)
                .toList();
    }

    public void recordCompleted(Ticket ticket, Counter counter, User staff) {
        History history = snapshot(ticket);
        history.setCounterId(counter.getCounterId());
        history.setCounterName(counter.getCounterName());
        history.setStaffId(staff.getUserId());
        history.setStaffName(staff.getFullName());
        history.setStartedAt(ticket.getServingStartedAt());
        history.setStatus("COMPLETED");
        history.setStaffNote("Hoàn thành phục vụ khách hàng");
        historyRepository.save(history);
    }

    public void recordCancelled(Ticket ticket) {
        History history = snapshot(ticket);
        history.setStartedAt(ticket.getCreatedAt());
        history.setStatus("CANCELLED");
        history.setStaffNote("Khách hàng hủy phiếu trước khi được phục vụ");
        historyRepository.save(history);
    }

    public void recordMissed(Ticket ticket, Counter counter, User staff) {
        History history = snapshot(ticket);
        history.setCounterId(counter.getCounterId());
        history.setCounterName(counter.getCounterName());
        history.setStaffId(staff.getUserId());
        history.setStaffName(staff.getFullName());
        history.setStartedAt(ticket.getServingStartedAt());
        history.setStatus("MISSED");
        history.setStaffNote("Khách không đến quầy sau khi được gọi");
        historyRepository.save(history);
    }

    public List<HistoryResponse> getAllHistory() {
        return findRecentScopedHistory(currentUserService.requireUser())
                .stream()
                .map(this::convertToResponse)
                .toList();
    }

    /** Giới hạn lịch sử màn hình ở 200 bản ghi gần nhất để không tải toàn bộ bảng vào bộ nhớ. */
    private List<History> findRecentScopedHistory(User currentUser) {
        return switch (currentUser.getRole()) {
            case "SUPER_ADMIN" -> historyRepository.findTop200ByOrderByCompletedAtDesc();
            case "BRANCH_ADMIN" -> historyRepository.findTop200ByBranchIdOrderByCompletedAtDesc(
                    currentUserService.requireBranchId());
            case "STAFF" -> historyRepository.findTop200ByStaffIdOrderByCompletedAtDesc(currentUser.getUserId());
            case "CUSTOMER" -> historyRepository.findTop200ByCustomerEmailIgnoreCaseOrderByCompletedAtDesc(
                    currentUser.getEmail());
            default -> List.of();
        };
    }

    /** Lọc theo ngày nhưng vẫn áp dụng scope riêng của SUPER_ADMIN, admin, staff và customer. */
    public List<HistoryResponse> getHistoryByDateRange(LocalDate from, LocalDate to) {
        User currentUser = currentUserService.requireUser();
        LocalDateTime fromDateTime = from.atStartOfDay();
        LocalDateTime toDateTime = to.atTime(23, 59, 59);

        return findScopedHistoryBetween(currentUser, fromDateTime, toDateTime)
                .stream()
                .sorted(this::newestFirst)
                .map(this::convertToResponse)
                .toList();
    }

    public List<History> findScopedHistory(User currentUser) {
        return switch (currentUser.getRole()) {
            case "SUPER_ADMIN" -> historyRepository.findAll();
            case "BRANCH_ADMIN" -> historyRepository.findByBranchId(currentUserService.requireBranchId());
            case "STAFF" -> historyRepository.findByStaffId(currentUser.getUserId());
            case "CUSTOMER" -> historyRepository.findByCustomerEmailIgnoreCase(currentUser.getEmail());
            default -> List.of();
        };
    }

    private List<History> findScopedHistoryBetween(
            User currentUser,
            LocalDateTime fromDateTime,
            LocalDateTime toDateTime) {

        return switch (currentUser.getRole()) {
            case "SUPER_ADMIN" -> historyRepository.findByCompletedAtBetween(fromDateTime, toDateTime);
            case "BRANCH_ADMIN" -> historyRepository.findByBranchIdAndCompletedAtBetween(
                    currentUserService.requireBranchId(),
                    fromDateTime,
                    toDateTime);
            case "STAFF" -> historyRepository.findByStaffIdAndCompletedAtBetween(
                    currentUser.getUserId(),
                    fromDateTime,
                    toDateTime);
            case "CUSTOMER" -> historyRepository.findByCustomerEmailIgnoreCaseAndCompletedAtBetween(
                    currentUser.getEmail(),
                    fromDateTime,
                    toDateTime);
            default -> List.of();
        };
    }

    private HistoryResponse convertToResponse(History history) {
        HistoryResponse response = new HistoryResponse();
        response.setHistoryId(history.getHistoryId());
        response.setTicketNumber(history.getTicketNumber());
        response.setStartedAt(history.getStartedAt());
        response.setCompletedAt(history.getCompletedAt());
        response.setStaffNote(history.getStaffNote());
        response.setStatus(history.getStatus());

        response.setServiceName(history.getServiceName());
        response.setCounterName(history.getCounterName());
        response.setBranchName(history.getBranchName());
        response.setQueueMachineName(history.getQueueMachineName());
        response.setCustomerEmail(history.getCustomerEmail());
        response.setStaffName(history.getStaffName());
        response.setStaffId(history.getStaffId());

        return response;
    }

    /**
     * Sao chép tên và mã liên quan thay vì chỉ giữ khóa ngoại, bảo toàn nội dung báo
     * cáo ngay cả khi chi nhánh, dịch vụ hoặc máy bốc số được đổi tên sau này.
     */
    private History snapshot(Ticket ticket) {
        History history = new History();
        history.setTicketId(ticket.getTicketId());
        history.setBranchId(ticket.getBranch().getBranchId());
        history.setBranchName(ticket.getBranch().getBranchName());
        history.setQueueMachineId(ticket.getQueueMachine() == null
                ? null : ticket.getQueueMachine().getQueueMachineId());
        history.setQueueMachineName(ticket.getQueueMachine() == null
                ? null : ticket.getQueueMachine().getMachineName());
        history.setServiceId(ticket.getService().getServiceId());
        history.setServiceName(ticket.getService().getServiceName());
        history.setCustomerEmail(ticket.getCustomerEmail());
        history.setTicketNumber(ticket.getTicketNumber());
        history.setCompletedAt(LocalDateTime.now());
        return history;
    }

    private int newestFirst(History left, History right) {
        return Comparator
                .comparing(History::getCompletedAt, Comparator.nullsLast(Comparator.reverseOrder()))
                .compare(left, right);
    }
}
