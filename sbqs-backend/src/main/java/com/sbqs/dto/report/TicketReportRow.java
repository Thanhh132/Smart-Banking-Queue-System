package com.sbqs.dto.report;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class TicketReportRow {
    private Integer ticketNumber;
    private String customerEmail;
    private String serviceName;
    private String queueMachineName;
    private String branchName;
    private String status;
    private String createdAt;
}
