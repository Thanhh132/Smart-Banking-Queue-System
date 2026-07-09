package com.sbqs.dto;

import java.time.LocalDateTime;
import java.util.List;

public record TicketStaffViewResponse(
        Long ticketId,
        Integer ticketNumber,
        String status,
        String customerEmail,
        LocalDateTime servingStartedAt,
        ServiceSummary service,
        List<TicketPaperlessFieldResponse> paperlessFields,
        boolean hasPaperlessProfile) {

    public record ServiceSummary(
            Long serviceId,
            String serviceCode,
            String serviceName,
            String serviceType) {
    }
}
