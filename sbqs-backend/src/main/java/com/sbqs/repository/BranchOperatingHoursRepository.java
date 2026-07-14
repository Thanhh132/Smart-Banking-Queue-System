package com.sbqs.repository;

import com.sbqs.entity.BranchOperatingHours;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface BranchOperatingHoursRepository extends JpaRepository<BranchOperatingHours, Long> {
    List<BranchOperatingHours> findByBranchBranchIdOrderByDayOfWeek(Long branchId);
    Optional<BranchOperatingHours> findByBranchBranchIdAndDayOfWeek(Long branchId, Integer dayOfWeek);
}
