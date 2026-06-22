package com.sbqs.dto.report;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class UserReportRow {
    private String fullName;
    private String email;
    private String phone;
    private String role;
    private String branchName;
    private String status;
    private String createdAt;
}
