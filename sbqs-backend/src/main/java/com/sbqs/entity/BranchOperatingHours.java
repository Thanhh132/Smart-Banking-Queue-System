package com.sbqs.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.time.LocalTime;

@Entity
@Table(name = "branch_operating_hours", uniqueConstraints =
        @UniqueConstraint(name = "uk_branch_hours_day", columnNames = {"branch_id", "day_of_week"}))
@Getter @Setter @NoArgsConstructor
public class BranchOperatingHours {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "operating_hours_id")
    private Long operatingHoursId;

    @ManyToOne(optional = false) @JoinColumn(name = "branch_id", nullable = false)
    private Branch branch;

    @Column(name = "day_of_week", nullable = false)
    private Integer dayOfWeek;

    @Column(name = "closed", nullable = false)
    private boolean closed;

    @Column(name = "morning_open") private LocalTime morningOpen;
    @Column(name = "morning_close") private LocalTime morningClose;
    @Column(name = "afternoon_open") private LocalTime afternoonOpen;
    @Column(name = "afternoon_close") private LocalTime afternoonClose;
}
