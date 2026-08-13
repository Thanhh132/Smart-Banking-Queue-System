package com.sbqs.repository;

import com.sbqs.entity.CustomerProfile;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CustomerProfileRepository extends JpaRepository<CustomerProfile, Long> {
    Optional<CustomerProfile> findByUserUserId(Long userId);
    void deleteByUserUserId(Long userId);
}
