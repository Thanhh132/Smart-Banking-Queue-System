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

    @Column(name = "counter_id", nullable = false)
    private Long counterId;

    @Column(name = "staff_id", nullable = false)
    private Long staffId;

    @Column(name = "branch_id", nullable = false)
    private Long branchId;

    @Column(name = "counter_name")
    private String counterName;

    @Column(name = "staff_name")
    private String staffName;

    @Column(name = "staff_email")
    private String staffEmail;

    @Column(name = "branch_name")
    private String branchName;

    @Column(name = "started_at", nullable = false)
    private LocalDateTime startedAt = LocalDateTime.now();

    @Column(name = "ended_at")
    private LocalDateTime endedAt;

    @Column(name = "status", nullable = false)
    private String status = "ACTIVE";
}
