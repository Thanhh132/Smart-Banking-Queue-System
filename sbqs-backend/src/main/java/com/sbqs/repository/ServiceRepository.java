package com.sbqs.repository;

import com.sbqs.entity.Branch;
import com.sbqs.entity.Services;
import com.sbqs.entity.ServiceCatalog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ServiceRepository extends JpaRepository<Services, Long> {

    List<Services> findByBranch(Branch branch);

    List<Services> findByBranchAndStatusNotIgnoreCase(Branch branch, String status);

    List<Services> findByCatalog(ServiceCatalog catalog);

    List<Services> findByBranchAndServiceType(Branch branch, String serviceType);

    List<Services> findByBranchAndServiceTypeAndStatusNotIgnoreCase(
            Branch branch,
            String serviceType,
            String status);

    boolean existsByBranchAndServiceCode(Branch branch, String serviceCode);

    boolean existsByBranchAndServiceNameIgnoreCase(Branch branch, String serviceName);

    boolean existsByBranchAndServiceCodeAndServiceIdNot(
            Branch branch,
            String serviceCode,
            Long serviceId);

    boolean existsByBranchAndServiceNameIgnoreCaseAndServiceIdNot(
            Branch branch,
            String serviceName,
            Long serviceId);
}
