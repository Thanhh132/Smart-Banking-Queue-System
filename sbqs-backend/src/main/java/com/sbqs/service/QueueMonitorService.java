package com.sbqs.service;

import com.sbqs.dto.QueueMonitorResponse;
import com.sbqs.dto.ServingCounterDTO;
import com.sbqs.entity.Branch;
import com.sbqs.entity.Counter;
import com.sbqs.repository.BranchRepository;
import com.sbqs.repository.CounterRepository;
import com.sbqs.repository.CounterSessionRepository;
import com.sbqs.repository.TicketRepository;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class QueueMonitorService {

    private final BranchRepository branchRepository;
    private final CounterRepository counterRepository;
    private final TicketRepository ticketRepository;
    private final CurrentUserService currentUserService;
    private final CounterSessionRepository counterSessionRepository;

    public QueueMonitorService(
            BranchRepository branchRepository,
            CounterRepository counterRepository,
            TicketRepository ticketRepository,
            CurrentUserService currentUserService,
            CounterSessionRepository counterSessionRepository) {

        this.branchRepository = branchRepository;
        this.counterRepository = counterRepository;
        this.ticketRepository = ticketRepository;
        this.currentUserService = currentUserService;
        this.counterSessionRepository = counterSessionRepository;
    }

    public QueueMonitorResponse getMonitor(Long branchId) {
        return getMonitor(branchId, null);
    }

    @Cacheable(
            cacheNames = "queueMonitor",
            key = "'branch:' + #branchId + ':machine:' + (#queueMachineId == null ? 'all' : #queueMachineId)")
    /** Tổng hợp hàng đợi và trạng thái quầy cho màn hình monitor theo chi nhánh/máy bốc số. */
    public QueueMonitorResponse getMonitor(Long branchId, Long queueMachineId) {
        if (!"CUSTOMER".equals(currentUserService.requireUser().getRole())) {
            currentUserService.requireBranch(branchId);
        }

        Branch branch = branchRepository.findById(branchId)
                .orElseThrow(() -> new RuntimeException("Khong tim thay chi nhanh"));

        List<Counter> counters = queueMachineId == null
                ? counterRepository.findByBranchBranchId(branchId)
                : counterRepository.findByBranchBranchIdAndQueueMachineQueueMachineId(
                        branchId,
                        queueMachineId);
        List<ServingCounterDTO> servingCounters = new ArrayList<>();

        for (Counter counter : counters) {
            String status = getMonitorStatus(counter);
            String staffName = counterSessionRepository
                    .findFirstByCounterIdAndStatusOrderByStartedAtDesc(counter.getCounterId(), "ACTIVE")
                    .map(session -> session.getStaffName())
                    .orElse(null);

            servingCounters.add(
                    new ServingCounterDTO(
                            counter.getCounterName(),
                            counter.getCurrentTicket() == null
                                    ? null
                                    : counter.getCurrentTicket().getTicketNumber(),
                            status,
                            counter.getQueueMachine() == null
                                    ? null
                                    : counter.getQueueMachine().getMachineName(),
                            staffName));
        }

        long waitingCount = queueMachineId == null
                ? ticketRepository.countByBranchBranchIdAndStatus(branchId, "WAITING")
                : ticketRepository.countByQueueMachineQueueMachineIdAndStatus(queueMachineId, "WAITING");

        QueueMonitorResponse response = new QueueMonitorResponse();
        response.setBranchName(branch.getBranchName());
        response.setServingCounters(servingCounters);
        response.setWaitingCount(waitingCount);

        return response;
    }

    private String getMonitorStatus(Counter counter) {
        if (!"ACTIVE".equalsIgnoreCase(counter.getStatus())) {
            return "INACTIVE";
        }

        return counter.getCurrentTicket() == null ? "IDLE" : "SERVING";
    }
}
