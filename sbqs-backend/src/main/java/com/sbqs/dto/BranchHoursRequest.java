package com.sbqs.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.time.LocalTime;

public record BranchHoursRequest(
        @NotNull @Min(1) @Max(7) Integer dayOfWeek,
        boolean closed,
        LocalTime morningOpen,
        LocalTime morningClose,
        LocalTime afternoonOpen,
        LocalTime afternoonClose) { }
