package com.sbqs.dto.report.query;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
public class TicketReportQueryRow {
    private Integer ticketNumber;
    private String customerEmail;
    private String serviceName;
    private String queueMachineName;
    private String branchName;
    private String status;
    private LocalDateTime createdAt;
}
