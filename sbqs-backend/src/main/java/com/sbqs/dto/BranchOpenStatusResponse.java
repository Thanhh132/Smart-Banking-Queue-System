package com.sbqs.dto;

import java.time.LocalDateTime;

public record BranchOpenStatusResponse(
        Long branchId, boolean openNow, String message, LocalDateTime checkedAt) { }
