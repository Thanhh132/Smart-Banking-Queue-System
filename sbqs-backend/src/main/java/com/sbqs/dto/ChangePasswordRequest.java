package com.sbqs.dto;

import jakarta.validation.constraints.NotBlank;

public record ChangePasswordRequest(
        @NotBlank(message = "Mat khau hien tai khong duoc de trong") String currentPassword,
        @NotBlank(message = "Mat khau moi khong duoc de trong") String newPassword) {
}
