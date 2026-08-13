package com.sbqs.dto;

import com.sbqs.entity.User;

public record DevLoginAccountResponse(
        Long userId,
        String displayName,
        String role,
        String branchName) {

    public static DevLoginAccountResponse from(User user) {
        return new DevLoginAccountResponse(
                user.getUserId(),
                user.getFullName(),
                user.getRole(),
                user.getBranch() == null ? null : user.getBranch().getBranchName());
    }
}
