package com.sbqs.dto;

import java.time.LocalDateTime;

public record TicketTrackingResponse(
        Long ticketId,
        Integer ticketNumber,
        String status,
        long peopleAhead,
        String counterName,
        String branchName,
        String serviceName,
        Long queueMachineId,
        String queueMachineLocationNote,
        LocalDateTime servingStartedAt) {
}
