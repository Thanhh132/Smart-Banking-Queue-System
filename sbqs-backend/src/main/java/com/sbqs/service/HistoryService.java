package com.sbqs.service;

import com.sbqs.dto.HistoryResponse;
import com.sbqs.entity.History;
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
            return historyRepository.findByBranchId(branchId)
                    .stream()
                    .sorted(this::newestFirst)
                    .map(this::convertToResponse)
                    .toList();
        }

        currentUserService.requireBranch(branchId);
        return historyRepository.findByBranchId(branchId)
                .stream()
                .sorted(this::newestFirst)
                .map(this::convertToResponse)
                .toList();
    }

    public List<HistoryResponse> getAllHistory() {
        return findScopedHistory(currentUserService.requireUser())
                .stream()
                .sorted(this::newestFirst)
                .map(this::convertToResponse)
                .toList();
    }

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

        return response;
    }

    private int newestFirst(History left, History right) {
        return Comparator
                .comparing(History::getCompletedAt, Comparator.nullsLast(Comparator.reverseOrder()))
                .compare(left, right);
    }
}
