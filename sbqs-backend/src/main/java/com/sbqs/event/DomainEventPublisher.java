package com.sbqs.event;

import java.util.Map;

public interface DomainEventPublisher {

    void publish(
            String type,
            String aggregateType,
            String aggregateId,
            Long branchId,
            Map<String, Object> payload);
}
