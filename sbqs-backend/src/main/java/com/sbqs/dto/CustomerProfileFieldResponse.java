package com.sbqs.dto;

public record CustomerProfileFieldResponse(
        String key,
        String label,
        String type,
        String placeholder,
        boolean required) {
}
