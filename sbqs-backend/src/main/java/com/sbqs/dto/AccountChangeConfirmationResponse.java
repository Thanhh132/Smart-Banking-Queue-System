package com.sbqs.dto;

public record AccountChangeConfirmationResponse(
        String status,
        String message,
        boolean emailChanged) {
}
