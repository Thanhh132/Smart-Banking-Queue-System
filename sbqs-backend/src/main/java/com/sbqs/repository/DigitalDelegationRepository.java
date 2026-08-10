package com.sbqs.repository;

import com.sbqs.entity.DigitalDelegation;
import com.sbqs.entity.Branch;
import com.sbqs.entity.User;
import com.sbqs.entity.Services;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.Optional;

public interface DigitalDelegationRepository extends JpaRepository<DigitalDelegation, Long> {
    List<DigitalDelegation> findByOwnerOrderByCreatedAtDesc(User owner);
    Optional<DigitalDelegation> findByReferenceCodeIgnoreCase(String referenceCode);
    List<DigitalDelegation> findByBranch(Branch branch);
    List<DigitalDelegation> findByService(Services service);
    boolean existsByReferenceCode(String referenceCode);

    @Modifying
    @Query("update DigitalDelegation delegation set delegation.verifiedBy = null where delegation.verifiedBy = :user")
    int clearVerifier(@Param("user") User user);
}
