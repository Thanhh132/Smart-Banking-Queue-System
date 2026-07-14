package com.sbqs.dto;

public record BranchQueueLoad(
        Long branchId,
        Long waitingTickets,
        Long estimatedWorkMinutes) {
}
