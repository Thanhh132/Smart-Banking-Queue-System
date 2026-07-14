package com.sbqs.repository;

import com.sbqs.entity.ServiceCatalog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ServiceCatalogRepository extends JpaRepository<ServiceCatalog, Long> {
    List<ServiceCatalog> findAllByOrderByServiceNameAsc();
    boolean existsByServiceCodeIgnoreCase(String serviceCode);
    boolean existsByServiceNameIgnoreCase(String serviceName);
    Optional<ServiceCatalog> findByServiceCodeIgnoreCase(String serviceCode);
}
