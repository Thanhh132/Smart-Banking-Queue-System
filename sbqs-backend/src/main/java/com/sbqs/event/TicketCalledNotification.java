package com.sbqs.event;

public record TicketCalledNotification(
        Long ticketId,
        String ticketNumber,
        String branchName,
        String serviceName,
        String queueMachineLocationNote,
        String counterName,
        String staffName) {
}
