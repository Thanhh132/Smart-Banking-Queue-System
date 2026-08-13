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

    @Column(name = "ticket_id")
    private Long ticketId;

    @Column(name = "branch_id", nullable = false)
    private Long branchId;

    @Column(name = "queue_machine_id")
    private Long queueMachineId;

    @Column(name = "counter_id")
    private Long counterId;

    @Column(name = "service_id")
    private Long serviceId;

    @Column(name = "staff_id")
    private Long staffId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id")
    private User customer;

    /** Email snapshot captured when the history row is created. Never used for ownership. */
    @Column(name = "customer_email")
    private String customerEmail;

    @Column(name = "branch_name")
    private String branchName;

    @Column(name = "queue_machine_name")
    private String queueMachineName;

    @Column(name = "counter_name")
    private String counterName;

    @Column(name = "service_name")
    private String serviceName;

    @Column(name = "staff_name")
    private String staffName;

    @Column(name = "ticket_number", nullable = false)
    private Integer ticketNumber;

    @Column(name = "started_at")
    private LocalDateTime startedAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @Column(name = "staff_note")
    private String staffNote;

    @Column(name = "status")
    private String status;

    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();
}
