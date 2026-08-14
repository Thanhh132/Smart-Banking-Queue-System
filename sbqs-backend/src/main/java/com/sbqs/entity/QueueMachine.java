package com.sbqs.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(
        name = "queue_machines",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_queue_machines_branch_code",
                columnNames = {"branch_id", "machine_code"}))
@Getter
@Setter
@NoArgsConstructor
public class QueueMachine {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "queue_machine_id")
    private Long queueMachineId;

    @ManyToOne
    @JoinColumn(name = "branch_id", nullable = false)
    private Branch branch;

    @Column(name = "machine_code", nullable = false)
    private String machineCode;

    @Column(name = "machine_name", nullable = false)
    private String machineName;

    @Column(name = "location_note")
    private String locationNote;

    @Column(name = "instruction_note")
    private String instructionNote;

    @Column(name = "status", nullable = false)
    private String status = "ACTIVE";

    @Column(name = "last_ticket_number", nullable = false)
    private Integer lastTicketNumber = 0;

    @Column(name = "last_ticket_date")
    private LocalDate lastTicketDate;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();
}
