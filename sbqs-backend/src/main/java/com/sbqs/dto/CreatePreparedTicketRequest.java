package com.sbqs.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.Map;

public record CreatePreparedTicketRequest(
        @NotNull Long branchId,
        @NotNull Long serviceId,
        @NotNull @Size(max = 50) Map<String, Object> values) {
}
