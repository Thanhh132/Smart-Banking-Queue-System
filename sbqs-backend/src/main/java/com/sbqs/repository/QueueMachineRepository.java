package com.sbqs.repository;

import com.sbqs.entity.Branch;
import com.sbqs.entity.QueueMachine;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface QueueMachineRepository
        extends JpaRepository<QueueMachine, Long> {

    List<QueueMachine> findByBranch(Branch branch);

    List<QueueMachine> findByStatus(String status);

    QueueMachine findByMachineCode(String machineCode);
}