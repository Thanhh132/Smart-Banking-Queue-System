package com.sbqs.service;

import com.sbqs.entity.Ticket;
import com.sbqs.repository.TicketRepository;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
public class QueueStatusService {

    private final TicketRepository ticketRepository;

    public QueueStatusService(TicketRepository ticketRepository) {
        this.ticketRepository = ticketRepository;
    }

    public Map<String, Object> getQueueStatus() {
        long waitingCount = ticketRepository.countByStatus("WAITING");

        Ticket currentServing =
                ticketRepository.findFirstByStatusOrderByTicketNumberAsc("SERVING");

        Map<String, Object> result = new HashMap<>();
        result.put("waitingCount", waitingCount);
        result.put("currentServing", currentServing);

        return result;
    }
}