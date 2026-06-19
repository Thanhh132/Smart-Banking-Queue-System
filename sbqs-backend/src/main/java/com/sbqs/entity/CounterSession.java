package com.sbqs.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "counter_sessions")
@Getter
@Setter
@NoArgsConstructor
public class CounterSession {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "counter_session_id")
    private Long counterSessionId;

    @ManyToOne
    @JoinColumn(name = "counter_id", nullable = false)
    private Counter counter;

    @ManyToOne
    @JoinColumn(name = "staff_id", nullable = false)
    private User staff;

    @ManyToOne
    @JoinColumn(name = "branch_id", nullable = false)
    private Branch branch;

    @Column(name = "started_at", nullable = false)
    private LocalDateTime startedAt = LocalDateTime.now();

    @Column(name = "ended_at")
    private LocalDateTime endedAt;

    @Column(name = "status", nullable = false)
    private String status = "ACTIVE";
}
