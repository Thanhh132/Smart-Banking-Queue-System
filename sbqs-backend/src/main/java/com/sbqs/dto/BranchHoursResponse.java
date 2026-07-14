package com.sbqs.dto;

import java.time.LocalTime;

public record BranchHoursResponse(
        Integer dayOfWeek, boolean closed,
        LocalTime morningOpen, LocalTime morningClose,
        LocalTime afternoonOpen, LocalTime afternoonClose) { }
