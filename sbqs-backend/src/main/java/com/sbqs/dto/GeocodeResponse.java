package com.sbqs.dto;

public record GeocodeResponse(
        String formattedAddress,
        Double latitude,
        Double longitude,
        String province,
        String district,
        String ward) {
}
