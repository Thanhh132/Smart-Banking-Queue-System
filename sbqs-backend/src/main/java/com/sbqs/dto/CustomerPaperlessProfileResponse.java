package com.sbqs.dto;

import java.util.List;
import java.util.Map;

public record CustomerPaperlessProfileResponse(
        Map<String, String> values,
        List<CustomerProfileFieldResponse> requiredFields,
        List<String> missingFields,
        boolean complete) {
}
