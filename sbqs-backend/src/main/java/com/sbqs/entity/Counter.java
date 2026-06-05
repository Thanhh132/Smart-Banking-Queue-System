package com.sbqs.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "counters")
@Getter
@Setter
@NoArgsConstructor
public class Counter {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "counter_id")
    private Long counterId;

    @Column(name = "counter_code", nullable = false, unique = true)
    private String counterCode;

    @Column(name = "counter_name", nullable = false)
    private String counterName;

    @ManyToOne
    @JoinColumn(name = "branch_id", nullable = false)
    private Branch branch; // liên kết Counter với Branch

    @Column(name = "status", nullable = false)
    private String status = "ACTIVE";
}