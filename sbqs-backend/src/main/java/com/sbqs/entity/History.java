package com.sbqs.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "service_histories")
@Getter
@Setter
@NoArgsConstructor
public class History {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "history_id")
    private Long historyId;

    @ManyToOne
    @JoinColumn(name = "ticket_id", nullable = false)
    private Ticket ticket;

    @ManyToOne
    @JoinColumn(name = "branch_id", nullable = false)
    private Branch branch;

    @ManyToOne
    @JoinColumn(name = "queue_machine_id")
    private QueueMachine queueMachine;

    @ManyToOne
    @JoinColumn(name = "counter_id")
    private Counter counter;

    @ManyToOne
    @JoinColumn(name = "service_id")
    private Services service;

    @Column(name = "ticket_number", nullable = false)
    private Integer ticketNumber;

    @Column(name = "started_at")
    private LocalDateTime startedAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @Column(name = "staff_note")
    private String staffNote;

    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();
}