package com.sbqs.repository;

import com.sbqs.entity.Counter;
import com.sbqs.entity.Branch;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import jakarta.persistence.LockModeType;

@Repository
public interface CounterRepository extends JpaRepository<Counter, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select c from Counter c where c.counterId = :counterId")
    Optional<Counter> findByIdForUpdate(@Param("counterId") Long counterId);

    Optional<Counter> findFirstByCurrentTicketTicketId(Long ticketId);

    List<Counter> findByBranch(Branch branch);

    List<Counter> findByStatus(String status);

    @EntityGraph(attributePaths = {"currentTicket", "queueMachine"})
    List<Counter> findByBranchBranchId(Long branchId);

    @EntityGraph(attributePaths = {"currentTicket", "queueMachine"})
    List<Counter> findByBranchBranchIdAndQueueMachineQueueMachineId(
            Long branchId,
            Long queueMachineId);

    boolean existsByBranchAndCounterCode(Branch branch, String counterCode);

    boolean existsByBranchAndCounterCodeAndCounterIdNot(
            Branch branch,
            String counterCode,
            Long counterId);
}
