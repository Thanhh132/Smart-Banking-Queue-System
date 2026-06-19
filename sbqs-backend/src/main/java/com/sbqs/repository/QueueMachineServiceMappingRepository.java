package com.sbqs.repository;

import com.sbqs.entity.QueueMachineServiceMapping;
import com.sbqs.entity.QueueMachineServiceMappingId;
import com.sbqs.entity.Branch;
import com.sbqs.entity.Services;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface QueueMachineServiceMappingRepository
        extends JpaRepository<QueueMachineServiceMapping, QueueMachineServiceMappingId> {

    Optional<QueueMachineServiceMapping> findFirstByService(Services service);

    Optional<QueueMachineServiceMapping> findFirstByQueueMachineBranchAndService(
            Branch branch,
            Services service);

    List<QueueMachineServiceMapping> findByQueueMachineBranchBranchId(Long branchId);
}
