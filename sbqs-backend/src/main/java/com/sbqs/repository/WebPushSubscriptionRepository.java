package com.sbqs.repository;

import com.sbqs.entity.WebPushSubscription;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface WebPushSubscriptionRepository extends JpaRepository<WebPushSubscription, Long> {
    Optional<WebPushSubscription> findByEndpointHash(String endpointHash);
    List<WebPushSubscription> findByUserUserIdAndActiveTrue(Long userId);
    void deleteByUserUserId(Long userId);
}
