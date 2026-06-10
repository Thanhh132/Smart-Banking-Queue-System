package com.sbqs.service;

import com.sbqs.dto.HistoryResponse;
import com.sbqs.entity.History;
import com.sbqs.repository.HistoryRepository;
import org.springframework.stereotype.Service;
import java.time.LocalDate;
import java.time.LocalDateTime;

import java.util.List;

@Service
public class HistoryService {

    private final HistoryRepository historyRepository;

    public HistoryService(
            HistoryRepository historyRepository) {

        this.historyRepository = historyRepository;
    }

    public List<HistoryResponse> getHistoryByBranch(Long branchId) {
        return historyRepository.findByBranchBranchId(branchId)
                .stream()
                .map(this::convertToResponse)
                .toList();
    }

    public List<HistoryResponse> getAllHistory() {

        return historyRepository.findAll()
                .stream()
                .map(this::convertToResponse)
                .toList();
    }

    public List<HistoryResponse> getHistoryByDateRange(
            LocalDate from,
            LocalDate to) {

        LocalDateTime fromDateTime = from.atStartOfDay();

        LocalDateTime toDateTime = to.atTime(23, 59, 59);

        return historyRepository
                .findByCompletedAtBetween(
                        fromDateTime,
                        toDateTime)
                .stream()
                .map(this::convertToResponse)
                .toList();
    }

    private HistoryResponse convertToResponse(
            History history) {

        HistoryResponse response = new HistoryResponse();

        response.setHistoryId(
                history.getHistoryId());

        response.setTicketNumber(
                history.getTicketNumber());

        response.setStartedAt(
                history.getStartedAt());

        response.setCompletedAt(
                history.getCompletedAt());

        response.setStaffNote(
                history.getStaffNote());

        if (history.getService() != null) {
            response.setServiceName(
                    history.getService().getServiceName());
        }

        if (history.getCounter() != null) {
            response.setCounterName(
                    history.getCounter().getCounterName());
        }

        return response;
    }
}