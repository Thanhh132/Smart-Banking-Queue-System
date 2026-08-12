package com.sbqs.dto;

import com.sbqs.entity.User;

import java.time.LocalDateTime;

public record UserManagementResponse(
        Long userId,
        String fullName,
        String email,
        String phone,
        String role,
        String status,
        LocalDateTime createdAt,
        BranchSummary branch) {

    public static UserManagementResponse from(User user) {
        return new UserManagementResponse(
                user.getUserId(), user.getFullName(), user.getEmail(), user.getPhone(),
                user.getRole(), user.getStatus(), user.getCreatedAt(),
                user.getBranch() == null ? null : new BranchSummary(
                        user.getBranch().getBranchId(), user.getBranch().getBranchCode(),
                        user.getBranch().getBranchName()));
    }

    public record BranchSummary(Long branchId, String branchCode, String branchName) {
    }
}
