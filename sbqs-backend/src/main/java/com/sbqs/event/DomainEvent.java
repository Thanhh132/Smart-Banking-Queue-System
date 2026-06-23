package com.sbqs.event;

import java.time.LocalDateTime;
import java.util.Map;

public record DomainEvent(
        String eventId,
        String type,
        String aggregateType,
        String aggregateId,
        Long branchId,
        String actorEmail,
        LocalDateTime occurredAt,
        Map<String, Object> payload) {
}
