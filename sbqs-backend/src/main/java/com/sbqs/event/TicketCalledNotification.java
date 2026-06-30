package com.sbqs.event;

public record TicketCalledNotification(
        String customerEmail,
        String ticketNumber,
        String branchName,
        String serviceName,
        String queueMachineLocationNote,
        String counterName,
        String staffName) {
}
