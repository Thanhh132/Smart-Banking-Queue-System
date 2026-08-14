package com.sbqs.repository;

import com.sbqs.entity.TicketOutboxEvent;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

import java.time.LocalDateTime;
import java.util.List;

public interface TicketOutboxEventRepository extends JpaRepository<TicketOutboxEvent, Long> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    List<TicketOutboxEvent> findTop20ByStatusAndAvailableAtLessThanEqualOrderByCreatedAtAsc(
            String status,
            LocalDateTime availableAt);
}
