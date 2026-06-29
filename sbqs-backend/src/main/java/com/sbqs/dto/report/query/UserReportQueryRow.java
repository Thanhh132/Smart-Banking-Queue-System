package com.sbqs.dto.report.query;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
public class UserReportQueryRow {
    private String fullName;
    private String email;
    private String phone;
    private String role;
    private String branchName;
    private String status;
    private LocalDateTime createdAt;
}
