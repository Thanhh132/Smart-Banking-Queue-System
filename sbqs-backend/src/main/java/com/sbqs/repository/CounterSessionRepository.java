package com.sbqs.repository;

import com.sbqs.entity.CounterSession;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CounterSessionRepository extends JpaRepository<CounterSession, Long> {

    Optional<CounterSession> findFirstByCounterIdAndStatusOrderByStartedAtDesc(
            Long counterId,
            String status);

    Optional<CounterSession> findFirstByStaffIdAndStatusOrderByStartedAtDesc(
            Long staffId,
            String status);

    List<CounterSession> findByBranchIdOrderByStartedAtDesc(Long branchId);
}
