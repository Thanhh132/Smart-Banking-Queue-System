package com.sbqs.service;

import com.sbqs.dto.QueueMonitorResponse;
import com.sbqs.dto.ServingCounterDTO;
import com.sbqs.entity.Branch;
import com.sbqs.entity.Counter;
import com.sbqs.repository.BranchRepository;
import com.sbqs.repository.CounterRepository;
import com.sbqs.repository.TicketRepository;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class QueueMonitorService {

    private final BranchRepository branchRepository;
    private final CounterRepository counterRepository;
    private final TicketRepository ticketRepository;

    public QueueMonitorService(
            BranchRepository branchRepository,
            CounterRepository counterRepository,
            TicketRepository ticketRepository) {

        this.branchRepository = branchRepository;
        this.counterRepository = counterRepository;
        this.ticketRepository = ticketRepository;
    }

    public QueueMonitorResponse getMonitor(Long branchId) {

        Branch branch = branchRepository.findById(branchId)
                .orElseThrow(() ->
                        new RuntimeException("Không tìm thấy chi nhánh"));

        List<Counter> counters =
                counterRepository.findByBranchBranchId(branchId);

        List<ServingCounterDTO> servingCounters =
                new ArrayList<>();

        for (Counter counter : counters) {

            if (counter.getCurrentTicket() != null) {

                servingCounters.add(
                        new ServingCounterDTO(
                                counter.getCounterName(),
                                counter.getCurrentTicket().getTicketNumber()
                        )
                );
            }
        }

        long waitingCount =
                ticketRepository.countByBranchBranchIdAndStatus(
                        branchId,
                        "WAITING"
                );

        QueueMonitorResponse response =
                new QueueMonitorResponse();

        response.setBranchName(branch.getBranchName());
        response.setServingCounters(servingCounters);
        response.setWaitingCount(waitingCount);

        return response;
    }
}