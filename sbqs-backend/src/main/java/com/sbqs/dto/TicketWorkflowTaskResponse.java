package com.sbqs.dto;

import java.time.LocalDateTime;

public record TicketWorkflowTaskResponse(
        String taskId,
        String taskName,
        Long ticketId,
        Integer ticketNumber,
        String branchName,
        String serviceName,
        String queueMachineName,
        String customerEmail,
        String status,
        LocalDateTime createdAt) {
}
