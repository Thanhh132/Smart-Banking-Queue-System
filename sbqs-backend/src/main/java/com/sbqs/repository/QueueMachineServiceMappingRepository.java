package com.sbqs.repository;

import com.sbqs.entity.QueueMachineServiceMapping;
import com.sbqs.entity.QueueMachineServiceMappingId;
import com.sbqs.entity.Branch;
import com.sbqs.entity.QueueMachine;
import com.sbqs.entity.Services;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface QueueMachineServiceMappingRepository
        extends JpaRepository<QueueMachineServiceMapping, QueueMachineServiceMappingId> {

    Optional<QueueMachineServiceMapping> findFirstByService(Services service);

    List<QueueMachineServiceMapping> findByService(Services service);

    Optional<QueueMachineServiceMapping> findFirstByQueueMachineBranchAndService(
            Branch branch,
            Services service);

    List<QueueMachineServiceMapping> findByQueueMachine(QueueMachine queueMachine);

    List<QueueMachineServiceMapping> findByQueueMachineBranchBranchId(Long branchId);

    @Query("""
            select distinct mapping.service
            from QueueMachineServiceMapping mapping
            where mapping.queueMachine.branch.branchId = :branchId
              and upper(mapping.service.status) = 'ACTIVE'
            """)
    List<Services> findActiveMappedServicesByBranchId(@Param("branchId") Long branchId);

    @Query("""
            select mapping
            from QueueMachineServiceMapping mapping
            where mapping.queueMachine.branch.branchId = :branchId
               or mapping.service.branch.branchId = :branchId
            """)
    List<QueueMachineServiceMapping> findAllRelatedToBranch(@Param("branchId") Long branchId);
}
