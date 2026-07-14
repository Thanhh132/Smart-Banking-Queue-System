package com.sbqs.dto;

import java.time.LocalDateTime;

public record SmartRoutingRecommendationResponse(
        int rank,
        boolean recommended,
        Long branchId,
        String bankName,
        String branchCode,
        String branchName,
        String address,
        String province,
        String district,
        String ward,
        String phone,
        Double latitude,
        Double longitude,
        double distanceKm,
        long waitingTickets,
        long activeCounters,
        long estimatedWaitMinutes,
        double distanceScore,
        double waitScore,
        double routingScore,
        String explanation,
        LocalDateTime calculatedAt) {
}
