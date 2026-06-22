package com.sbqs.repository;

import com.sbqs.entity.History;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.LocalDateTime;
import java.util.List;

public interface HistoryRepository extends JpaRepository<History, Long> {

    List<History> findByBranchBranchId(Long branchId);
    List<History> findByCompletedAtBetween(
        LocalDateTime from,
        LocalDateTime to);

    List<History> findByBranchBranchIdAndCompletedAtBetween(
        Long branchId,
        LocalDateTime from,
        LocalDateTime to);
}
