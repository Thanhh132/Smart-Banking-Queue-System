package com.sbqs.repository;

import com.sbqs.entity.DigitalDelegation;
import com.sbqs.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface DigitalDelegationRepository extends JpaRepository<DigitalDelegation, Long> {
    List<DigitalDelegation> findByOwnerOrderByCreatedAtDesc(User owner);
    Optional<DigitalDelegation> findByReferenceCodeIgnoreCase(String referenceCode);
    boolean existsByReferenceCode(String referenceCode);
}
