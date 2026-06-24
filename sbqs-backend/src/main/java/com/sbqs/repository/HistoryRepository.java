package com.sbqs.repository;

import com.sbqs.entity.History;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.LocalDateTime;
import java.util.List;

public interface HistoryRepository extends JpaRepository<History, Long> {

    List<History> findByBranchId(Long branchId);

    List<History> findByCustomerEmailIgnoreCase(String customerEmail);

    List<History> findByStaffId(Long staffId);

    List<History> findByCompletedAtBetween(
        LocalDateTime from,
        LocalDateTime to);

    List<History> findByBranchIdAndCompletedAtBetween(
        Long branchId,
        LocalDateTime from,
        LocalDateTime to);

    List<History> findByStaffIdAndCompletedAtBetween(
        Long staffId,
        LocalDateTime from,
        LocalDateTime to);

    List<History> findByCustomerEmailIgnoreCaseAndCompletedAtBetween(
        String customerEmail,
        LocalDateTime from,
        LocalDateTime to);
}
