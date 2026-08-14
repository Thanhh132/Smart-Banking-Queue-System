package com.sbqs.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "ticket_outbox_events")
@Getter
@Setter
@NoArgsConstructor
public class TicketOutboxEvent {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "outbox_id")
    private Long outboxId;

    @Column(name = "ticket_id", nullable = false)
    private Long ticketId;

    @Column(name = "event_type", nullable = false, length = 50)
    private String eventType;

    @Column(name = "status", nullable = false, length = 20)
    private String status = "PENDING";

    @Column(name = "attempts", nullable = false)
    private Integer attempts = 0;

    @Column(name = "available_at", nullable = false)
    private LocalDateTime availableAt = LocalDateTime.now();

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "processed_at")
    private LocalDateTime processedAt;

    @Column(name = "last_error", length = 1000)
    private String lastError;
}
