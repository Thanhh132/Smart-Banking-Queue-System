package com.sbqs.event;

public record TicketQueueThresholdNotification(
        Long ticketId,
        Integer ticketNumber,
        long peopleAhead) { }
