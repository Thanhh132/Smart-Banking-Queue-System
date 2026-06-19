package com.sbqs.repository;

import com.sbqs.entity.Counter;
import com.sbqs.entity.CounterSession;
import com.sbqs.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CounterSessionRepository extends JpaRepository<CounterSession, Long> {

    Optional<CounterSession> findFirstByCounterAndStatusOrderByStartedAtDesc(
            Counter counter,
            String status);

    Optional<CounterSession> findFirstByStaffAndStatusOrderByStartedAtDesc(
            User staff,
            String status);

    List<CounterSession> findByBranchBranchIdOrderByStartedAtDesc(Long branchId);
}
