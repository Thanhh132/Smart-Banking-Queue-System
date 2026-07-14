package com.sbqs.service;

import com.sbqs.entity.BranchOperatingHours;
import com.sbqs.repository.BranchOperatingHoursRepository;
import com.sbqs.repository.BranchRepository;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class BranchOperatingHoursServiceTest {
    private final BranchOperatingHoursRepository repository = mock(BranchOperatingHoursRepository.class);
    private final BranchOperatingHoursService service = new BranchOperatingHoursService(
            repository, mock(BranchRepository.class), mock(CurrentUserService.class));

    @Test
    void defaultScheduleClosesDuringLunchAndWeekend() {
        when(repository.findByBranchBranchIdAndDayOfWeek(1L, 1)).thenReturn(Optional.empty());
        when(repository.findByBranchBranchIdAndDayOfWeek(1L, 7)).thenReturn(Optional.empty());

        assertTrue(service.isOpen(1L, LocalDateTime.of(2026, 7, 13, 9, 0)));
        assertFalse(service.isOpen(1L, LocalDateTime.of(2026, 7, 13, 12, 30)));
        assertFalse(service.isOpen(1L, LocalDateTime.of(2026, 7, 19, 9, 0)));
    }

    @Test
    void configuredClosedDayOverridesDefault() {
        BranchOperatingHours hours = new BranchOperatingHours();
        hours.setDayOfWeek(2);
        hours.setClosed(true);
        when(repository.findByBranchBranchIdAndDayOfWeek(1L, 2)).thenReturn(Optional.of(hours));

        assertFalse(service.isOpen(1L, LocalDateTime.of(2026, 7, 14, 9, 0)));
    }

    @Test
    void configuredSplitShiftExcludesBreak() {
        BranchOperatingHours hours = new BranchOperatingHours();
        hours.setDayOfWeek(2);
        hours.setMorningOpen(LocalTime.of(7, 30));
        hours.setMorningClose(LocalTime.of(11, 30));
        hours.setAfternoonOpen(LocalTime.of(13, 30));
        hours.setAfternoonClose(LocalTime.of(16, 30));
        when(repository.findByBranchBranchIdAndDayOfWeek(1L, 2)).thenReturn(Optional.of(hours));

        assertTrue(service.isOpen(1L, LocalDateTime.of(2026, 7, 14, 8, 0)));
        assertFalse(service.isOpen(1L, LocalDateTime.of(2026, 7, 14, 12, 0)));
    }
}
