package com.sbqs.repository;

import com.sbqs.entity.Branch;
import com.sbqs.entity.Services;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ServiceRepository extends JpaRepository<Services, Long> {

    List<Services> findByBranch(Branch branch);

    List<Services> findByBranchAndServiceType(Branch branch, String serviceType);

    boolean existsByBranchAndServiceCode(Branch branch, String serviceCode);

    boolean existsByBranchAndServiceCodeAndServiceIdNot(
            Branch branch,
            String serviceCode,
            Long serviceId);
}
