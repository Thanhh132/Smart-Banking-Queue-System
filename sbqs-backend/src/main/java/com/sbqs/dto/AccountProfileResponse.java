package com.sbqs.dto;

import com.sbqs.entity.User;

import java.time.LocalDateTime;

public record AccountProfileResponse(
        Long userId,
        String fullName,
        String email,
        String phone,
        String role,
        String status,
        Long branchId,
        String branchName,
        LocalDateTime createdAt) {

    public static AccountProfileResponse from(User user) {
        return new AccountProfileResponse(
                user.getUserId(),
                user.getFullName(),
                user.getEmail(),
                user.getPhone(),
                user.getRole(),
                user.getStatus(),
                user.getBranch() == null ? null : user.getBranch().getBranchId(),
                user.getBranch() == null ? null : user.getBranch().getBranchName(),
                user.getCreatedAt());
    }
}
