package com.sbqs.event;

public record TicketQueueThresholdNotification(
        Long ticketId,
        String customerEmail,
        Integer ticketNumber,
        long peopleAhead) { }
