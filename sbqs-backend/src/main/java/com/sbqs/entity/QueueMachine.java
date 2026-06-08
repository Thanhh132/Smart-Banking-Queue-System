package com.sbqs.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "queue_machines")
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

    @Column(name = "machine_code", nullable = false, unique = true)
    private String machineCode;

    @Column(name = "machine_name", nullable = false)
    private String machineName;

    @Column(name = "location_note")
    private String locationNote;

    @Column(name = "instruction_note")
    private String instructionNote;

    @Column(name = "status", nullable = false)
    private String status = "ACTIVE";

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();
}