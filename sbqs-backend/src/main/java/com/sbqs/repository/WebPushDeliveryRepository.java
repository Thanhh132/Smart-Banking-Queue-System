package com.sbqs.repository;

import com.sbqs.entity.WebPushDelivery;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WebPushDeliveryRepository extends JpaRepository<WebPushDelivery, Long> {
    boolean existsByTicketTicketIdAndSubscriptionSubscriptionIdAndNotificationType(
            Long ticketId, Long subscriptionId, String notificationType);
}
