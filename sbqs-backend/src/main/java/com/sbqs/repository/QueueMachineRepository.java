package com.sbqs.repository;

import com.sbqs.entity.Branch;
import com.sbqs.entity.QueueMachine;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

import jakarta.persistence.LockModeType;

@Repository
public interface QueueMachineRepository
        extends JpaRepository<QueueMachine, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select qm from QueueMachine qm where qm.queueMachineId = :queueMachineId")
    Optional<QueueMachine> findByIdForTicketIssuing(@Param("queueMachineId") Long queueMachineId);

    List<QueueMachine> findByBranch(Branch branch);

    List<QueueMachine> findByStatus(String status);

    QueueMachine findByMachineCode(String machineCode);

    boolean existsByBranchAndMachineCode(Branch branch, String machineCode);

    boolean existsByBranchAndMachineCodeAndQueueMachineIdNot(
            Branch branch,
            String machineCode,
            Long queueMachineId);
}
